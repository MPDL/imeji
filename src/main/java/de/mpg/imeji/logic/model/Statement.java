package de.mpg.imeji.logic.model;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jList;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.model.util.StatementUtil;
import de.mpg.imeji.logic.util.ObjectHelper;

/**
 * Define the properties of a {@link Metadata}. {@link Statement} are defined in a
 * {@link MetadataProfile}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource("http://imeji.org/terms/statement")
@j2jModel("statement")
@j2jId(getMethod = "getUri", setMethod = "setUri")
public class Statement implements Serializable, Cloneable {
  private static final long serialVersionUID = -7950561563075491540L;
  private StatementType type = StatementType.TEXT;
  private URI uri;
  @j2jLiteral("http://imeji.org/terms/index")
  private String index;
  @j2jLiteral("http://purl.org/dc/terms/type")
  private String typeString = type.name();
  @j2jLiteral("http://imeji.org/terms/namespace")
  private String namespace;
  @j2jResource("http://purl.org/dc/dcam/VocabularyEncodingScheme")
  private URI vocabulary;
  @j2jList("http://imeji.org/terms/literalConstraint")
  private Collection<String> literalConstraints = new ArrayList<String>();

  public Statement() {

  }

  public StatementType getType() {
    type = StatementType.valueOf(typeString);
    return type;
  }

  public void setType(StatementType type) {
    this.type = type;
    this.typeString = type.name();
  }

  /**
   * @return the index
   */
  public String getIndex() {
    return index;
  }

  /**
   * Format the index in a case insensitive UrlFriendly manner (but not url encoded!)
   * 
   * @param index
   * @return
   * @throws UnsupportedEncodingException
   */
  private String encodeIndex(String index) {
    return StatementUtil.formatIndex(index);
  }

  /**
   * Get the index encoded in UTF-8
   * 
   * @return
   */
  public String getIndexUrlEncoded() {
    return StatementUtil.getIndexUrlEncoded(index);
  }

  /**
   * Return the index which can be used for the search
   * 
   * @return
   */
  public String getSearchIndex() {
    return SearchFields.md.getIndex() + "." + getIndexUrlEncoded() + "." + getMetadataField();
  }

  private String getMetadataField() {
    switch (getType()) {
      case DATE:
        return SearchMetadataFields.date.getIndex();
      case NUMBER:
        return SearchMetadataFields.number.getIndex();
      default:
        return SearchMetadataFields.exact.getIndex();
    }
  }

  /**
   * @param index the index to set
   */
  public void setIndex(String index) {
    this.index = index.trim();
    this.uri = ObjectHelper.getURI(Statement.class, encodeIndex(this.index));
  }

  public URI getVocabulary() {
    return vocabulary;
  }

  public void setVocabulary(URI vocabulary) {
    this.vocabulary = vocabulary;
  }

  public Collection<String> getLiteralConstraints() {
    final List<String> constraints = new ArrayList<String>(literalConstraints);
    Collections.sort(constraints, (a, b) -> a.compareToIgnoreCase(b));
    literalConstraints = constraints;
    return literalConstraints;
  }

  public void setLiteralConstraints(Collection<String> literalConstraints) {
    this.literalConstraints = literalConstraints;
  }

  public void setUri(URI uri) {
    this.uri = uri;
  }

  public URI getUri() {
    return uri;
  }

  /**
   * @return the namespace
   */
  public String getNamespace() {
    return namespace;
  }

  /**
   * @param namespace the namespace to set
   */
  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#clone()
   */
  @Override
  public Statement clone() {
    final Statement clone = new Statement();
    clone.literalConstraints = literalConstraints;
    clone.index = index;
    clone.type = type;
    clone.vocabulary = vocabulary;
    clone.uri = uri;
    clone.typeString = typeString;
    clone.namespace = namespace;
    return clone;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Statement)) {
      return false;
    }
    Statement s = (Statement) o;
    if (!(s.getLiteralConstraints() == null ? literalConstraints == null
        : s.getLiteralConstraints().equals(literalConstraints))) {
      return false;
    }
    if (!(s.getIndex() == null ? index == null : s.getIndex().equals(index))) {
      return false;
    }
    if (!(s.getType() == null ? type == null : s.getType().equals(type))) {
      return false;
    }
    if (!(s.getVocabulary() == null ? vocabulary == null : s.getVocabulary().equals(vocabulary))) {
      return false;
    }
    if (!(s.getUri() == null ? uri == null : s.getUri().equals(uri))) {
      return false;
    }
    if (!(s.getNamespace() == null ? namespace == null : s.getNamespace().equals(namespace))) {
      return false;
    }

    return true;
  }

}
