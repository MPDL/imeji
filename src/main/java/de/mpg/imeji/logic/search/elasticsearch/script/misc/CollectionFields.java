package de.mpg.imeji.logic.search.elasticsearch.script.misc;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.get.GetField;

import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;

/**
 * Inner class to store collection informations
 * 
 * @author saquet
 *
 */
public class CollectionFields {
  private final List<String> authors;
  private final List<String> organizations;
  private final String titleWithId;

  public CollectionFields(CollectionImeji c) {
    this.authors =
        c.getPersons().stream().map(p -> p.getCompleteName()).collect(Collectors.toList());
    this.organizations = c.getPersons().stream().flatMap(p -> p.getOrganizations().stream())
        .map(o -> o.getName()).collect(Collectors.toList());
    this.titleWithId = ObjectHelper.getId(URI.create(c.getId().toString())) + " " + c.getTitle();
  }

  public CollectionFields(GetField authorsField, GetField organizationsField, GetField id,
      GetField title) {
    this.authors = authorsField != null
        ? authorsField.getValues().stream().map(Object::toString).collect(Collectors.toList())
        : new ArrayList<>();
    this.organizations = organizationsField != null
        ? organizationsField.getValues().stream().map(Object::toString).collect(Collectors.toList())
        : new ArrayList<>();
    this.titleWithId =
        ObjectHelper.getId(URI.create(id.getValue().toString())) + " " + title.getValue();
  }

  public XContentBuilder toXContentBuilder() throws IOException {
    return XContentFactory.jsonBuilder().startObject()
        .field(ElasticFields.AUTHORS_OF_COLLECTION.field(), authors)
        .field(ElasticFields.ORGANIZATION_OF_COLLECTION.field(), organizations)
        .field(ElasticFields.TITLE_WITH_ID_OF_COLLECTION.field(), titleWithId).endObject();
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
