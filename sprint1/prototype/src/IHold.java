/**
 * Interfaccia per la gestione della stiva (Hold) del Cargo System.
 * Definisce i metodi di istanza per prenotare e liberare slot, interrogare le coordinate
 * degli slot e della HOME e verificare lo stato di occupazione.
 */
public interface IHold {
    int doReserveSlot();
    void doFreeSlot(int slotId);
    int doGetSlotX(int slotId);
    int doGetSlotY(int slotId);
    int doGetHomeX();
    int doGetHomeY();
    CellType getCell(int x, int y);
    boolean isOccupied(int slotId);
}
