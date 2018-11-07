package de.mpg.imeji.logic.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.mpg.imeji.j2j.annotations.j2jList;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jResource;

@j2jResource("http://imeji.org/terms/basismetadata")
public class BasisMetadata extends Properties {
	private static final long serialVersionUID = -6441013718789358371L;
	@j2jLiteral("http://purl.org/dc/elements/1.1/title")
	private String title;
	@j2jLiteral("http://purl.org/dc/elements/1.1/description")
	private String description;
	@j2jList("http://xmlns.com/foaf/0.1/person")
	protected Collection<Person> persons = new ArrayList<Person>();
	@j2jList("http://imeji.org/AdditionalInfo")
	private List<ContainerAdditionalInfo> additionalInformations = new ArrayList<>();

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Collection<Person> getPersons() {
		return persons;
	}

	public void setPersons(Collection<Person> person) {
		this.persons = person;
	}

	/**
	 * @return the additionalInformations
	 */
	public List<ContainerAdditionalInfo> getAdditionalInformations() {
		return additionalInformations;
	}

	/**
	 * @param additionalInformations
	 *            the additionalInformations to set
	 */
	public void setAdditionalInformations(List<ContainerAdditionalInfo> additionalInformations) {
		this.additionalInformations = additionalInformations;
	}

}
