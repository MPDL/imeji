package de.mpg.imeji.logic.model.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.net.URI;

/**
 * Removes Unicode \u0000 from technical metadata, as it is not allowed in postgres
 */
@Converter()
public class HibernateTechnicalMetadataConverter implements
        AttributeConverter<String, String> {

    private static final String SEPARATOR = ", ";

    @Override
    public String convertToDatabaseColumn(String value) {
        if (value == null) {
            return null;
        }

        return value.replace("\u0000", "");
    }

    @Override
    public String convertToEntityAttribute(String uriString) {
       return uriString;
    }
}