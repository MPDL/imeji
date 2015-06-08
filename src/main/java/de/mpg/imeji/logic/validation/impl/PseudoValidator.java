package de.mpg.imeji.logic.validation.impl;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.validation.Validator;
import de.mpg.imeji.logic.vo.MetadataProfile;

/**
 * Validator which never validate... Usefull when an Object has no specific
 * validator defined
 * 
 * @author saquet
 *
 */
public class PseudoValidator implements Validator<Object> {

	@Override
	public void validate(Object t) throws UnprocessableError {
		// Do nothing
	}

	@Override
	public void validate(Object t, MetadataProfile p) throws UnprocessableError {
		// Do nothing
	}

}