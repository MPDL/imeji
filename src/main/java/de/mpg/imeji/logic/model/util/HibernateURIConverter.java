package de.mpg.imeji.logic.model.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.net.URI;

@Converter(autoApply = true)
public class HibernateURIConverter implements
        AttributeConverter<URI, String> {

    private static final String SEPARATOR = ", ";

    @Override
    public String convertToDatabaseColumn(URI uri) {
        if (uri == null) {
            return null;
        }

        return uri.toString();
    }

    @Override
    public URI convertToEntityAttribute(String uriString) {
        if (uriString == null || uriString.isEmpty()) {
            return null;
        }

       return URI.create(uriString);
    }
}