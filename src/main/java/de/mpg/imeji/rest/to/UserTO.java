package de.mpg.imeji.rest.to;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.mpg.imeji.logic.model.User;

/**
 * TO for {@link User}
 *
 * @author bastiens
 *
 */
@JsonInclude(Include.NON_NULL)
public class UserTO {
	private String id;
	private String email;
	private long quota;
	private String apiKey;
	private PersonTO person = new PersonTO();

	public UserTO() {
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email
	 *            the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @return the quota
	 */
	public long getQuota() {
		return quota;
	}

	/**
	 * @param quota
	 *            the quota to set
	 */
	public void setQuota(long quota) {
		this.quota = quota;
	}

	/**
	 * @return the apiKey
	 */
	public String getApiKey() {
		return apiKey;
	}

	/**
	 * @param apiKey
	 *            the apiKey to set
	 */
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	/**
	 * @return the person
	 */
	public PersonTO getPerson() {
		return person;
	}

	/**
	 * @param person
	 *            the person to set
	 */
	public void setPerson(PersonTO person) {
		this.person = person;
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

}
