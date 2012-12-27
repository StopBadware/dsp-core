package org.stopbadware.dsp;

public class AutonomousSystem {
	
	private int asn;
	private String country;
	private String name;
	
	public AutonomousSystem() {
		
	}

	public AutonomousSystem(int asn, String country, String name) {
		this.asn = asn;
		this.country = country;
		this.name = name;
	}

	public int getASN() {
		return asn;
	}

	public void setASN(int asn) {
		this.asn = asn;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
