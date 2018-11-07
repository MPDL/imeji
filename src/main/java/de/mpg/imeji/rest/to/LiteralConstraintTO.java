package de.mpg.imeji.rest.to;

import java.io.Serializable;

public class LiteralConstraintTO implements Serializable {

	private static final long serialVersionUID = -5472900602335641203L;

	private String value;

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
