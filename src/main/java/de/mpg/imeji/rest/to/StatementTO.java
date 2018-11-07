package de.mpg.imeji.rest.to;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.model.Statement;

/**
 * TO for {@link Statement}
 *
 * @author saquet
 *
 */
public class StatementTO implements Serializable {
	private static final long serialVersionUID = -5987536340352396442L;
	private String id;
	private String type;
	private URI vocabulary;
	private List<LiteralConstraintTO> literalConstraints = new ArrayList<LiteralConstraintTO>();
	private String minOccurs;
	private String maxOccurs;
	private String index;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public URI getVocabulary() {
		return vocabulary;
	}

	public void setVocabulary(URI vocabulary) {
		this.vocabulary = vocabulary;
	}

	public List<LiteralConstraintTO> getLiteralConstraints() {
		return literalConstraints;
	}

	public void setLiteralConstraints(List<LiteralConstraintTO> literalConstraints) {
		this.literalConstraints = literalConstraints;
	}

	public String getMinOccurs() {
		return minOccurs;
	}

	public void setMinOccurs(String minOccurs) {
		this.minOccurs = minOccurs;
	}

	public String getMaxOccurs() {
		return maxOccurs;
	}

	public void setMaxOccurs(String maxOccurs) {
		this.maxOccurs = maxOccurs;
	}

	/**
	 * @return the index
	 */
	public String getIndex() {
		return index;
	}

	/**
	 * @param index
	 *            the index to set
	 */
	public void setIndex(String index) {
		this.index = index;
	}
}
