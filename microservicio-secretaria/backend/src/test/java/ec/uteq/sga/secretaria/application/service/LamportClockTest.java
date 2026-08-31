package ec.uteq.sga.secretaria.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pruebas Unitarias: LamportClock (Microservicio Secretaría)")
class LamportClockTest {

    @Test
    @DisplayName("tick incrementa secuencialmente el reloj de Lamport")
    void test_tick_incrementsSequentially() {
        LamportClock clock = new LamportClock();
        assertThat(clock.current()).isEqualTo(0L);

        long t1 = clock.tick();
        assertThat(t1).isEqualTo(1L);
        assertThat(clock.current()).isEqualTo(1L);

        long t2 = clock.tick();
        assertThat(t2).isEqualTo(2L);
    }

    @Test
    @DisplayName("update sincroniza con timestamp remoto mayor: max(local, remote) + 1")
    void test_update_withHigherRemoteTimestamp() {
        LamportClock clock = new LamportClock();
        clock.tick(); // local = 1

        long updated = clock.update(10L);
        assertThat(updated).isEqualTo(11L);
        assertThat(clock.current()).isEqualTo(11L);
    }

    @Test
    @DisplayName("update con timestamp remoto menor incrementa sobre local: local + 1")
    void test_update_withLowerRemoteTimestamp() {
        LamportClock clock = new LamportClock();
        clock.seed(20L); // local = 20

        long updated = clock.update(5L);
        assertThat(updated).isEqualTo(21L);
        assertThat(clock.current()).isEqualTo(21L);
    }

    @Test
    @DisplayName("seed adelanta el reloj sin incrementar de más si es mayor que local")
    void test_seed_setsBaseline() {
        LamportClock clock = new LamportClock();
        long seeded = clock.seed(50L);
        assertThat(seeded).isEqualTo(50L);
        assertThat(clock.current()).isEqualTo(50L);

        long seededLower = clock.seed(30L);
        assertThat(seededLower).isEqualTo(50L);
    }
}
