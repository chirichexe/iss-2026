package it.unibo.hold;

public class Hold {

	public enum CellType {
		FREE, OBSTACLE, HOME, IOPORT, SLOT1, SLOT2, SLOT3, SLOT4, SLOT5
	}

	private final CellType[][] cells = {
			{ CellType.FREE, CellType.HOME, CellType.FREE, CellType.FREE, CellType.FREE, CellType.FREE, CellType.FREE },
			{ CellType.FREE, CellType.FREE, CellType.SLOT1, CellType.OBSTACLE, CellType.SLOT2, CellType.FREE,
					CellType.FREE },
			{ CellType.FREE, CellType.FREE, CellType.FREE, CellType.FREE, CellType.FREE, CellType.SLOT5,
					CellType.FREE },
			{ CellType.FREE, CellType.FREE, CellType.SLOT3, CellType.OBSTACLE, CellType.SLOT4, CellType.FREE,
					CellType.FREE },
			{ CellType.FREE, CellType.FREE, CellType.FREE, CellType.FREE, CellType.FREE, CellType.FREE, CellType.FREE },
			{ CellType.FREE, CellType.FREE, CellType.FREE, CellType.FREE, CellType.FREE, CellType.FREE, CellType.FREE },
			{ CellType.IOPORT, CellType.FREE, CellType.FREE, CellType.FREE, CellType.FREE, CellType.FREE,
					CellType.FREE } };

	// slot occupati (SLOT1 ... SLOT5)
	private final boolean[] occupiedSlots = new boolean[5];
}
