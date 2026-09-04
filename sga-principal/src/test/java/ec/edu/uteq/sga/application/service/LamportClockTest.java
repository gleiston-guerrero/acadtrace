package ec.edu.uteq.sga.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LamportClockTest {

    private LamportClock clock;

    @BeforeEach
    void setUp() {
        clock = new LamportClock();
    }

    @Test
    void tick_incrementaSecuencialmente() {
        assertEquals(1, clock.tick());
        assertEquals(2, clock.tick());
        assertEquals(3, clock.tick());
    }

    @Test
    void update_adelantaRelojSiRemotoEsMayor() {
        clock.tick(); // local = 1
        long updated = clock.update(10); // local = max(1, 10) + 1 = 11
        assertEquals(11, updated);
        assertEquals(12, clock.tick());
    }

    @Test
    void update_noRetrocedeSiRemotoEsMenor() {
        clock.seed(20);
        long updated = clock.update(5); // local = max(20, 5) + 1 = 21
        assertEquals(21, updated);
    }

    @Test
    void seed_fijaPisoSinDecrementar() {
        assertEquals(50, clock.seed(50));
        assertEquals(50, clock.seed(30)); // no baja
        assertEquals(51, clock.tick());
    }
}
