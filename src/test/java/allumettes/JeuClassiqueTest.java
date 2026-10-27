package allumettes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JeuClassiqueTest {

    @Test
    void commenceAvec13AllumettesParDefaut() {
        assertEquals(13, new JeuClassique().getNombreAllumettes());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void retirerUnNombreValideDecrementeLeJeu(int prise) throws CoupInvalideException {
        Jeu jeu = new JeuClassique();
        jeu.retirer(prise);
        assertEquals(13 - prise, jeu.getNombreAllumettes());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void refuseUnePriseInferieureA1(int prise) {
        Jeu jeu = new JeuClassique();
        CoupInvalideException e = assertThrows(CoupInvalideException.class,
                () -> jeu.retirer(prise));
        assertEquals(prise, e.getCoup());
        assertEquals("<1", e.getProbleme());
        assertEquals(13, jeu.getNombreAllumettes());
    }

    @Test
    void refuseUnePriseSuperieureAuMax() {
        Jeu jeu = new JeuClassique();
        CoupInvalideException e = assertThrows(CoupInvalideException.class,
                () -> jeu.retirer(Jeu.PRISE_MAX + 1));
        assertEquals(">" + Jeu.PRISE_MAX, e.getProbleme());
    }

    @Test
    void refuseDePrendrePlusQueCeQuiReste() {
        Jeu jeu = new JeuClassique(2);
        CoupInvalideException e = assertThrows(CoupInvalideException.class,
                () -> jeu.retirer(3));
        assertEquals(">2", e.getProbleme());
        assertEquals(2, jeu.getNombreAllumettes());
    }
}
