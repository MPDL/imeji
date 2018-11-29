package de.mpg.imeji.logic.search.elasticsearch.model;

/**
 * Indexes for elasticSearch
 *
 * @author bastiens
 *
 */
public enum ElasticFields {
  ALL,
  ID,
  IDSTRING,
  NAME,
  DESCRIPTION,
  FULLTEXT,
  LICENSE,
  READ,
  UPLOAD,
  USERS,
  STATUS,
  LASTEDITOR,
  CREATOR,
  CREATORS,
  CREATED,
  MODIFIED,
  FAMILYNAME,
  GIVENNAME,
  ORGANIZATION,
  MEMBER,
  PID,
  FILETYPE,
  FILEEXTENSION,
  SIZE,
  FOLDER,
  PROFILE,
  ALBUM,
  CHECKSUM,
  METADATA,
  METADATA_EXACT("metadata.text.exact"),
  METADATA_TEXT("metadata.text", true),
  METADATA_NAME("metadata.name"),
  METADATA_TITLE("metadata.title"),
  METADATA_NUMBER("metadata.number"),
  METADATA_TIME("metadata.time"),
  METADATA_LOCATION("metadata.location"),
  METADATA_URI("metadata.uri"),
  METADATA_ORGANIZATION("metadata.organization"),
  METADATA_FAMILYNAME("metadata.familyname"),
  METADATA_GIVENNAME("metadata.givenname"),
  METADATA_LONGITUDE("metadata.longitude"),
  METADATA_LATITUDE("metadata.latitude"),
  METADATA_INDEX("metadata.index"),
  METADATA_TYPE("metadata.type"),
  AUTHOR_FAMILYNAME("author.familyname"),
  AUTHOR_GIVENNAME("author.givenname"),
  AUTHOR_COMPLETENAME("author.completename"),
  AUTHOR_ORGANIZATION("author.organization"),
  INFO_LABEL("info.label"),
  INFO_TEXT("info.text"),
  INFO_URL("info.url"),
  EMAIL,
  TECHNICAL,
  TECHNICAL_NAME("technical.name"),
  TECHNICAL_VALUE("technical.value"),
  PARENT("_parent"),
  AUTHORS_OF_COLLECTION("authorsOfCollection"),
  ORGANIZATION_OF_COLLECTION("organizationsOfCollection"),
  TITLE_WITH_ID_OF_COLLECTION("titleWithIdOfCollection");

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
