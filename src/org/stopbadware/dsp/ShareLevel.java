package org.stopbadware.dsp;

public enum ShareLevel {
	SBW_ONLY {
		public String toString() {
			return "SBW";
		}
	},
	DSP_ONLY {
		public String toString() {
			return "DSP";
		}
	},
	TAKEDOWN {
		public String toString() {
			return "TD";
		}
	},
	RESEARCH {
		public String toString() {
			return "RES";
		}
	},
	PUBLIC {
		public String toString() {
			return "PUB";
		}
	};

	public static ShareLevel castFromString(String level) {
		ShareLevel shareLevel;
		if (level.equalsIgnoreCase(SBW_ONLY.toString())) {
			shareLevel = SBW_ONLY;
		} else if (level.equalsIgnoreCase(DSP_ONLY.toString())) {
			shareLevel = DSP_ONLY;
		}  else if (level.equalsIgnoreCase(TAKEDOWN.toString())) {
			shareLevel = TAKEDOWN;
		} else if (level.equalsIgnoreCase(RESEARCH.toString())) {
			shareLevel = RESEARCH;
		} else {
			shareLevel = PUBLIC;
		}
		return shareLevel;
	}
	
	public static ShareLevel getLeastRestrictive(ShareLevel oldLevel, ShareLevel newLevel) {
		return (oldLevel.compareTo(newLevel) >= 0) ? oldLevel : newLevel ;
	}
}
