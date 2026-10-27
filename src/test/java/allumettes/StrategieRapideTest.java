package allumettes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StrategieRapideTest {

    private final Strategie strategie = new StrategieRapide();

    @Test
    void prendLeMaximumQuandIlEnResteAssez() {
        assertEquals(Jeu.PRISE_MAX, strategie.getPrise(new JeuClassique()));
    }

    @Test
    void prendCeQuiResteQuandIlEnResteMoinsQueLeMax() throws CoupInvalideException {
        Jeu jeu = new JeuClassique(5);
        jeu.retirer(Jeu.PRISE_MAX);
        assertEquals(2, strategie.getPrise(jeu));
    }

    @Test
    void prendLaDerniereAllumette() {
        assertEquals(1, strategie.getPrise(new JeuClassique(1)));
    }
}
