package allumettes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JoueurTest {

    @Test
    void delegueLaPriseASaStrategie() {
        Joueur joueur = new Joueur("Brahim", jeu -> 2);
        assertEquals("Brahim", joueur.getNom());
        assertEquals(2, joueur.getPrise(new JeuClassique()));
    }
}
