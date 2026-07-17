public class Hold implements IHold {

    private static Hold INSTANCE = new Hold("hold_config.json");

    private int width = 8;
    private int height = 6;
    private int homeX = 0;
    private int homeY = 0;
    private int dfree = 150;

    private CellType[][] cells;

    // slot states: "free", "reserved", "occupied", "marker"
    private final String[] slotStates = new String[] { "free", "free", "free", "free", "marker" };
    private final int[] slotX = new int[] { 1, 4, 1, 4, 5 };
    private final int[] slotY = new int[] { 1, 1, 3, 3, 2 };

    public static int getDfree() {
        return INSTANCE.dfree;
    }

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

            // Extract home position
            int hX = extractInt(json, "\"home\"", "\"x\":", 0);
            int hY = extractInt(json, "\"home\"", "\"y\":", 0);
            this.homeX = hX;
            this.homeY = hY;
            if (homeY < height && homeX < width) {
                cells[homeY][homeX] = CellType.HOME;
            }

            // Extract I/O port position
            int ioX = extractInt(json, "\"ioport\"", "\"x\":", 0);
            int ioY = extractInt(json, "\"ioport\"", "\"y\":", 4);
            if (ioY < height && ioX < width) {
                cells[ioY][ioX] = CellType.IOPORT;
            }

            // Extract slot positions
            for (int id = 1; id <= 4; id++) {
                int sx = extractSlotX(json, id, slotX[id - 1]);
                int sy = extractSlotY(json, id, slotY[id - 1]);
                slotX[id - 1] = sx;
                slotY[id - 1] = sy;
                if (sy < height && sx < width) {
                    cells[sy][sx] = getSlotCellType(id);
                }
            }

            // Extract slot 5 position (marker)
            int s5x = extractSlotX(json, 5, slotX[4]);
            int s5y = extractSlotY(json, 5, slotY[4]);
            slotX[4] = s5x;
            slotY[4] = s5y;
            if (s5y < height && s5x < width) {
                cells[s5y][s5x] = CellType.SLOT5;
            }

            // Extract dfree value
            int dfreeVal = extractTopLevelInt(json, "\"dfree\"", 150);
            this.dfree = dfreeVal;

        } catch (Exception e) {
            System.err.println("Hold | Error loading config from JSON: " + e.getMessage());
        }
    }

    private int extractTopLevelInt(String json, String fieldToken, int defaultVal) {
        int fieldIdx = json.indexOf(fieldToken);
        if (fieldIdx < 0) return defaultVal;
        int start = fieldIdx + fieldToken.length();
        while (start < json.length() && (Character.isWhitespace(json.charAt(start)) || json.charAt(start) == ':' || json.charAt(start) == ',')) start++;
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        if (end > start) {
            return Integer.parseInt(json.substring(start, end));
        }
        return defaultVal;
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

    public static synchronized int reserveSlot() {
        return INSTANCE.doReserveSlot();
    }

    @Override
    public synchronized int doReserveSlot() {
        for (int i = 0; i < 4; i++) {
            if ("free".equals(slotStates[i])) {
                slotStates[i] = "reserved";
                return i + 1;
            }
        }
        return 0;
    }

    public static synchronized void freeSlot(int slotId) {
        INSTANCE.doFreeSlot(slotId);
    }

    @Override
    public synchronized void doFreeSlot(int slotId) {
        if (slotId >= 1 && slotId <= 4) {
            slotStates[slotId - 1] = "free";
        }
    }

    public static synchronized void occupySlot(int slotId) {
        INSTANCE.doOccupySlot(slotId);
    }

    @Override
    public synchronized void doOccupySlot(int slotId) {
        if (slotId >= 1 && slotId <= 4) {
            slotStates[slotId - 1] = "occupied";
        }
    }

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

    public static int getHomeX() {
        return INSTANCE.doGetHomeX();
    }

    @Override
    public int doGetHomeX() {
        return homeX;
    }

    public static int getHomeY() {
        return INSTANCE.doGetHomeY();
    }

    @Override
    public int doGetHomeY() {
        return homeY;
    }

    @Override
    public CellType getCell(int x, int y) {
        if (y >= 0 && y < height && x >= 0 && x < width) {
            return cells[y][x];
        }
        return CellType.FREE;
    }

    @Override
    public boolean isOccupied(int slotId) {
        if (slotId >= 1 && slotId <= 4) {
            return "occupied".equals(slotStates[slotId - 1]);
        }
        return false;
    }

    @Override
    public String getSlotState(int slotId) {
        if (slotId >= 1 && slotId <= 5) {
            return slotStates[slotId - 1];
        }
        return "free";
    }

    public static String toJson(String serviceState, String workingState, boolean ioPortOccupied, int reservedSlot) {
        return INSTANCE.doToJson(serviceState, workingState, ioPortOccupied, reservedSlot);
    }

    @Override
    public synchronized String doToJson(String serviceState, String workingState, boolean ioPortOccupied, int reservedSlot) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"serviceState\":\"").append(serviceState).append("\",");
        sb.append("\"workingState\":\"").append(workingState).append("\",");
        sb.append("\"ioPortOccupied\":").append(ioPortOccupied).append(",");
        sb.append("\"reservedSlot\":").append(reservedSlot).append(",");
        sb.append("\"slots\":{");
        sb.append("\"slot1\":\"").append(slotStates[0]).append("\",");
        sb.append("\"slot2\":\"").append(slotStates[1]).append("\",");
        sb.append("\"slot3\":\"").append(slotStates[2]).append("\",");
        sb.append("\"slot4\":\"").append(slotStates[3]).append("\",");
        sb.append("\"slot5\":\"").append(slotStates[4]).append("\"");
        sb.append("}");
        sb.append("}");
        return sb.toString();
    }

}
