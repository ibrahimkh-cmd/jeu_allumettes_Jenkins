package allumettes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class StrategieTricheTest {

    @Test
    void trichePourNeLaisserQueDeuxAllumettes() {
        Jeu jeu = new JeuClassique();
        int prise = new StrategieTriche().getPrise(jeu);
        assertEquals(2, jeu.getNombreAllumettes());
        assertEquals(1, prise);
    }

    @Test
    void estDetecteeAvecUneProcuration() {
        Jeu procuration = new JeuProcurration(new JeuClassique());
        assertThrows(OperationInterditeException.class,
                () -> new StrategieTriche().getPrise(procuration));
    }
}
