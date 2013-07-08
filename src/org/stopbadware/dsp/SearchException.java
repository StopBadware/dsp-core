package org.stopbadware.dsp;

public class SearchException extends Exception {
	
	private int code = 0;

	private static final long serialVersionUID = 6017982276104534064L;
	
	public SearchException(String msg, int code) {
		super(msg);
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}

}
