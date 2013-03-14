package org.stopbadware.dsp;

public enum Mode {
	DEV,
	TEST,
	STAGING,
	PRODUCTION;
	
	public static Mode castFromString(String str) {
		Mode mode = DEV;
		if (str != null && str.length() > 0) {
			for (Mode m : Mode.values()) {
				if (str.equalsIgnoreCase(m.toString())) {
					mode = m;
					break;
				}
			}
		}
		return mode;
	}
		
	
	public static Mode getCurrentMode() {
		return castFromString(System.getenv("MODE"));
	}
}
