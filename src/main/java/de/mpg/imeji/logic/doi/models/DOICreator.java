package de.mpg.imeji.logic.doi.models;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DOICreator implements Serializable {
	private static final long serialVersionUID = 4500731404759806944L;
	private String creatorName;

	public DOICreator() {

	}

	public DOICreator(String name) {
		this.creatorName = name;
	}

	public String getCreatorName() {
		return creatorName;
	}

	public void setCreatorName(String creatorName) {
		this.creatorName = creatorName;
	}
}
