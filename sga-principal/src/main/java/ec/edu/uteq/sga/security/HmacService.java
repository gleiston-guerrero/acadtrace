package ec.edu.uteq.sga.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

/**
 * Firma HMAC-SHA256 para la columna auditoria.hmac (existia en la tabla
 * desde FIX-3 pero nunca se calculaba). Usa el mismo jwt.secret que ya
 * comparten sga-principal, secretaria y soporte para JWT/token interno,
 * asi que cualquier servicio puede firmar/verificar filas de auditoria
 * sin necesitar un secreto nuevo. Sirve para detectar si una fila de
 * auditoria fue alterada por fuera de la aplicacion (ej. UPDATE manual).
 */
@Component
public class HmacService {

    private final Mac mac;

    public HmacService(@Value("${jwt.secret}") String secret) {
        try {
            this.mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("No se pudo inicializar HmacSHA256", e);
        }
    }

    /** Firma la concatenacion de los campos clave de una fila de auditoria. Hex minuscula, 64 caracteres. */
    public synchronized String firmar(String... campos) {
        String canonical = String.join("|", campos);
        byte[] digest = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) hex.append(String.format("%02x", b));
        return hex.toString();
    }
}
