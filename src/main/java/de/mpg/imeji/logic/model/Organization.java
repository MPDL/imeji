package de.mpg.imeji.logic.model;

import java.io.Serializable;
import java.net.URI;

import javax.xml.bind.annotation.XmlAttribute;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.util.IdentifierUtil;

/**
 * An organization
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource("http://purl.org/escidoc/metadata/profiles/0.1/organizationalunit")
@j2jId(getMethod = "getId", setMethod = "setId")
public class Organization implements Cloneable, Serializable {
	private static final long serialVersionUID = -7541779415288019910L;
	private URI id;
	@j2jLiteral("http://purl.org/dc/terms/title")
	private String name;
	@j2jLiteral("http://imeji.org/terms/position")
	private int pos = 0;

	public Organization() {
		id = IdentifierUtil.newURI(Organization.class);
	}

	public Organization(String name) {
		this();
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPos() {
		return pos;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}

	public int compareTo(Organization o) {
		if (o.getPos() > this.pos) {
			return -1;
		} else if (o.getPos() == this.pos) {
			return 0;
		} else {
			return 1;
		}
	}

	public void setId(URI id) {
		this.id = id;
	}

	@XmlAttribute(name = "id")
	public URI getId() {
		return id;
	}

	@Override
	public Organization clone() {
		final Organization clone = new Organization();
		clone.name = this.name;
		clone.pos = this.pos;
		return clone;
	}
}
