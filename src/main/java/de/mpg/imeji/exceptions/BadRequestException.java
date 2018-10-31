package de.mpg.imeji.exceptions;

/**
 *
 */
public class BadRequestException extends ImejiException {
	private static final long serialVersionUID = -3721639144291667847L;

	/**
	 * Constructor for HTTP 400 BadRequest
	 */

	public BadRequestException(String message) {
		super(message);
	}

	public BadRequestException(String message, Throwable e) {
		super(message, e);
	}

}
