package de.mpg.imeji.logic.search.elasticsearch.model;

import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.model.Organization;
import de.mpg.imeji.logic.model.Person;

/**
 * The Elastic representation of a {@link Person}
 *
 * @author bastiens
 *
 */
public class ElasticPerson {
	private final String familyname;
	private final String givenname;
	private final String completename;
	private final String identifier;
	private final List<String> organization = new ArrayList<>();

	public ElasticPerson() {
		this.familyname = null;
		this.givenname = null;
		this.identifier = null;
		this.completename = null;
	}

	/**
	 * Constructor for a {@link Person}
	 *
	 * @param p
	 */
	public ElasticPerson(Person p) {
		if (p == null) {
			this.familyname = null;
			this.givenname = null;
			this.identifier = null;
			this.completename = null;
		} else {
			this.familyname = p.getFamilyName();
			this.givenname = p.getGivenName();
			this.identifier = p.getIdentifier();
			this.completename = p.getCompleteName();
			for (final Organization org : p.getOrganizations()) {
				organization.add(org.getName());
			}
		}
	}

	/**
	 * @return the familyname
	 */
	public String getFamilyname() {
		return familyname;
	}

	/**
	 * @return the givenname
	 */
	public String getGivenname() {
		return givenname;
	}

	/**
	 * @return the completename
	 */
	public String getCompletename() {
		return completename;
	}

	/**
	 * @return the identifier
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * @return the organisation
	 */
	public List<String> getOrganization() {
		return organization;
	}

}
