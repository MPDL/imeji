/**
 *
 */
package de.mpg.imeji.logic.validation.impl;

/**
 * @author natasab
 *
 */
public abstract class ObjectValidator {

	protected Validator.Method validateForMethod;

	/**
	 * @param validateForMethod
	 *            the validateForMethod to set
	 */
	public void setValidateForMethod(Validator.Method validateForMethod) {
		this.validateForMethod = validateForMethod;
	}

	public Validator.Method getValidateForMethod() {
		return validateForMethod;
	}

	public boolean isDelete() {
		return this.validateForMethod.equals(Validator.Method.DELETE);
	}

	public boolean isCreate() {
		return this.validateForMethod.equals(Validator.Method.CREATE);
	}

	public boolean isUpdate() {
		return this.validateForMethod.equals(Validator.Method.UPDATE);
	}

}
