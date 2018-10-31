package de.mpg.imeji.rest.to;

import java.io.Serializable;

public class LocalizedStringTO implements Serializable {

	private static final long serialVersionUID = 2406077359700974056L;
	private String value;
	private String lang;

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

}
