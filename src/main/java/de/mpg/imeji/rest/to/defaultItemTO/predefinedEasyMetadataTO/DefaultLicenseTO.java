package de.mpg.imeji.rest.to.defaultItemTO.predefinedEasyMetadataTO;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class DefaultLicenseTO implements Serializable {

	private static final long serialVersionUID = 4218264604177901345L;

	private String license;

	private String url;

	public String getLicense() {
		return license;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

}
