package org.stopbadware.dsp.json;

public class Error extends SearchResults implements Response {

	private int code = 0;
	private String error = "";
	
	public static final int NOT_PERMITTED = 42;
	public static final int BAD_FORMAT = 47;
	public static final int DATABASE = 54;
	
	public Error(int code) {
		this(code, null);
	}
	
	public Error(int code, String error) {
		this.code = code;
		switch (code) {
			case BAD_FORMAT:
				this.error = "Incorrectly formatted request";
				break;
			case DATABASE:
				this.error = "Unable to execute query";
				break;
			default:
				break;
		}
		
		if (error != null && error.length() > 0) {
			this.error += ": "+error;
		}
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}
}
