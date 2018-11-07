package de.mpg.imeji.rest.to;

import java.io.Serializable;

import de.mpg.imeji.logic.model.CollectionElement;

/**
 * TO for {@link CollectionElement}
 * 
 * @author saquet
 *
 */
public class CollectionElementTO implements Serializable {
	private static final long serialVersionUID = 1511020792626861256L;
	public String name;
	public String id;
	public String type;

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
}
