package de.mpg.imeji.logic.model;

import java.util.stream.Stream;



/**
 * The fields that can be used for metadata (md.index.field)
 * 
 * @author saquet
 *
 */
public enum SearchMetadataFields {
  exact, text, number, date, time, placename, coordinates, url, title, familyname, completename, givenname, identifier, organisation;

  private final String index;

  private SearchMetadataFields() {
    // index is same as name
    index = this.name().toLowerCase();
  }

  private SearchMetadataFields(String index) {
    this.index = index;
  }

  public String getIndex() {
    return index;
  }

  public static SearchMetadataFields valueOfIndex(String index) {
    return Stream.of(SearchMetadataFields.values())
        .filter(f -> f.getIndex().equals(index.toLowerCase())).findFirst().get();
  }
}
