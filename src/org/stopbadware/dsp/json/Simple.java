package org.stopbadware.dsp.json;

public class Simple implements Response {
	
	private String msg = "";
	
	public Simple(String msg) {
		this.msg = msg;
	}
	
	public String getMsg() {
		return msg;
	}
}
