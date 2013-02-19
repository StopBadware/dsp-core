package org.stopbadware.dsp;

import java.util.ArrayList;
import java.util.List;

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

	/**
	 * Converts string used as share levels to enums for
	 * comparision purposes
	 * @param level string representation of the share level, returned from
	 * each ShareLevel's toString() method
	 * @return a comparable ShareLevel enum
	 */
	public static ShareLevel castFromString(String level) {
		ShareLevel shareLevel;
		if (level.equalsIgnoreCase(PUBLIC.toString())) {
			shareLevel = PUBLIC;
		} else if (level.equalsIgnoreCase(DSP_ONLY.toString())) {
			shareLevel = DSP_ONLY;
		} else if (level.equalsIgnoreCase(TAKEDOWN.toString())) {
			shareLevel = TAKEDOWN;
		} else if (level.equalsIgnoreCase(RESEARCH.toString())) {
			shareLevel = RESEARCH;
		} else {
			shareLevel = SBW_ONLY;
		}
		return shareLevel;
	}
	
	/**
	 * Determines which of the two provided ShareLevels are the least restrictive
	 * @param a the first ShareLevel to compare
	 * @param b ShareLevel to compare to the first
	 * @return the least restrictive ShareLevel of the two
	 */
	public static ShareLevel getLeastRestrictive(ShareLevel a, ShareLevel b) {
		return (a.compareTo(b) >= 0) ? a : b ;
	}
	
	/**
	 * Creates an array of Strings containing all ShareLevels above
	 * the ShareLevel provided (including the provided ShareLevel itself)
	 * The concept of "above" refers to an ascending order of least restrictive
	 * to most restrictive ShareLevels
	 * @param lowest most restrictive ShareLevel to include
	 * @return array of Strings containing the provided ShareLevel and all 
	 * less restrictive levels
	 */
	public static String[] getAllAbove(ShareLevel lowest) {
		List<String> levels = new ArrayList<>(); 
		for (ShareLevel level : ShareLevel.values()) {
			if (level.compareTo(lowest) >= 0) {
				levels.add(level.toString());
			}
		}
		return levels.toArray(new String[levels.size()]);
	}
}
