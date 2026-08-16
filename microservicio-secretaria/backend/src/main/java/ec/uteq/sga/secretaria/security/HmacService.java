package ec.uteq.sga.secretaria.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Firma HMAC-SHA256 para filas de sga_principal.auditoria escritas desde
 * secretaria. Debe producir exactamente la misma firma que
 * ec.edu.uteq.sga.security.HmacService en sga-principal (mismo secreto
 * app.jwt.secret == jwt.secret, mismo orden de campos) para que una fila
 * escrita por cualquiera de los dos servicios sea verificable por el otro.
 */
@Component
public class HmacService {

    private final Mac mac;

    public HmacService(@Value("${app.jwt.secret}") String secret) {
        try {
            this.mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("No se pudo inicializar HmacSHA256", e);
        }
    }

    public synchronized String firmar(String... campos) {
        String canonical = String.join("|", campos);
        byte[] digest = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) hex.append(String.format("%02x", b));
        return hex.toString();
    }
}
