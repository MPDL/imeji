package de.mpg.imeji.presentation.item.edit.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 * Converter for Double: Display NaN as empty string, and transform empty String
 * as NaN
 *
 * @author saquet
 */
@FacesConverter("DoubleConverter")
public class DoubleConverter implements Converter {
	@Override
	public Object getAsObject(FacesContext arg0, UIComponent arg1, String arg2) {
		try {
			return Double.parseDouble(arg2.replace(",", "."));
		} catch (final Exception e) {
			// Is not a number (NaN)
		}
		return Double.NaN;
	}

	@Override
	public String getAsString(FacesContext arg0, UIComponent arg1, Object arg2) {
		final Double d = Double.parseDouble(arg2.toString());
		if (Double.compare(Double.NaN, d) == 0) {
			return "";
		} else if (d == d.intValue()) {
			return Integer.toString(d.intValue());
		}
		return Double.toString(d);
	}
}
