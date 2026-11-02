package com.mangaflow.browser;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Leitor CBZ offline: funciona com arquivos do usuário, sem login ou VIP.
 * Usa o seletor do Android; nunca tenta acessar conteúdo pago de servidores.
 */
public final class CbzReaderActivity extends Activity {
    private static final int PICK_FILE = 4101;
    private static final int BG = Color.rgb(10, 15, 27);
    private static final int TEXT = Color.rgb(238, 242, 255);
    private static final long MAX_PAGE_BYTES = 25L * 1024 * 1024;
    private static final long MAX_TOTAL_BYTES = 300L * 1024 * 1024;
    private static final int MAX_PAGES = 800;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ArrayList<Page> pages = new ArrayList<>();
    private ScrollView scroller;
    private ImageView picture;
    private TextView status;
    private Button previous, next;
    private File workDir;
    private int index = 0;
    private int generation = 0;
    private volatile boolean closing = false;

    private static final class Page {
        final String name;
        final File file;
        Page(String name, File file) { this.name = name; this.file = file; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("*/*");
        pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(pick, PICK_FILE);
    }

    private Button button(String title) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextColor(TEXT);
        b.setAllCaps(false);
        return b;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));
        Button back = button("← Voltar");
        header.addView(back, new LinearLayout.LayoutParams(0, -2, 1));
        back.setOnClickListener(v -> finish());
        Button open = button("📂 Outro CBZ");
        header.addView(open, new LinearLayout.LayoutParams(0, -2, 1));
        open.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            pick.setType("*/*");
            startActivityForResult(pick, PICK_FILE);
        });

        scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        root.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));
        picture = new ImageView(this);
        picture.setAdjustViewBounds(true);
        picture.setScaleType(ImageView.ScaleType.FIT_START);
        scroller.addView(picture, new ScrollView.LayoutParams(-1, -2));

        status = new TextView(this);
        status.setTextColor(TEXT);
        status.setGravity(Gravity.CENTER);
        status.setText("Selecione um arquivo CBZ ou ZIP com páginas de imagem.");
        status.setPadding(12, 12, 12, 12);
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout footer = new LinearLayout(this);
        root.addView(footer, new LinearLayout.LayoutParams(-1, -2));
        previous = button("◀ Anterior");
        next = button("Próxima ▶");
        footer.addView(previous, new LinearLayout.LayoutParams(0, -2, 1));
        footer.addView(next, new LinearLayout.LayoutParams(0, -2, 1));
        previous.setOnClickListener(v -> move(-1));
        next.setOnClickListener(v -> move(1));
        updateButtons();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_FILE) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            if (pages.isEmpty()) finish();
            return;
        }
        loadArchive(data.getData());
    }

    private static boolean isImage(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")
            || n.endsWith(".webp") || n.endsWith(".gif");
    }

    private static int naturalCompare(String a, String b) {
        int i = 0, j = 0;
        while (i < a.length() && j < b.length()) {
            char ca = Character.toLowerCase(a.charAt(i));
            char cb = Character.toLowerCase(b.charAt(j));
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                long na = 0, nb = 0;
                while (i < a.length() && Character.isDigit(a.charAt(i)))
                    na = Math.min(1000000000L, na * 10 + Character.digit(a.charAt(i++), 10));
                while (j < b.length() && Character.isDigit(b.charAt(j)))
                    nb = Math.min(1000000000L, nb * 10 + Character.digit(b.charAt(j++), 10));
                if (na != nb) return Long.compare(na, nb);
            } else {
                if (ca != cb) return Character.compare(ca, cb);
                i++; j++;
            }
        }
        return Integer.compare(a.length() - i, b.length() - j);
    }

    private void loadArchive(Uri uri) {
        ++generation;
        status.setText("Preparando páginas offline…");
        picture.setImageDrawable(null);
        previous.setEnabled(false);
        next.setEnabled(false);
        worker.execute(() -> {
            File dir = new File(getCacheDir(), "cbz-" + System.nanoTime());
            ArrayList<Page> imported = new ArrayList<>();
            try {
                if (!dir.mkdirs()) throw new Exception("Não foi possível criar o cache");
                long total = 0;
                try (InputStream raw = getContentResolver().openInputStream(uri)) {
                    if (raw == null) throw new Exception("Não foi possível abrir o arquivo");
                    try (ZipInputStream zip = new ZipInputStream(raw)) {
                        ZipEntry entry;
                        byte[] buffer = new byte[16384];
                        while ((entry = zip.getNextEntry()) != null) {
                            if (closing) throw new Exception("Leitura cancelada");
                            if (!entry.isDirectory() && isImage(entry.getName())) {
                                if (imported.size() >= MAX_PAGES)
                                    throw new Exception("Arquivo com mais de 800 páginas");
                                File target = new File(dir, String.format(Locale.US, "%04d.img", imported.size()));
                                long pageBytes = 0;
                                try (FileOutputStream out = new FileOutputStream(target)) {
                                    int n;
                                    while ((n = zip.read(buffer)) != -1) {
                                        pageBytes += n; total += n;
                                        if (pageBytes > MAX_PAGE_BYTES || total > MAX_TOTAL_BYTES)
                                            throw new Exception("Arquivo excede o limite de segurança");
                                        out.write(buffer, 0, n);
                                    }
                                }
                                imported.add(new Page(entry.getName(), target));
                            }
                            zip.closeEntry();
                        }
                    }
                }
                if (imported.isEmpty()) throw new Exception("Nenhuma imagem JPG/PNG/WebP encontrada");
                imported.sort((a, b) -> naturalCompare(a.name, b.name));
                runOnUiThread(() -> {
                    if (closing) return;
                    File old = workDir;
                    workDir = dir;
                    pages.clear();
                    pages.addAll(imported);
                    index = 0;
                    showPage();
                    if (old != null) worker.execute(() -> deleteDir(old));
                });
            } catch (Exception e) {
                deleteDir(dir);
                runOnUiThread(() -> {
                    if (closing) return;
                    status.setText("Não foi possível abrir o CBZ: " + e.getMessage());
                    updateButtons();
                });
            }
        });
    }

    private void move(int delta) {
        int target = index + delta;
        if (target >= 0 && target < pages.size()) {
            index = target;
            showPage();
        }
    }

    private void showPage() {
        final int requested = ++generation;
        if (pages.isEmpty()) return;
        Page selected = pages.get(index);
        status.setText(String.format(Locale.getDefault(), "Página %d de %d", index + 1, pages.size()));
        updateButtons();
        picture.setImageDrawable(null);
        scroller.scrollTo(0, 0);
        worker.execute(() -> {
            Bitmap decoded = null;
            try {
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(selected.file.getAbsolutePath(), bounds);
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new Exception("Imagem inválida");
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                int sample = 1;
                int targetWidth = getResources().getDisplayMetrics().widthPixels * 2;
                while (bounds.outWidth / sample > targetWidth ||
                    ((long) bounds.outWidth / sample) * (bounds.outHeight / sample) > 12000000L)
                    sample *= 2;
                options.inSampleSize = sample;
                decoded = BitmapFactory.decodeFile(selected.file.getAbsolutePath(), options);
                if (decoded == null) throw new Exception("Não foi possível decodificar a imagem");
                final Bitmap ready = decoded;
                runOnUiThread(() -> {
                    if (closing || requested != generation) { ready.recycle(); return; }
                    picture.setImageBitmap(ready);
                    scroller.scrollTo(0, 0);
                });
            } catch (Exception e) {
                if (decoded != null) decoded.recycle();
                runOnUiThread(() -> {
                    if (!closing && requested == generation)
                        status.setText("Erro na página: " + e.getMessage());
                });
            }
        });
    }

    private void updateButtons() {
        previous.setEnabled(index > 0);
        next.setEnabled(index + 1 < pages.size());
    }

    private static void deleteDir(File dir) {
        if (dir == null) return;
        File[] children = dir.listFiles();
        if (children != null) for (File child : children) {
            if (child.isDirectory()) deleteDir(child);
            else child.delete();
        }
        dir.delete();
    }

    @Override protected void onDestroy() {
        closing = true;
        ++generation;
        picture.setImageDrawable(null);
        final File old = workDir;
        if (old != null) worker.execute(() -> deleteDir(old));
        worker.shutdown();
        super.onDestroy();
    }
}
