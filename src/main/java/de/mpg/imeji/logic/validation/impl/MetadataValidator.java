package de.mpg.imeji.logic.validation.impl;

import java.net.URI;
import java.util.Collection;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.util.DateFormatter;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Statement;


/**
 * {@link Validator} for a {@link Metadata}. Only working with profile
 *
 * @author saquet
 *
 */
public class MetadataValidator extends ObjectValidator implements Validator<Metadata> {


  @Override
  public void validate(Metadata md, Method m) throws UnprocessableError {
    validate(md, null, m);
  }

  public void validate(Metadata md, Statement statement, Method m) throws UnprocessableError {
    setValidateForMethod(m);
    if (isDelete()) {
      return;
    }
    validataMetadata(md, statement);
  }


  /**
   * Validate the {@link Metadata} for the differents types
   *
   * @param md
   * @param s
   * @return
   * @throws UnprocessableError
   */
  private void validataMetadata(Metadata md, Statement s) throws UnprocessableError {
    UnprocessableError e = new UnprocessableError();
    switch (s.getType()) {
      case TEXT:
        if (!isAllowedValueString(md.getText(), s)) {
          e = new UnprocessableError("error_metadata_invalid_value" + md.getText(), e);
        }
        break;
      case NUMBER:
        if (!isAllowedValueDouble(md.getNumber(), s)) {
          e = new UnprocessableError("error_metadata_invalid_value" + md.getNumber(), e);
        }
        break;
      case DATE:
        if (!isValidDate(md.getDate())) {
          e = new UnprocessableError("error_date_format" + md.getText(), e);
        }
        break;
      case URL:
        if (md.getUrl() == null) {
          e = new UnprocessableError("error_metadata_url_empty", e);
        }
        if (!isAllowedValueString(md.getUrl(), s)) {
          e = new UnprocessableError("error_metadata_invalid_value" + md.getUrl(), e);
        }
        break;
      case GEOLOCATION:
        try {
          new GeolocationValidator().validate(md, validateForMethod);
        } catch (final UnprocessableError e2) {
          e = new UnprocessableError(e2.getMessages(), e);
        }
        break;
      case PERSON:
        try {
          new PersonValidator().validate(md.getPerson(), validateForMethod);
        } catch (final UnprocessableError e1) {
          e = new UnprocessableError(e1.getMessages(), e);
        }
        break;
    }

    if (e.hasMessages()) {
      throw e;
    }
  }


  /**
   * True if the String is valid Date
   *
   * @param dateString
   * @return
   */
  private boolean isValidDate(String dateString) {
    return DateFormatter.parseDate(dateString) != null;
  }


  /**
   * Check if the value is allowed according the literal constraints
   *
   * @param value
   * @param s
   * @return
   */
  private boolean isAllowedValueString(String value, Statement s) {
    if (value == null) {
      return false;
    }
    if (s.getLiteralConstraints() != null && s.getLiteralConstraints().size() > 0) {
      return containsString(s.getLiteralConstraints(), value);
    }
    return true;
  }

  /**
   * Check if the value is allowed according the literal constraints
   *
   * @param value
   * @param s
   * @return
   */
  private boolean isAllowedValueDouble(double value, Statement s) {
    if (s.getLiteralConstraints() != null && s.getLiteralConstraints().size() > 0) {
      return containsDouble(s.getLiteralConstraints(), value);
    }
    return true;
  }

  /**
   * Check if the value is allowed according the literal constraints
   *
   * @param value
   * @param s
   * @return
   */
  private boolean isAllowedValueURI(URI value, Statement s) {
    if (s.getLiteralConstraints() != null && s.getLiteralConstraints().size() > 0) {
      return containsURI(s.getLiteralConstraints(), value);
    }
    return true;
  }

  /**
   * Test if the {@link Collection} contains the {@link String}
   *
   * @param l
   * @param value
   * @return
   */
  private boolean containsString(Collection<String> l, String value) {
    for (final String s : l) {
      if (s.equals(value)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Test if the {@link Collection} contains the {@link Double}
   *
   * @param l
   * @param value
   * @return
   */
  private boolean containsDouble(Collection<String> l, double value) {
    for (final String s : l) {
      if (Double.parseDouble(s) == value) {
        return true;
      }
    }
    return false;
  }

  /**
   * Test if the {@link Collection} contains the {@link URI}
   *
   * @param l
   * @param value
   * @return
   */
  private boolean containsURI(Collection<String> l, URI value) {
    for (final String s : l) {
      if (URI.create(s).equals(value)) {
        return true;
      }
    }
    return false;
  }


}
