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
  FULLTEXT("content.fulltext"),
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
  CHECKSUM("content.checksum"),
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
  AUTHOR_COMPLETENAME_EXACT("author.completename.exact"),
  AUTHOR_ORGANIZATION("author.organization"),
  AUTHOR_ORGANIZATION_EXACT("author.organization.exact"),
  INFO_LABEL("info.label"),
  INFO_TEXT("info.text"),
  INFO_LABEL_EXACT("info.label.exact"),
  INFO_TEXT_EXACT("info.text.exact"),
  INFO_TEXT_SPLITTED("info.splitted"),
  INFO_URL("info.url"),
  INFO("info"),
  COLLECTION_TYPE("types"),
  EMAIL,
  TECHNICAL("content.technical"),
  TECHNICAL_NAME("content.technical.name"),
  TECHNICAL_VALUE("content.technical.value"),
  // PARENT("_parent"),
  AUTHORS_OF_COLLECTION("authorsOfCollection"),
  ORGANIZATION_OF_COLLECTION("organizationsOfCollection"),
  TITLE_WITH_ID_OF_COLLECTION("titleWithIdOfCollection"),
  JOIN_FIELD("joinField");
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
