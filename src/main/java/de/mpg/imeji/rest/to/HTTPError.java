package de.mpg.imeji.rest.to;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class HTTPError implements Serializable {
	private static final long serialVersionUID = -763007655028104486L;
	public String code;
	public String title;
	public String message;
	public String exceptionReport;
	public String id;

	public String getExceptionReport() {
		return exceptionReport;
	}

	public void setExceptionReport(String exceptionReport) {
		this.exceptionReport = exceptionReport;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
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
