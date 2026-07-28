package ec.uteq.sga.secretaria.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifrado simetrico de campos sensibles (AES-256-GCM) para datos de
 * estudiantes/representantes (menores de edad). El nonce (12 bytes) se
 * genera por operacion y se antepone al texto cifrado antes de codificar en
 * base64, asi no hace falta guardar ni transmitir el IV por separado.
 *
 * IMPORTANTE: solo debe aplicarse a columnas que Secretaria es la unica
 * escritora; si sga-principal tambien escribe la misma columna en claro, el
 * descifrado de esas filas fallara (ver historial_promocion / migracion
 * pendiente hasta que el gRPC centralice las escrituras).
 */
@Component
public class CryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public CryptoService(@Value("${app.crypto.secret-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "app.crypto.secret-key debe ser una clave AES-256 (32 bytes) en base64; recibidos "
                            + keyBytes.length + " bytes");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] output = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, output, 0, iv.length);
            System.arraycopy(ciphertext, 0, output, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(output);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Error cifrando dato sensible", e);
        }
    }

    public String decrypt(String encoded) {
        if (encoded == null) return null;
        try {
            byte[] input = Base64.getDecoder().decode(encoded);
            if (input.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Valor cifrado invalido (demasiado corto)");
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(input, 0, iv, 0, GCM_IV_LENGTH);
            byte[] ciphertext = new byte[input.length - GCM_IV_LENGTH];
            System.arraycopy(input, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Error descifrando dato sensible", e);
        }
    }
}
