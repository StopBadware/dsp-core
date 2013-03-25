package org.stopbadware.dsp.json;

public class Error implements Response {

	private int code = 0;
	private String msg = null;
	
	public Error(int code, String msg) {
		this.setCode(code);
		this.setMsg(msg);
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
}
