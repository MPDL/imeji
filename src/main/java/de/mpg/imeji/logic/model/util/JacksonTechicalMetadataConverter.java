package de.mpg.imeji.logic.model.util;

import com.fasterxml.jackson.databind.util.StdConverter;

public class JacksonTechicalMetadataConverter extends StdConverter<String, String> {
  @Override
  public String convert(String value) {
    if (value == null) {
      return null;
    }

    return value.replace("\u0000", "");
  }
}
