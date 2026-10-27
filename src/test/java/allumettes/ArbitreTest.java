package allumettes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArbitreTest {

    private final PrintStream sortieOriginale = System.out;
    private final ByteArrayOutputStream sortie = new ByteArrayOutputStream();

    @BeforeEach
    void capturerLaSortie() {
        System.setOut(new PrintStream(sortie));
    }

    @AfterEach
    void restaurerLaSortie() {
        System.setOut(sortieOriginale);
    }

    @Test
    void rapideContreRapideLePremierPerd() {
        Jeu jeu = new JeuClassique();
        new Arbitre(new Joueur("J1", new StrategieRapide()),
                new Joueur("J2", new StrategieRapide())).arbitrer(jeu);

        assertEquals(0, jeu.getNombreAllumettes());
        assertTrue(sortie.toString().contains("J1 perd !"));
        assertTrue(sortie.toString().contains("J2 gagne !"));
    }

    @Test
    void lExpertEnSecondBatLeRapide() {
        new Arbitre(new Joueur("Rapide", new StrategieRapide()),
                new Joueur("Expert", new StrategieExperte())).arbitrer(new JeuClassique());

        assertTrue(sortie.toString().contains("Expert gagne !"));
    }

    @Test
    void unArbitreConfiantLaisseLeTricheurGagner() {
        Arbitre arbitre = new Arbitre(new Joueur("Tricheur", new StrategieTriche()),
                new Joueur("Rapide", new StrategieRapide()));
        arbitre.setConfiant();
        arbitre.arbitrer(new JeuClassique());

        assertTrue(sortie.toString().contains("Tricheur gagne !"));
    }
}
