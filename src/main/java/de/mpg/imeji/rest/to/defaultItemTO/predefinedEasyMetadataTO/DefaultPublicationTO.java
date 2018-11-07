package de.mpg.imeji.rest.to.defaultItemTO.predefinedEasyMetadataTO;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class DefaultPublicationTO implements Serializable {

	private static final long serialVersionUID = 1207174642221784716L;
	private String format;
	private String publication;
	private String citation;

	public String getPublication() {
		return publication;
	}

	public void setPublication(String publication) {
		this.publication = publication;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getCitation() {
		return citation;
	}

	public void setCitation(String citation) {
		this.citation = citation;
	}

}
