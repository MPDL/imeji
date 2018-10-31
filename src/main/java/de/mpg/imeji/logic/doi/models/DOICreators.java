package de.mpg.imeji.logic.doi.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DOICreators implements Serializable {
	private static final long serialVersionUID = -3568115433437845941L;
	private List<DOICreator> creator = new ArrayList<>();

	public DOICreators() {

	}

	/**
	 * @return the creator
	 */
	public List<DOICreator> getCreator() {
		return creator;
	}

	/**
	 * @param creator
	 *            the creator to set
	 */
	public void setCreator(List<DOICreator> creator) {
		this.creator = creator;
	}

}
