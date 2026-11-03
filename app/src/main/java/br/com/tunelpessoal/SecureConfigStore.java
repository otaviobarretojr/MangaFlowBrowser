package br.com.tunelpessoal;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Keeps the private key and tunnel config encrypted at rest with Android Keystore. */
final class SecureConfigStore {
    private static final String KEY_ALIAS = "tunel_pessoal_config_v1";
    private static final String PREF = "private_store";
    private static final String DATA = "encrypted_wireguard";
    private final SharedPreferences prefs;

    SecureConfigStore(Context context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private SecretKey secretKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        KeyStore.Entry entry = ks.getEntry(KEY_ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry)
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build());
        return generator.generateKey();
    }

    boolean hasProfile() { return prefs.contains(DATA); }

    void save(String config) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new SecureRandom());
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(config.getBytes(StandardCharsets.UTF_8));
        String value = Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP);
        if (!prefs.edit().putString(DATA, value).commit())
            throw new IllegalStateException("Falha ao salvar perfil");
    }

    String load() throws Exception {
        String saved = prefs.getString(DATA, null);
        if (saved == null) return null;
        String[] parts = saved.split(":", 2);
        if (parts.length != 2) throw new IllegalStateException("Perfil inválido");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey(),
            new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
        byte[] plain = cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP));
        return new String(plain, StandardCharsets.UTF_8);
    }

    void clear() { prefs.edit().remove(DATA).apply(); }
}
