/**
 *
 */
package de.mpg.imeji.exceptions;

public class NotSupportedMethodException extends ImejiException {

	/**
	 *
	 */
	private static final long serialVersionUID = -474498315581861322L;

	public NotSupportedMethodException() {
		super("Method is not supported.");
	}

	public NotSupportedMethodException(String extraMesg) {
		super("Method is not supported: " + extraMesg);
	}

}
