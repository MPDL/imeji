package de.mpg.imeji.logic.validation.impl;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.util.StringHelper;

public class FacetValidator extends ObjectValidator implements Validator<Facet> {

	@Override
	public void validate(Facet facet, de.mpg.imeji.logic.validation.impl.Validator.Method method)
			throws UnprocessableError {
		UnprocessableError error = new UnprocessableError();
		if (StringHelper.isNullOrEmptyTrim(facet.getName())) {
			error = new UnprocessableError("Facets must have a name", error);
		}
		if (StringHelper.isNullOrEmptyTrim(facet.getIndex()) || StringHelper.isNullOrEmptyTrim(facet.getIndex())) {
			error = new UnprocessableError("Please select a facet of a metadata", error);
		}
		if (error.hasMessages()) {
			throw error;
		}
	}

}
