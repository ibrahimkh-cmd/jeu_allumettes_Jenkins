package allumettes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StrategieExperteTest {

    private final Strategie strategie = new StrategieExperte();

    @Test
    void prendLaDerniereAllumette() {
        assertEquals(1, strategie.getPrise(new JeuClassique(1)));
    }

    /** Depuis une position gagnante, l'expert laisse 4k+1 allumettes à l'adversaire. */
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 6, 7, 8, 10, 11, 12})
    void laisseUnePositionPerdanteALAdversaire(int allumettes) {
        int prise = strategie.getPrise(new JeuClassique(allumettes));
        assertEquals(0, (allumettes - prise - 1) % (Jeu.PRISE_MAX + 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13})
    void joueToujoursUnCoupValide(int allumettes) {
        int prise = strategie.getPrise(new JeuClassique(allumettes));
        assertTrue(prise >= 1 && prise <= Jeu.PRISE_MAX && prise <= allumettes);
    }
}
