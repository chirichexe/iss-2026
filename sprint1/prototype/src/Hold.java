public class Hold implements IHold {

    private static Hold INSTANCE = new Hold("hold_config.json");

    private int width = 8;
    private int height = 6;
    private int homeX = 0;
    private int homeY = 0;

    private CellType[][] cells;

    // slot occupati (SLOT1 ... SLOT5)
    private final boolean[] occupiedSlots = new boolean[5];
    private final int[] slotX = new int[] { 1, 4, 1, 4, 5 };
    private final int[] slotY = new int[] { 1, 1, 3, 3, 2 };

    public Hold() {
        initDefaultGrid();
    }

    public Hold(String jsonFilePath) {
        initDefaultGrid();
        if (jsonFilePath != null) {
            loadConfigFromJson(jsonFilePath);
        }
    }

    private void initDefaultGrid() {
        cells = new CellType[height][width];
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                cells[r][c] = CellType.FREE;
            }
        }
        cells[homeY][homeX] = CellType.HOME;
    }

    private void loadConfigFromJson(String filePath) {
        try {
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                file = new java.io.File("src/" + filePath);
            }
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

            // Estrai HOME position
            int hX = extractInt(json, "\"home\"", "\"x\":", 0);
            int hY = extractInt(json, "\"home\"", "\"y\":", 0);
            this.homeX = hX;
            this.homeY = hY;
            if (homeY < height && homeX < width) {
                cells[homeY][homeX] = CellType.HOME;
            }

            // Estrai IOPORT position
            int ioX = extractInt(json, "\"ioport\"", "\"x\":", 0);
            int ioY = extractInt(json, "\"ioport\"", "\"y\":", 4);
            if (ioY < height && ioX < width) {
                cells[ioY][ioX] = CellType.IOPORT;
            }

            // Estrai working slots 1..4
            for (int id = 1; id <= 4; id++) {
                int sx = extractSlotX(json, id, slotX[id - 1]);
                int sy = extractSlotY(json, id, slotY[id - 1]);
                slotX[id - 1] = sx;
                slotY[id - 1] = sy;
                if (sy < height && sx < width) {
                    cells[sy][sx] = getSlotCellType(id);
                }
            }

            // Estrai labeling slot 5
            int s5x = extractSlotX(json, 5, slotX[4]);
            int s5y = extractSlotY(json, 5, slotY[4]);
            slotX[4] = s5x;
            slotY[4] = s5y;
            if (s5y < height && s5x < width) {
                cells[s5y][s5x] = CellType.SLOT5;
            }

        } catch (Exception e) {
            System.err.println("Hold | Error loading config from JSON: " + e.getMessage());
        }
    }

    private int extractInt(String json, String sectionToken, String fieldToken, int defaultVal) {
        int secIdx = json.indexOf(sectionToken);
        if (secIdx < 0) return defaultVal;
        int fieldIdx = json.indexOf(fieldToken, secIdx);
        if (fieldIdx < 0) return defaultVal;
        int start = fieldIdx + fieldToken.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        if (end > start) {
            return Integer.parseInt(json.substring(start, end));
        }
        return defaultVal;
    }

    private int extractSlotX(String json, int id, int defaultVal) {
        int idIdx = json.indexOf("\"id\": " + id);
        if (idIdx < 0) idIdx = json.indexOf("\"id\":" + id);
        if (idIdx < 0) return defaultVal;
        return extractInt(json.substring(idIdx), "\"position\"", "\"x\":", defaultVal);
    }

    private int extractSlotY(String json, int id, int defaultVal) {
        int idIdx = json.indexOf("\"id\": " + id);
        if (idIdx < 0) idIdx = json.indexOf("\"id\":" + id);
        if (idIdx < 0) return defaultVal;
        return extractInt(json.substring(idIdx), "\"position\"", "\"y\":", defaultVal);
    }

    private CellType getSlotCellType(int id) {
        switch (id) {
            case 1: return CellType.SLOT1;
            case 2: return CellType.SLOT2;
            case 3: return CellType.SLOT3;
            case 4: return CellType.SLOT4;
            case 5: return CellType.SLOT5;
            default: return CellType.FREE;
        }
    }

    public static Hold getInstance() {
        return INSTANCE;
    }

    /**
     * Cerca il primo slot libero (SLOT1..SLOT4) in modo thread-safe,
     * lo marca come occupato e ne restituisce l'ID (1..4). Restituisce 0 se la stiva è piena.
     */
    public static synchronized int reserveSlot() {
        return INSTANCE.doReserveSlot();
    }

    @Override
    public synchronized int doReserveSlot() {
        for (int i = 0; i < 4; i++) {
            if (!occupiedSlots[i]) {
                occupiedSlots[i] = true;
                return i + 1;
            }
        }
        return 0; // 0 indica stiva piena
    }

    /**
     * Libera lo slot specificato (1..4) ripristinandone la disponibilità.
     */
    public static synchronized void freeSlot(int slotId) {
        INSTANCE.doFreeSlot(slotId);
    }

    @Override
    public synchronized void doFreeSlot(int slotId) {
        if (slotId >= 1 && slotId <= 5) {
            occupiedSlots[slotId - 1] = false;
        }
    }

    /**
     * Restituisce la coordinata cartesiana X dello slot caricata dalla configurazione JSON.
     */
    public static int getSlotX(int slotId) {
        return INSTANCE.doGetSlotX(slotId);
    }

    @Override
    public int doGetSlotX(int slotId) {
        if (slotId >= 1 && slotId <= 5) {
            return slotX[slotId - 1];
        }
        return homeX;
    }

    /**
     * Restituisce la coordinata cartesiana Y dello slot caricata dalla configurazione JSON.
     */
    public static int getSlotY(int slotId) {
        return INSTANCE.doGetSlotY(slotId);
    }

    @Override
    public int doGetSlotY(int slotId) {
        if (slotId >= 1 && slotId <= 5) {
            return slotY[slotId - 1];
        }
        return homeY;
    }

    /**
     * Restituisce la coordinata X della HOME.
     */
    public static int getHomeX() {
        return INSTANCE.doGetHomeX();
    }

    @Override
    public int doGetHomeX() {
        return homeX;
    }

    /**
     * Restituisce la coordinata Y della HOME.
     */
    public static int getHomeY() {
        return INSTANCE.doGetHomeY();
    }

    @Override
    public int doGetHomeY() {
        return homeY;
    }

    /**
     * Restituisce il tipo di cella (CellType) alle coordinate (x, y) specificate.
     */
    @Override
    public CellType getCell(int x, int y) {
        if (y >= 0 && y < height && x >= 0 && x < width) {
            return cells[y][x];
        }
        return CellType.FREE;
    }

    /**
     * Verifica se uno specifico slot risulta attualmente occupato.
     */
    @Override
    public boolean isOccupied(int slotId) {
        if (slotId >= 1 && slotId <= 5) {
            return occupiedSlots[slotId - 1];
        }
        return false;
    }
}

