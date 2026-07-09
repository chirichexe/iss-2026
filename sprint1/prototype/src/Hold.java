public class Hold {

    private static Hold INSTANCE = new Hold();

    private final CellType[][] cells = {
        { CellType.FREE,   CellType.HOME,   CellType.FREE,     CellType.FREE,     CellType.FREE,     CellType.FREE,  CellType.FREE },
        { CellType.SONAR,  CellType.FREE,   CellType.SLOT1,    CellType.OBSTACLE, CellType.SLOT2,    CellType.FREE,  CellType.FREE }, 
        { CellType.FREE,   CellType.FREE,   CellType.FREE,     CellType.FREE,     CellType.FREE,     CellType.SLOT5, CellType.FREE },
        { CellType.FREE,   CellType.FREE,   CellType.SLOT3,    CellType.OBSTACLE, CellType.SLOT4,    CellType.FREE,  CellType.FREE },
        { CellType.FREE,   CellType.FREE,   CellType.FREE,     CellType.FREE,     CellType.FREE,     CellType.FREE,  CellType.FREE },
        { CellType.FREE,   CellType.FREE,   CellType.FREE,     CellType.FREE,     CellType.FREE,     CellType.FREE,  CellType.FREE },
        { CellType.IOPORT, CellType.FREE,   CellType.FREE,     CellType.FREE,     CellType.FREE,     CellType.FREE,  CellType.FREE } 
    };

    // slot occupati (SLOT1 ... SLOT5)
    private final boolean[] occupiedSlots = new boolean[5];

    public Hold() {
        for (int i = 0; i < occupiedSlots.length; i++) {
            occupiedSlots[i] = false;
        }
    }

    public Hold(String jsonFilePath) {
        this();
        if (jsonFilePath != null) {
            loadConfigFromJson(jsonFilePath);
        }
    }

    private void loadConfigFromJson(String filePath) {
        try {
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                return;
            }
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            String json = sb.toString();
            for (int i = 1; i <= 4; i++) {
                if (json.contains("\"slot" + i + "\": true") || json.contains("\"slot" + i + "\":true")) {
                    occupiedSlots[i - 1] = true;
                }
            }
        } catch (Exception e) {
            System.err.println("Hold | Error loading config from JSON: " + e.getMessage());
        }
    }

    public static Hold getInstance() {
        return INSTANCE;
    }


    // cerca il primo slot libero (SLOT1..SLOT4), lo marca come occupato e ne restituisce l'ID (1..4). Restituisce 0 se la stiva è piena.

    public static synchronized int reserveSlot() {
        return INSTANCE.doReserveSlot();
    }

    public synchronized int doReserveSlot() {
        for (int i = 0; i < 4; i++) {
            if (!occupiedSlots[i]) {
                occupiedSlots[i] = true;
                return i + 1;
            }
        }
        return 0; // 0 indica stiva piena
    }


    // Libera lo slot specificato (1..4) ripristinandone la disponibilità.
    public static synchronized void freeSlot(int slotId) {
        INSTANCE.doFreeSlot(slotId);
    }

    public synchronized void doFreeSlot(int slotId) {
        if (slotId >= 1 && slotId <= 5) {
            occupiedSlots[slotId - 1] = false;
        }
    }

    // Restituisce la coordinata cartesiana X dello slot sulla griglia 7x7.

    public static int getSlotX(int slotId) {
        return INSTANCE.doGetSlotX(slotId);
    }

    public int doGetSlotX(int slotId) {
        switch (slotId) {
            case 1: return 2;
            case 2: return 4;
            case 3: return 2;
            case 4: return 4;
            case 5: return 5;
            default: return 1; // HOME X
        }
    }

    // Restituisce la coordinata cartesiana Y dello slot sulla griglia 7x7.

    public static int getSlotY(int slotId) {
        return INSTANCE.doGetSlotY(slotId);
    }

    public int doGetSlotY(int slotId) {
        switch (slotId) {
            case 1: return 1;
            case 2: return 1;
            case 3: return 3;
            case 4: return 3;
            case 5: return 2;
            default: return 0; // HOME Y
        }
    }

    // Restituisce il tipo di cella (CellType) alle coordinate (x, y) specificate.
    public CellType getCell(int x, int y) {
        if (y >= 0 && y < cells.length && x >= 0 && x < cells[0].length) {
            return cells[y][x];
        }
        return CellType.FREE;
    }

    // Verifica se uno specifico slot risulta attualmente occupato.
    public boolean isOccupied(int slotId) {
        if (slotId >= 1 && slotId <= 5) {
            return occupiedSlots[slotId - 1];
        }
        return false;
    }
}
