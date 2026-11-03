package br.com.tunelpessoal;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.VpnService;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.wireguard.config.Config;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Simple offline, single-profile WireGuard client. No account or telemetry. */
public class MainActivity extends Activity {
    private static final int PICK_CONF = 501;
    private static final int VPN_PERMISSION = 502;
    private static final int NAVY = Color.rgb(8, 21, 44);
    private static final int PANEL = Color.rgb(17, 38, 70);
    private static final int BLUE = Color.rgb(58, 134, 255);
    private static final int LIGHT = Color.rgb(232, 240, 253);
    private final int pad = 20;
    private TextView status, profile, message;
    private Button toggle, importButton, removeButton;
    private SecureConfigStore secureStore;
    private VpnApp app;
    private boolean busy = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        secureStore = new SecureConfigStore(this);
        app = (VpnApp) getApplication();
        createScreen();
        refresh();
    }

    private int dp(float value) { return (int) (getResources().getDisplayMetrics().density * value + 0.5f); }

    private GradientDrawable background(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private TextView label(String text, int size, int color, boolean strong) {
        TextView result = new TextView(this);
        result.setText(text);
        result.setTextSize(size);
        result.setTextColor(color);
        result.setLineSpacing(dp(2), 1);
        if (strong) result.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return result;
    }

    private void addGap(LinearLayout parent, int pixels) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(pixels)));
    }

    private Button button(String label, int color) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(16);
        b.setTextColor(Color.WHITE);
        b.setBackground(background(color, 16));
        b.setMinHeight(dp(56));
        b.setPadding(dp(12), dp(14), dp(12), dp(14));
        return b;
    }

    private void createScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(NAVY);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(pad), dp(36), dp(pad), dp(24));
        root.setFitsSystemWindows(true);
        scroll.addView(root);

        TextView eyebrow = label("PROTEÇÃO DE REDE  •  ANDROID", 12, BLUE, true);
        root.addView(eyebrow);
        addGap(root, 12);
        root.addView(label("Túnel Pessoal", 34, Color.WHITE, true));
        addGap(root, 6);
        root.addView(label("Sua VPN, no seu celular, com controle simples.", 15, 0xFFBACAE4, false));
        addGap(root, 28);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(24), dp(20), dp(24));
        card.setBackground(background(PANEL, 22));
        root.addView(card);

        TextView shield = label("◆", 44, BLUE, true);
        shield.setGravity(Gravity.CENTER);
        card.addView(shield);
        addGap(card, 14);
        status = label("", 24, Color.WHITE, true);
        status.setGravity(Gravity.CENTER);
        card.addView(status);
        addGap(card, 10);
        profile = label("", 14, LIGHT, false);
        profile.setGravity(Gravity.CENTER);
        card.addView(profile);
        addGap(card, 22);
        toggle = button("CONECTAR", BLUE);
        card.addView(toggle, new LinearLayout.LayoutParams(-1, dp(58)));
        toggle.setOnClickListener(v -> toggleVpn());

        addGap(root, 24);
        root.addView(label("CONFIGURAÇÃO", 13, BLUE, true));
        addGap(root, 12);
        importButton = button("Importar perfil WireGuard (.conf)", 0xFF2B476B);
        root.addView(importButton, new LinearLayout.LayoutParams(-1, dp(56)));
        importButton.setOnClickListener(v -> importProfile());
        addGap(root, 10);
        removeButton = button("Remover perfil salvo", 0xFF3D3351);
        root.addView(removeButton, new LinearLayout.LayoutParams(-1, dp(52)));
        removeButton.setOnClickListener(v -> {
            if (app.isActive() || busy) {
                toast("Desconecte a VPN antes de remover o perfil.");
                return;
            }
            secureStore.clear();
            message.setText("Perfil removido deste aparelho.");
            refresh();
        });

        addGap(root, 24);
        message = label("Importe um arquivo .conf de um servidor WireGuard confiável.", 14, LIGHT, false);
        root.addView(message);
        addGap(root, 16);
        root.addView(label("IMPORTANTE: A VPN exige um servidor remoto e não garante acesso em redes que bloqueiam VPNs. Respeite as regras da rede utilizada. O app não possui contas nem análise de uso.", 12, 0xFF93A8C8, false));
        setContentView(scroll);
    }

    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }

    private void refresh() {
        boolean connected = app.isActive();
        status.setText(connected ? "Túnel ativado" : "Desconectado");
        status.setTextColor(connected ? 0xFF66E2AE : Color.WHITE);
        profile.setText(secureStore.hasProfile() ?
            "Perfil WireGuard importado" : "Nenhum perfil de servidor");
        toggle.setText(connected ? "DESCONECTAR" : "CONECTAR");
        toggle.setEnabled(!busy && (connected || secureStore.hasProfile()));
        toggle.setAlpha(toggle.isEnabled() ? 1.0f : 0.45f);
        importButton.setEnabled(!busy && !connected);
        removeButton.setEnabled(!busy && !connected && secureStore.hasProfile());
    }

    private void busy(boolean value) {
        busy = value;
        refresh();
    }

    private void importProfile() {
        if (busy || app.isActive()) {
            toast("Desconecte antes de trocar de perfil.");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_CONF);
    }

    private void toggleVpn() {
        if (busy) return;
        if (app.isActive()) {
            busy(true);
            message.setText("Encerrando túnel...");
            app.disconnect((success, result) -> runOnUiThread(() -> {
                busy(false);
                message.setText(result);
            }));
            return;
        }
        if (!secureStore.hasProfile()) {
            toast("Importe primeiro um perfil WireGuard.");
            return;
        }
        Intent permission = VpnService.prepare(this);
        if (permission != null) startActivityForResult(permission, VPN_PERMISSION);
        else startVpn();
    }

    private void startVpn() {
        busy(true);
        message.setText("Ativando túnel seguro...");
        app.connect((success, result) -> runOnUiThread(() -> {
            busy(false);
            message.setText(result);
        }));
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == VPN_PERMISSION) {
            if (result == RESULT_OK) startVpn();
            else message.setText("Permissão de VPN não concedida.");
        } else if (request == PICK_CONF && result == RESULT_OK && data != null) {
            try (InputStream in = getContentResolver().openInputStream(data.getData())) {
                if (in == null) throw new IllegalStateException("Arquivo indisponível");
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] chunk = new byte[4096];
                int length;
                while ((length = in.read(chunk)) != -1) {
                    if (out.size() + length > 64 * 1024)
                        throw new IllegalArgumentException("Arquivo muito grande");
                    out.write(chunk, 0, length);
                }
                byte[] bytes = out.toByteArray();
                Config parsed = Config.parse(new ByteArrayInputStream(bytes));
                if (parsed.getPeers().isEmpty())
                    throw new IllegalArgumentException("Perfil sem [Peer]");
                secureStore.save(new String(bytes, StandardCharsets.UTF_8));
                message.setText("Perfil importado e protegido com criptografia local.");
                refresh();
            } catch (Exception e) {
                message.setText("Não foi possível importar. Selecione um .conf WireGuard válido.");
            }
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (status != null) refresh();
    }
}
