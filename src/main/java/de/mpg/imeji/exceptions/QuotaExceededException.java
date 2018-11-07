package de.mpg.imeji.exceptions;

/**
 * Created by vlad on 13.08.15.
 */
public class QuotaExceededException extends ImejiException {

	private static final long serialVersionUID = 145948204335897106L;

	public QuotaExceededException(String message) {
		super(message);
		minimizeStacktrace();
	}

}
