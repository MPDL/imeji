package de.mpg.imeji.logic.doi.models;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlType
public class DOIIdentifier implements Serializable {
	private static final long serialVersionUID = -3885850011298834578L;
	private String identifierType = "DOI";

	@XmlValue
	private final String identifier = "";

	public DOIIdentifier() {

	}

	@XmlAttribute
	public String getIdentifierType() {
		return identifierType;
	}

	public void setIdentifierType(String identifierType) {
		this.identifierType = identifierType;
	}

}
