package allumettes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class JeuProcurrationTest {

    @Test
    void donneLeNombreDAllumettesDuVraiJeu() {
        Jeu vrai = new JeuClassique(5);
        assertEquals(5, new JeuProcurration(vrai).getNombreAllumettes());
    }

    @Test
    void interditDeRetirerEtNeModifiePasLeVraiJeu() {
        Jeu vrai = new JeuClassique();
        Jeu procuration = new JeuProcurration(vrai);
        assertThrows(OperationInterditeException.class, () -> procuration.retirer(1));
        assertEquals(13, vrai.getNombreAllumettes());
    }
}
