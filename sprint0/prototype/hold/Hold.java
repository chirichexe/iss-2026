public enum CellType {
    FREE, OBSTACLE, HOME, SONAR, IOPORT, SLOT1, SLOT2, SLOT3, SLOT4, SLOT5
}

public class Hold {

    private final CellType[][] cells = {

        { FREE,     HOME,     FREE,      FREE,      FREE,      FREE,  FREE },
        { SONAR,   FREE,     SLOT1,     OBSTACLE,  SLOT2,     FREE,  FREE }, 
        { FREE,     FREE,     FREE,      FREE,      FREE,      SLOT5, FREE },
        { FREE,     FREE,     SLOT3,     OBSTACLE,  SLOT4,     FREE,  FREE },
        { FREE,     FREE,     FREE,      FREE,      FREE,      FREE,  FREE },
        { FREE,     FREE,     FREE,      FREE,      FREE,      FREE,  FREE },
        { IOPORT,   FREE,     FREE,      FREE,      FREE,      FREE,  FREE } 
    };

    // slot occupati (SLOT1 ... SLOT5)
    private final boolean[] occupiedSlots = new boolean[5];
}
