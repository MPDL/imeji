package de.mpg.imeji.logic.search.elasticsearch.script.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.index.get.GetField;

/**
 * Inner class to store collection informations
 * 
 * @author saquet
 *
 */
public class CollectionFields {
  private final List<String> authors;
  private final List<String> organizations;

  public CollectionFields(GetField authorsField, GetField organizationsField) {
    this.authors = authorsField != null
        ? authorsField.getValues().stream().map(Object::toString).collect(Collectors.toList())
        : new ArrayList<>();
    this.organizations = organizationsField != null
        ? organizationsField.getValues().stream().map(Object::toString).collect(Collectors.toList())
        : new ArrayList<>();
  }

  public List<String> getAuthors() {
    return authors;
  }

  public List<String> getOrganizations() {
    return organizations;
  }
}
