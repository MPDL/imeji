package de.mpg.imeji.rest.version.exception;

import de.mpg.imeji.exceptions.ImejiException;

/**
 * Exception when a request is done to the API with an unknown API version
 *
 * @author saquet
 *
 */
public class UnknowAPIVersionException extends ImejiException {
	private static final long serialVersionUID = 2759977782360745354L;

	public UnknowAPIVersionException(String message) {
		super(message);
		minimizeStacktrace();
	}

}
