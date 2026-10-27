package allumettes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.RepeatedTest;

class StrategieNaifTest {

    @RepeatedTest(50)
    void prendEntre1EtLeMax() {
        int prise = new StrategieNaif().getPrise(new JeuClassique());
        assertTrue(prise >= 1 && prise <= Jeu.PRISE_MAX, "prise = " + prise);
    }
}
