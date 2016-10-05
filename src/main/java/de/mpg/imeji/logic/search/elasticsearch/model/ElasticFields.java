package de.mpg.imeji.logic.search.elasticsearch.model;

/**
 * Indexes for elasticSearch
 *
 * @author bastiens
 *
 */
public enum ElasticFields {
  ALL, ID, NAME, DESCRIPTION, FULLTEXT, LICENSE, SPACE, READ, UPLOAD, USERS, COLLECTIONSINSPACE, STATUS, LASTEDITOR, CREATOR, CREATED, MODIFIED, MEMBER, PID, FILETYPE, SIZE, FOLDER, PROFILE, ALBUM, CHECKSUM, FILENAME, METADATA, METADATA_TEXT(
      "metadata.text", true), METADATA_NUMBER("metadata.number"), METADATA_LOCATION(
          "metadata.location"), METADATA_URI("metadata.uri"), METADATA_FAMILYNAME(
              "metadata.familyname"), METADATA_GIVENNAME("metadata.givenname"), METADATA_LONGITUDE(
                  "metadata.longitude"), METADATA_LATITUDE("metadata.latitude"), METADATA_STATEMENT(
                      "metadata.statement"), METADATA_TYPE("metadata.type"), AUTHOR_FAMILYNAME(
                          "author.familyname"), AUTHOR_GIVENNAME(
                              "author.givenname"), AUTHOR_COMPLETENAME(
                                  "author.completename"), AUTHOR_ORGANIZATION_NAME(
                                      "author.organization.name"), AUTHOR_ORGANIZATION_CITY(
                                          "author.organization.city"), AUTHOR_ORGANIZATION_COUNTRY(
                                              "author.organization.country"), AUTHOR_ORGANIZATION_DESCRIPTION(
                                                  "author.organization.description"), INFO_LABEL(
                                                      "info.label"), INFO_TEXT(
                                                          "info.text"), INFO_URL(
                                                              "info.url"), EMAIL, TECHNICAL, TECHNICAL_NAME(
                                                                  "technical.name"), TECHNICAL_VALUE(
                                                                      "technical.value");
  /**
   * The field which must be used to search in elasticsearch
   */
  private final String field;
  /**
   * If this field has the subfield "exact": important for fields which should be both analyzed and
   * not analyzed
   */
  private final boolean exact;

  /**
   * The index will be the same than the enum value
   */
  private ElasticFields() {
    this.field = name().toLowerCase();
    exact = false;
  }

  /**
   * Give a specific index value
   *
   * @param index
   */
  private ElasticFields(String index) {
    this.field = index;
    exact = false;
  }

  /**
   * Give a specific index value and add an exact field
   *
   * @param index
   */
  private ElasticFields(String index, boolean exact) {
    this.field = index;
    this.exact = exact;
  }

  /**
   * Get the Elastic Saerch index
   *
   * @return
   */
  public String field() {
    return field;
  }

  /**
   * Return the field to search for the exact value
   *
   * @return
   */
  public String fieldExact() {
    return exact ? field + ".exact" : field;
  }

}
