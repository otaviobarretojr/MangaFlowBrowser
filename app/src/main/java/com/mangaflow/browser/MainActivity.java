package com.mangaflow.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.ContentBlocking;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebExtension;

import java.util.List;

/**
 * MangaFlow V0.1 — um único separador com leitor automatizado e biblioteca local.
 * Sem coleta de dados, scripts remotos próprios ou permissões sensíveis.
 */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(10, 15, 27);
    private static final int PANEL = Color.rgb(20, 28, 46);
    private static final int ACCENT = Color.rgb(110, 141, 255);
    private static final int TEXT = Color.rgb(238, 242, 255);
    private static final int MUTED = Color.rgb(150, 164, 188);
    private static final String EXT_PATH = "resource://android/assets/reader/";
    private static final String EXT_ID = "reader@mangaflow.local";

    private static GeckoRuntime runtime;
    private GeckoSession session;
    private GeckoView geckoView;
    private WebExtension.Port activePort;
    private SharedPreferences prefs;
    private LinearLayout topBar, bottomBar;
    private FrameLayout browserArea;
    private LinearLayout homeOverlay;
    private EditText address;
    private TextView readStatus, speedText;
    private Button autoButton, readerButton, fullscreenButton, backButton, nextButton;
    private boolean isPlaying, readerMode, fullscreen, canGoBack, canGoForward, stepMode;
    private int speed, stepSeconds;
    private String currentUrl = "";
    private String currentTitle = "MangaFlow";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("mangaflow.local", MODE_PRIVATE);
        speed = prefs.getInt("speed", 45);
        readerMode = prefs.getBoolean("reader", false);
        stepMode = prefs.getBoolean("step_mode", false);
        stepSeconds = prefs.getInt("step_seconds", 3);
        if (stepSeconds != 2 && stepSeconds != 3 && stepSeconds != 5 && stepSeconds != 8) stepSeconds = 3;
        buildUi();
        initBrowser();
        showHome();
    }

    private int dp(float value) {
        return (int) (getResources().getDisplayMetrics().density * value + 0.5f);
    }

    private GradientDrawable background(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private TextView label(String text, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String title) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextColor(TEXT);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setPadding(dp(9), 0, dp(9), 0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setBackground(background(PANEL, 12));
        return b;
    }

    private void rowButton(LinearLayout row, Button button, int leftMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(43), 1);
        p.leftMargin = dp(leftMargin);
        row.addView(button, p);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);

        topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.VERTICAL);
        topBar.setPadding(dp(12), dp(8), dp(12), dp(8));
        root.addView(topBar, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        topBar.addView(titleRow, new LinearLayout.LayoutParams(-1, dp(43)));
        TextView appTitle = label("MangaFlow", 21, TEXT, true);
        titleRow.addView(appTitle, new LinearLayout.LayoutParams(0, -1, 1));
        Button favorite = button("☆ Salvar");
        titleRow.addView(favorite, new LinearLayout.LayoutParams(dp(95), dp(40)));
        favorite.setOnClickListener(v -> saveFavorite());
        Button home = button("⌂");
        LinearLayout.LayoutParams h = new LinearLayout.LayoutParams(dp(44), dp(40));
        h.leftMargin = dp(8);
        titleRow.addView(home, h);
        home.setOnClickListener(v -> showHome());

        LinearLayout addressRow = new LinearLayout(this);
        addressRow.setGravity(Gravity.CENTER_VERTICAL);
        topBar.addView(addressRow, new LinearLayout.LayoutParams(-1, dp(46)));
        address = new EditText(this);
        address.setTextSize(13);
        address.setSingleLine(true);
        address.setHint("Cole o link ou digite o site de mangás");
        address.setHintTextColor(MUTED);
        address.setTextColor(TEXT);
        address.setSelectAllOnFocus(true);
        address.setPadding(dp(14), 0, dp(12), 0);
        address.setBackground(background(PANEL, 12));
        address.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressRow.addView(address, new LinearLayout.LayoutParams(0, dp(42), 1));
        Button navigate = button("Abrir");
        LinearLayout.LayoutParams n = new LinearLayout.LayoutParams(dp(70), dp(42));
        n.leftMargin = dp(8);
        addressRow.addView(navigate, n);
        navigate.setOnClickListener(v -> navigateFromInput());
        address.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                navigateFromInput();
                return true;
            }
            return false;
        });

        browserArea = new FrameLayout(this);
        root.addView(browserArea, new LinearLayout.LayoutParams(-1, 0, 1));
        geckoView = new GeckoView(this);
        browserArea.addView(geckoView, new FrameLayout.LayoutParams(-1, -1));

        homeOverlay = new LinearLayout(this);
        homeOverlay.setOrientation(LinearLayout.VERTICAL);
        homeOverlay.setGravity(Gravity.CENTER);
        homeOverlay.setPadding(dp(28), dp(12), dp(28), dp(12));
        homeOverlay.setBackgroundColor(BG);
        browserArea.addView(homeOverlay, new FrameLayout.LayoutParams(-1, -1));
        TextView emoji = label("📖", 54, TEXT, true);
        emoji.setGravity(Gravity.CENTER);
        homeOverlay.addView(emoji, new LinearLayout.LayoutParams(-1, dp(94)));
        TextView greeting = label("Sua leitura, sem distrações", 23, TEXT, true);
        greeting.setGravity(Gravity.CENTER);
        homeOverlay.addView(greeting, new LinearLayout.LayoutParams(-1, dp(64)));
        TextView hint = label("Abra seu site favorito, ative o modo leitor e aproveite a rolagem automática.", 14, MUTED, false);
        hint.setGravity(Gravity.CENTER);
        homeOverlay.addView(hint, new LinearLayout.LayoutParams(-1, dp(78)));
        Button library = button("★ Meus favoritos");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(48));
        lp.topMargin = dp(20);
        homeOverlay.addView(library, lp);
        library.setOnClickListener(v -> showFavorites());
        Button continueReading = button("↪ Continuar última leitura");
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, dp(48));
        cp.topMargin = dp(12);
        homeOverlay.addView(continueReading, cp);
        continueReading.setOnClickListener(v -> {
            String last = prefs.getString("last_url", "");
            if (last.isEmpty()) Toast.makeText(this, "Nenhuma leitura anterior", Toast.LENGTH_SHORT).show();
            else navigate(last);
        });

        fullscreenButton = button("⛶");
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(dp(43), dp(43), Gravity.TOP | Gravity.END);
        fp.setMargins(0, dp(7), dp(8), 0);
        browserArea.addView(fullscreenButton, fp);
        fullscreenButton.setAlpha(0.78f);
        fullscreenButton.setOnClickListener(v -> toggleFullscreen());

        bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.VERTICAL);
        bottomBar.setPadding(dp(10), dp(8), dp(10), dp(10));
        bottomBar.setBackgroundColor(BG);
        root.addView(bottomBar, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout navRow = new LinearLayout(this);
        bottomBar.addView(navRow, new LinearLayout.LayoutParams(-1, dp(43)));
        backButton = button("←");
        nextButton = button("→");
        Button reloadButton = button("↻");
        readerButton = button(readerMode ? "Leitor ✓" : "Leitor");
        autoButton = button("▶ Auto");
        rowButton(navRow, backButton, 0);
        rowButton(navRow, nextButton, 6);
        rowButton(navRow, reloadButton, 6);
        rowButton(navRow, readerButton, 6);
        rowButton(navRow, autoButton, 6);
        backButton.setOnClickListener(v -> { if (canGoBack) session.goBack(); else showHome(); });
        nextButton.setOnClickListener(v -> { if (canGoForward) session.goForward(); });
        reloadButton.setOnClickListener(v -> { if (!currentUrl.isEmpty()) session.reload(); });
        readerButton.setOnClickListener(v -> {
            readerMode = !readerMode;
            prefs.edit().putBoolean("reader", readerMode).apply();
            readerButton.setText(readerMode ? "Leitor ✓" : "Leitor");
            command("reader", "enabled", readerMode);
        });
        autoButton.setOnClickListener(v -> {
            if (homeOverlay.getVisibility() == View.VISIBLE) {
                Toast.makeText(this, "Abra um capítulo primeiro", Toast.LENGTH_SHORT).show();
                return;
            }
            isPlaying = !isPlaying;
            command("mode", "value", stepMode ? "steps" : "continuous");
            command("interval", "value", stepSeconds * 1000);
            command(isPlaying ? "play" : "pause", null, null);
            setAutoUi();
        });
        autoButton.setOnLongClickListener(v -> { showAutoSettings(); return true; });

        LinearLayout scrollRow = new LinearLayout(this);
        scrollRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(43));
        sp.topMargin = dp(4);
        bottomBar.addView(scrollRow, sp);
        speedText = label(stepMode ? stepSeconds + "s / etapa" : speed + " px/s", 12, MUTED, false);
        speedText.setOnClickListener(v -> showAutoSettings());
        scrollRow.addView(speedText, new LinearLayout.LayoutParams(dp(86), -1));
        SeekBar seek = new SeekBar(this);
        seek.setMax(150);
        seek.setProgress(speed - 10);
        scrollRow.addView(seek, new LinearLayout.LayoutParams(0, dp(43), 1));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                speed = 10 + progress;
                if (!stepMode) speedText.setText(speed + " px/s");
                prefs.edit().putInt("speed", speed).apply();
                command("speed", "value", speed);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        readStatus = label("Toque na velocidade ou segure Auto para escolher o modo", 11, MUTED, false);
        readStatus.setGravity(Gravity.CENTER);
        bottomBar.addView(readStatus, new LinearLayout.LayoutParams(-1, dp(20)));
    }

    private void initBrowser() {
        if (runtime == null) {
            ContentBlocking.Settings blocking = new ContentBlocking.Settings.Builder()
                .antiTracking(ContentBlocking.AntiTracking.STRICT)
                .build();
            GeckoRuntimeSettings settings = new GeckoRuntimeSettings.Builder()
                .contentBlocking(blocking)
                .build();
            runtime = GeckoRuntime.create(getApplicationContext(), settings);
        }
        session = new GeckoSession();
        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override public GeckoResult<AllowOrDeny> onLoadRequest(
                    GeckoSession s, GeckoSession.NavigationDelegate.LoadRequest request) {
                String uri = request.uri;
                if (!isHttp(uri) && !uri.startsWith("about:")) return GeckoResult.deny();
                if (request.target == GeckoSession.NavigationDelegate.TARGET_WINDOW_NEW) {
                    if (request.hasUserGesture && isHttp(uri)) {
                        s.loadUri(uri); // Clique real: usar a aba atual.
                    }
                    return GeckoResult.deny(); // Pop-up sem clique: sempre negar.
                }
                return GeckoResult.allow();
            }
            @Override public GeckoResult<GeckoSession> onNewSession(GeckoSession s, String url) {
                return null; // Não criar abas ou janelas em segundo plano.
            }
            @Override public void onLocationChange(
                    GeckoSession s, String url,
                    List<GeckoSession.PermissionDelegate.ContentPermission> permissions,
                    Boolean hasUserGesture) {
                if (url == null || !isHttp(url)) return;
                if (!url.equals(currentUrl)) {
                    stopAutoLocally();
                    currentUrl = url;
                }
                address.setText(url);
                prefs.edit().putString("last_url", url).apply();
                homeOverlay.setVisibility(View.GONE);
            }
            @Override public void onCanGoBack(GeckoSession s, boolean enabled) {
                canGoBack = enabled;
                backButton.setAlpha(enabled ? 1f : .45f);
            }
            @Override public void onCanGoForward(GeckoSession s, boolean enabled) {
                canGoForward = enabled;
                nextButton.setAlpha(enabled ? 1f : .45f);
            }
        });
        session.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override public void onTitleChange(GeckoSession s, String title) {
                if (title != null && !title.trim().isEmpty()) currentTitle = title.trim();
            }
        });
        session.open(runtime);
        geckoView.setSession(session);
        installReaderExtension();
    }

    private void installReaderExtension() {
        runtime.getWebExtensionController().ensureBuiltIn(EXT_PATH, EXT_ID).accept(ext -> {
            session.getWebExtensionController().setMessageDelegate(
                ext, new WebExtension.MessageDelegate() {
                    @Override public void onConnect(WebExtension.Port port) {
                        // As portas são autorizadas somente para a sessão visível.
                        if (port.sender.session != session) return;
                        activePort = port;
                        port.setDelegate(new WebExtension.PortDelegate() {
                            @Override public void onPortMessage(Object message, WebExtension.Port p) {
                                if (p != activePort || !(message instanceof JSONObject)) return;
                                JSONObject data = (JSONObject) message;
                                String type = data.optString("type", "");
                                if ("ready".equals(type)) {
                                    String url = data.optString("url", "");
                                    // A extensão pode conectar em navegações anteriores: ignorar se divergir.
                                    if (!url.equals(currentUrl)) return;
                                    command("speed", "value", speed);
                                    command("mode", "value", stepMode ? "steps" : "continuous");
                                    command("interval", "value", stepSeconds * 1000);
                                    command("reader", "enabled", readerMode);
                                    float pos = prefs.getFloat("p:" + url, 0f);
                                    // Sempre confirmar restauração, inclusive sem progresso anterior.
                                    command("restore", "progress", pos);
                                } else if ("progress".equals(type)) {
                                    String url = data.optString("url", "");
                                    if (!url.equals(currentUrl)) return;
                                    float position = (float) data.optDouble("progress", 0d);
                                    if (position >= 0f && position <= 1f) {
                                        prefs.edit().putFloat("p:" + url, position).apply();
                                        int percentage = Math.round(position * 100);
                                        readStatus.setText("Leitura: " + percentage + "% • " +
                                            (data.optBoolean("playing") ? "Rolagem ativa" : "Pausado"));
                                    }
                                } else if ("playing".equals(type)) {
                                    isPlaying = data.optBoolean("playing", false);
                                    setAutoUi();
                                } else if ("finished".equals(type)) {
                                    stopAutoLocally();
                                    readStatus.setText("Fim da página • procure o próximo capítulo");
                                }
                            }
                            @Override public void onDisconnect(WebExtension.Port disconnected) {
                                if (disconnected == activePort) {
                                    activePort = null;
                                    stopAutoLocally();
                                }
                            }
                        });
                    }
                }, "mangaflow");
        }, error -> {
            Toast.makeText(this, "Falha ao iniciar o leitor: " + error.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private static boolean isHttp(String url) {
        if (url == null) return false;
        Uri uri = Uri.parse(url);
        return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
            && uri.getHost() != null;
    }

    private void navigateFromInput() {
        String url = address.getText().toString().trim();
        if (url.isEmpty()) return;
        if (!url.contains("://")) url = "https://" + url;
        if (!isHttp(url)) {
            Toast.makeText(this, "Digite um endereço HTTP ou HTTPS válido", Toast.LENGTH_SHORT).show();
            return;
        }
        navigate(url);
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(address.getWindowToken(), 0);
        address.clearFocus();
    }

    private void navigate(String url) {
        stopAutoLocally();
        homeOverlay.setVisibility(View.GONE);
        currentUrl = url;
        currentTitle = Uri.parse(url).getHost();
        address.setText(url);
        session.loadUri(url);
    }

    private void showHome() {
        command("pause", null, null);
        stopAutoLocally();
        if (fullscreen) toggleFullscreen();
        homeOverlay.setVisibility(View.VISIBLE);
        address.setText("");
    }

    private void toggleFullscreen() {
        fullscreen = !fullscreen;
        topBar.setVisibility(fullscreen ? View.GONE : View.VISIBLE);
        bottomBar.setVisibility(fullscreen ? View.GONE : View.VISIBLE);
        fullscreenButton.setText(fullscreen ? "⤢" : "⛶");
        if (fullscreen) getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void command(String name, String param, Object value) {
        WebExtension.Port port = activePort;
        if (port == null) {
            if ("play".equals(name)) {
                stopAutoLocally();
                Toast.makeText(this, "Leitor ainda não está pronto neste site", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        try {
            JSONObject message = new JSONObject().put("command", name);
            if (param != null) message.put(param, value);
            port.postMessage(message);
        } catch (Exception error) {
            if ("play".equals(name)) stopAutoLocally();
        }
    }

    private void showAutoSettings() {
        String[] options = {
            "Contínua — ajustar pela barra",
            "Por etapas — a cada 2 segundos",
            "Por etapas — a cada 3 segundos",
            "Por etapas — a cada 5 segundos",
            "Por etapas — a cada 8 segundos"
        };
        int selected = !stepMode ? 0 : stepSeconds == 2 ? 1 : stepSeconds == 3 ? 2 :
            stepSeconds == 5 ? 3 : 4;
        new AlertDialog.Builder(this)
            .setTitle("Como deseja rolar o capítulo?")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                stepMode = which != 0;
                if (stepMode) stepSeconds = new int[]{0, 2, 3, 5, 8}[which];
                prefs.edit().putBoolean("step_mode", stepMode)
                    .putInt("step_seconds", stepSeconds).apply();
                speedText.setText(stepMode ? stepSeconds + "s / etapa" : speed + " px/s");
                command("mode", "value", stepMode ? "steps" : "continuous");
                command("interval", "value", stepSeconds * 1000);
                readStatus.setText(stepMode
                    ? "Rolagem por etapas: " + stepSeconds + " segundos"
                    : "Rolagem contínua: " + speed + " px/s");
                dialog.dismiss();
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void stopAutoLocally() {
        isPlaying = false;
        setAutoUi();
    }

    private void setAutoUi() {
        if (autoButton == null) return;
        autoButton.setText(isPlaying ? "Ⅱ Pausar" : "▶ Auto");
        getWindow().setFlags(isPlaying ? WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON : 0,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void saveFavorite() {
        if (!isHttp(currentUrl) || homeOverlay.getVisibility() == View.VISIBLE) {
            Toast.makeText(this, "Abra um site antes de salvar", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONArray input = new JSONArray(prefs.getString("favorites", "[]"));
            JSONArray out = new JSONArray();
            for (int i = 0; i < input.length(); i++) {
                JSONObject existing = input.getJSONObject(i);
                if (!currentUrl.equals(existing.optString("url"))) out.put(existing);
            }
            out.put(new JSONObject().put("url", currentUrl).put("title", currentTitle));
            prefs.edit().putString("favorites", out.toString()).apply();
            Toast.makeText(this, "Salvo nos favoritos", Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            Toast.makeText(this, "Não foi possível salvar", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFavorites() {
        try {
            JSONArray favorites = new JSONArray(prefs.getString("favorites", "[]"));
            if (favorites.length() == 0) {
                new AlertDialog.Builder(this).setTitle("Favoritos")
                    .setMessage("Nenhum site salvo ainda. Abra um capítulo e toque em ☆ Salvar.")
                    .setPositiveButton("Entendi", null).show();
                return;
            }
            String[] entries = new String[favorites.length()];
            for (int i = 0; i < favorites.length(); i++) {
                JSONObject item = favorites.getJSONObject(i);
                entries[i] = item.optString("title", "Site") + "\n" + item.optString("url", "");
            }
            new AlertDialog.Builder(this).setTitle("★ Biblioteca pessoal")
                .setItems(entries, (dialog, index) -> {
                    String url = favorites.optJSONObject(index).optString("url", "");
                    if (isHttp(url)) navigate(url);
                })
                .setNeutralButton("Organizar", (dialog, which) -> manageFavorites(favorites))
                .setNegativeButton("Fechar", null).show();
        } catch (Exception error) {
            Toast.makeText(this, "Favoritos inválidos", Toast.LENGTH_SHORT).show();
        }
    }

    private void manageFavorites(JSONArray items) {
        String[] labels = new String[items.length()];
        for (int i = 0; i < items.length(); i++) {
            labels[i] = items.optJSONObject(i).optString("title", "Site");
        }
        new AlertDialog.Builder(this).setTitle("Remover favorito")
            .setItems(labels, (d, which) -> new AlertDialog.Builder(this)
                .setTitle("Remover este favorito?")
                .setPositiveButton("Remover", (d2, w2) -> {
                    JSONArray out = new JSONArray();
                    for (int j = 0; j < items.length(); j++) if (j != which) out.put(items.opt(j));
                    prefs.edit().putString("favorites", out.toString()).apply();
                }).setNegativeButton("Cancelar", null).show())
            .setNegativeButton("Voltar", null).show();
    }

    @Override public void onBackPressed() {
        if (fullscreen) { toggleFullscreen(); return; }
        if (homeOverlay.getVisibility() == View.VISIBLE) { super.onBackPressed(); return; }
        if (canGoBack) session.goBack();
        else showHome();
    }

    @Override protected void onPause() {
        super.onPause();
        if (isPlaying) command("pause", null, null);
        stopAutoLocally();
    }

    @Override protected void onDestroy() {
        if (activePort != null) activePort.disconnect();
        if (session != null) session.close();
        super.onDestroy();
    }
}
