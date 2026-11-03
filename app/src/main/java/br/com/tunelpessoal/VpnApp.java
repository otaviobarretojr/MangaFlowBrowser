package br.com.tunelpessoal;

import android.app.Application;
import com.wireguard.android.backend.GoBackend;
import com.wireguard.android.backend.Tunnel;
import com.wireguard.config.Config;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VpnApp extends Application {
    interface Callback { void done(boolean success, String message); }

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private GoBackend backend;
    private volatile Tunnel.State state = Tunnel.State.DOWN;
    private final Tunnel tunnel = new Tunnel() {
        @Override public String getName() { return "Pessoal"; }
        @Override public void onStateChange(Tunnel.State changed) { state = changed; }
    };

    @Override public void onCreate() {
        super.onCreate();
        // Lazy initialization: loading the native backend should never crash the UI at startup.
    }

    private synchronized GoBackend getBackend() {
        if (backend == null) backend = new GoBackend(getApplicationContext());
        return backend;
    }

    boolean isActive() { return state == Tunnel.State.UP; }

    void connect(final Callback callback) {
        io.execute(() -> {
            try {
                String profile = new SecureConfigStore(this).load();
                if (profile == null) throw new IllegalStateException("Importe um perfil primeiro.");
                Config config = Config.parse(new ByteArrayInputStream(profile.getBytes(StandardCharsets.UTF_8)));
                if (config.getPeers().isEmpty())
                    throw new IllegalArgumentException("Perfil sem servidor [Peer].");
                state = getBackend().setState(tunnel, Tunnel.State.UP, config);
                callback.done(state == Tunnel.State.UP,
                    state == Tunnel.State.UP ? "Túnel ativado. Verifique se há acesso à internet." :
                    "Não foi possível ativar o túnel.");
            } catch (Exception ex) {
                callback.done(false, "Erro de conexão: " + safeError(ex));
            }
        });
    }

    void disconnect(final Callback callback) {
        io.execute(() -> {
            try {
                state = getBackend().setState(tunnel, Tunnel.State.DOWN, null);
                callback.done(true, "VPN desconectada.");
            } catch (Exception ex) {
                callback.done(false, "Erro ao desconectar: " + safeError(ex));
            }
        });
    }

    private static String safeError(Exception e) {
        String name = e.getClass().getSimpleName();
        // Do not show exception messages: errors may accidentally contain secret config values.
        if (e instanceof IllegalStateException || e instanceof IllegalArgumentException)
            return "verifique as informações do perfil.";
        return name + " (verifique seu servidor e a rede).";
    }
}
