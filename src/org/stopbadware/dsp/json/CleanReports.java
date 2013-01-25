package org.stopbadware.dsp.json;

import java.util.Set;

public class CleanReports {
	
	private int size;
	private long time;
	private Set<String> clean;
	
	public CleanReports() {
		clean = null;
		size = 0;
		time = 0;
	}
	
	public CleanReports(Set<String> clean) {
		this.clean = clean;
		size = clean.size();
		time = System.currentTimeMillis() / 1000;
	}

	public int getSize() {
		return size;
	}

	public long getTime() {
		return time;
	}

	public Set<String> getClean() {
		return clean;
	}

}
