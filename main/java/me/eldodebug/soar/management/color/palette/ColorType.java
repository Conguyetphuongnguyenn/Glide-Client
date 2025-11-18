package me.eldodebug.soar.management.color.palette;

public enum ColorType {
	DARK(0),
	NORMAL(1),
	BACKGROUND(2),
	ON_BACKGROUND(3),
	SURFACE(4),
	ON_SURFACE(5),
	SURFACE_VARIANT(6),
	OUTLINE(7),
	PRIMARY(8),
	ON_PRIMARY(9);
	
	private final int index;
	
	private ColorType(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}
}