package de.mpg.imeji.logic.search.elasticsearch.script.misc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.util.ObjectHelper;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class to store collection informations with items in ElasticSearch
 * 
 * @author saquet
 *
 */
public class CollectionFields {

  private static final String TITLE_ID_SEPARATOR = " ";
  private final List<String> authors;
  private final List<String> organizations;
  private final String titleWithId;

  public CollectionFields(CollectionImeji c) {
    this.authors = c.getPersons().stream().map(p -> p.getCompleteName()).collect(Collectors.toList());
    this.organizations =
        c.getPersons().stream().flatMap(p -> p.getOrganizations().stream()).map(o -> o.getName()).collect(Collectors.toList());
    this.titleWithId = this.titleWithIdOfCollection(c.getTitle(), ObjectHelper.getId(URI.create(c.getId().toString())));
  }

  /*
  public CollectionFields(DocumentField authorsField, DocumentField organizationsField, DocumentField id, DocumentField title) {
    this.authors =
        authorsField != null ? authorsField.getValues().stream().map(Object::toString).collect(Collectors.toList()) : new ArrayList<>();
    this.organizations =
        organizationsField != null ? organizationsField.getValues().stream().map(Object::toString).collect(Collectors.toList())
            : new ArrayList<>();
    this.titleWithId = this.titleWithIdOfCollection((String) title.getValue(), ObjectHelper.getId(URI.create(id.getValue().toString())));
  }
   */

  public CollectionFields(JsonNode sourceNode) throws IOException {
    //ObjectMapper mapper = new ObjectMapper();
    //JsonNode sourceNode = mapper.readTree(sourceAsBytes);
    ArrayList<String> authorNames = new ArrayList<>();
    ArrayList<String> authorOrganizations = new ArrayList<>();
    JsonNode authorNode = sourceNode.get("author");

    if (authorNode.isArray()) {
      for (JsonNode node : authorNode) {
        authorNames.add(node.get("completename").asText());
        JsonNode orgsNode = node.get("organization");
        if (orgsNode.isArray()) {
          for (JsonNode on : orgsNode) {
            authorOrganizations.add(on.asText());
          }
        }
      }
    }
    this.authors = authorNames;
    this.organizations = authorOrganizations;
    String collectionId = sourceNode.get(ElasticFields.ID.field()).textValue();
    this.titleWithId =
        this.titleWithIdOfCollection(sourceNode.get(ElasticFields.NAME.field()).asText(), ObjectHelper.getId(URI.create(collectionId)));

  }

  /**
   * Constructs a compound String that contains both the title and the ID of a collection
   * 
   * @param collectionTitle
   * @param id
   * @return
   */
  private String titleWithIdOfCollection(String collectionTitle, String id) {
    return collectionTitle + TITLE_ID_SEPARATOR + id;
  }

  /**
   * Deconstructs a compound String that contains both the title and the ID of a collection Returns
   * the title
   * 
   * @return
   */
  public static String getTitle(String titleWithIdOfCollection) {

    // ID is at the end of the String
    int subStringTo = titleWithIdOfCollection.lastIndexOf(TITLE_ID_SEPARATOR);
    String label = titleWithIdOfCollection.substring(0, subStringTo);
    return label;
  }

  /**
   * Deconstructs a compound String that contains both the title and the ID of a collection Return
   * the string
   * 
   * @return
   */
  public static String getID(String titleWithIdOfCollection) {

    // ID is at the end of the String
    int subStringFrom = titleWithIdOfCollection.lastIndexOf(TITLE_ID_SEPARATOR);
    String id = titleWithIdOfCollection.substring(subStringFrom + 1);
    return id;
  }


  public ObjectNode toJsonNode() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode rootNode = mapper.createObjectNode();
    rootNode.putArray(ElasticFields.AUTHORS_OF_COLLECTION.field())
        .addAll(authors.stream().map(auth -> new TextNode(auth)).collect(Collectors.toList()));
    rootNode.putArray(ElasticFields.ORGANIZATION_OF_COLLECTION.field())
        .addAll(organizations.stream().map(auth -> new TextNode(auth)).collect(Collectors.toList()));
    rootNode.put(ElasticFields.TITLE_WITH_ID_OF_COLLECTION.field(), titleWithId);
    return rootNode;



    /*
    return XContentFactory.jsonBuilder().startObject().field(ElasticFields.AUTHORS_OF_COLLECTION.field(), authors)
        .field(ElasticFields.ORGANIZATION_OF_COLLECTION.field(), organizations)
        .field(ElasticFields.TITLE_WITH_ID_OF_COLLECTION.field(), titleWithId).endObject();
        */

  }


  public List<String> getAuthors() {
    return authors;
  }

  public List<String> getOrganizations() {
    return organizations;
  }

  public String getTitleWithId() {
    return titleWithId;
  }
}
