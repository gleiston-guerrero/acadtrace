package ec.uteq.sga.secretaria.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CryptoServiceTest {

    private CryptoService newService() {
        byte[] key32 = new byte[32];
        System.arraycopy("sga-secretaria-test-key-32bytes!".getBytes(), 0, key32, 0, 32);
        return new CryptoService(Base64.getEncoder().encodeToString(key32));
    }

    @Test
    void encryptThenDecrypt_returnsOriginalValue() {
        CryptoService crypto = newService();
        String original = "Av. Siempre Viva 742, Quito";

        String encrypted = crypto.encrypt(original);

        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);
        assertEquals(original, crypto.decrypt(encrypted));
    }

    @Test
    void encrypt_producesDifferentCiphertextEachTime() {
        CryptoService crypto = newService();
        String original = "0991234567";

        String first = crypto.encrypt(original);
        String second = crypto.encrypt(original);

        assertNotEquals(first, second, "el nonce aleatorio debe variar el ciphertext entre llamadas");
        assertEquals(original, crypto.decrypt(first));
        assertEquals(original, crypto.decrypt(second));
    }

    @Test
    void nullInput_staysNull() {
        CryptoService crypto = newService();

        assertNull(crypto.encrypt(null));
        assertNull(crypto.decrypt(null));
    }

    @Test
    void constructor_rejectsKeyWithWrongLength() {
        String shortKey = Base64.getEncoder().encodeToString("too-short".getBytes());
        assertThrows(IllegalStateException.class, () -> new CryptoService(shortKey));
    }
}
