package de.mpg.imeji.logic.validation.impl;

import java.net.URI;
import java.util.Collection;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.util.DateFormatter;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.predefinedMetadata.ConePerson;
import de.mpg.imeji.logic.vo.predefinedMetadata.Date;
import de.mpg.imeji.logic.vo.predefinedMetadata.Geolocation;
import de.mpg.imeji.logic.vo.predefinedMetadata.License;
import de.mpg.imeji.logic.vo.predefinedMetadata.Link;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;
import de.mpg.imeji.logic.vo.predefinedMetadata.Number;
import de.mpg.imeji.logic.vo.predefinedMetadata.Publication;
import de.mpg.imeji.logic.vo.predefinedMetadata.Text;
import de.mpg.imeji.logic.vo.util.MetadataAndProfileHelper;

/**
 * {@link Validator} for a {@link Metadata}. Only working with profile
 *
 * @author saquet
 *
 */
public class MetadataValidator extends ObjectValidator implements Validator<Metadata> {

  @Override
  public void validate(Metadata t, Method m) throws UnprocessableError {
    throw new UnprocessableError("Metadata can not be validated without a profile");
  }

  @Override
  public void validate(Metadata md, MetadataProfile p, Method m) throws UnprocessableError {
    setValidateForMethod(m);
    if (isDelete()) {
      return;
    }
    Statement s = MetadataAndProfileHelper.getStatement(md.getStatement(), p);
    validataMetadata(md, s);
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
    if (md instanceof Text) {
      String value = ((Text) md).getText();
      if (!isAllowedValueString(value, s)) {
        e = new UnprocessableError("error_metadata_invalid_value" + value, e);
      }
    } else if (md instanceof Number) {
      double value = ((Number) md).getNumber();
      if (!isAllowedValueDouble(value, s)) {
        e = new UnprocessableError("error_metadata_invalid_value" + value, e);
      }
    } else if (md instanceof Date) {
      String value = ((Date) md).getDate();
      if (!isValidDate(value)) {
        e = new UnprocessableError("error_date_format" + value, e);
      }
    } else if (md instanceof Link) {
      URI value = ((Link) md).getUri();
      if (value == null) {
        e = new UnprocessableError("error_metadata_url_empty", e);
      }
      if (!isAllowedValueURI(value, s)) {
        e = new UnprocessableError("error_metadata_invalid_value" + value, e);
      }
    } else if (md instanceof Geolocation) {
      try {
        new GeolocationValidator().validate((Geolocation) md, validateForMethod);
      } catch (UnprocessableError e2) {
        e = new UnprocessableError(e2.getMessages(), e);
      }
    } else if (md instanceof ConePerson) {
      // no validation here for person only will be invoked, if family name is not
      // provided, presentation is deleting the whole person object!!!
      // should be fixed in the presentation
      try {
        new PersonValidator().validate(((ConePerson) md).getPerson(), validateForMethod);
      } catch (UnprocessableError e1) {
        e = new UnprocessableError(e1.getMessages(), e);
      }
    } else if (md instanceof License) {
      String value = ((License) md).getLicense();
      if (value == null) {
        e = new UnprocessableError("error_metadata_invalid_value" + value, e);
      }
    } else if (md instanceof Publication) {
      URI value = ((Publication) md).getUri();
      if (value == null) {
        e = new UnprocessableError("error_metadata_invalid_value" + value, e);
      }
    }
    if (e.hasMessages()) {
      throw e;
    }
  }

  private String getTypeLabel(Metadata md) {
    if (md instanceof Text) {
      return "Text";
    } else if (md instanceof Number) {
      return "Number";
    } else if (md instanceof Date) {
      return "Date";
    } else if (md instanceof Link) {
      return "Link";
    } else if (md instanceof Geolocation) {
      return "Location";
    } else if (md instanceof ConePerson) {
      return "Person/Organization";
    } else if (md instanceof License) {
      return "License";
    } else if (md instanceof Publication) {
      return "Publication";
    }
    return "";
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
    for (String s : l) {
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
    for (String s : l) {
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
    for (String s : l) {
      if (URI.create(s).equals(value)) {
        return true;
      }
    }
    return false;
  }


}
