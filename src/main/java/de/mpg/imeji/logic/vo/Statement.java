/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.vo;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jList;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.util.IdentifierUtil;
import de.mpg.imeji.util.LocalizedString;

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
public class Statement implements Comparable<Statement>, Serializable, Cloneable {
  private static final long serialVersionUID = -7950561563075491540L;
  private String id;
  // Id: creation to be changed with pretty ids
  private URI uri = IdentifierUtil.newURI(Statement.class);
  @j2jLiteral("http://purl.org/dc/terms/type")
  private StatementType type = StatementType.TEXT;
  @j2jList("http://www.w3.org/2000/01/rdf-schema#label")
  private Collection<LocalizedString> labels = new ArrayList<LocalizedString>();
  @j2jResource("http://purl.org/dc/dcam/VocabularyEncodingScheme")
  private URI vocabulary;
  @j2jList("http://imeji.org/terms/literalConstraint")
  private Collection<String> literalConstraints = new ArrayList<String>();
  @j2jLiteral("http://imeji.org/terms/position")
  private int pos = 0;
  @j2jResource("http://imeji.org/terms/namespace")
  private URI namespace;

  public Statement() {}

  public StatementType getType() {
    return type;
  }

  public void setType(StatementType type) {
    this.type = type;
  }

  public Collection<LocalizedString> getLabels() {
    return labels;
  }

  public void setLabels(Collection<LocalizedString> labels) {
    this.labels = labels;
  }

  /**
   * Return the default label (english if exists, otherwise the 1st one)
   *
   * @return
   */
  public String getLabel() {
    for (LocalizedString l : labels) {
      if (l.getLang().equals("en")) {
        return l.getValue();
      }
    }
    return labels.iterator().next().getValue();
  }

  public URI getVocabulary() {
    return vocabulary;
  }

  public void setVocabulary(URI vocabulary) {
    this.vocabulary = vocabulary;
  }

  public Collection<String> getLiteralConstraints() {
    List<String> constraints = new ArrayList<String>(literalConstraints);
    Collections.sort(constraints, new SortIgnoreCase());
    literalConstraints = constraints;
    return literalConstraints;
  }

  public void setLiteralConstraints(Collection<String> literalConstraints) {
    this.literalConstraints = literalConstraints;
  }


  public int getPos() {
    return pos;
  }

  public void setPos(int pos) {
    this.pos = pos;
  }

  @Override
  public int compareTo(Statement o) {
    if (o.getPos() > this.pos) {
      return -1;
    } else if (o.getPos() == this.pos) {
      return 0;
    } else {
      return 1;
    }
  }

  public void setUri(URI uri) {
    this.uri = uri;
  }

  public URI getUri() {
    return uri;
  }

  /**
   * @param namespace the namespace to set
   */
  public void setNamespace(URI namespace) {
    this.namespace = namespace;
  }

  /**
   * @return the namespace
   */
  public URI getNamespace() {
    return namespace;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#clone()
   */
  @Override
  public Statement clone() {
    Statement clone = new Statement();
    clone.labels = new ArrayList<>(labels);
    clone.literalConstraints = literalConstraints;
    clone.pos = pos;
    clone.type = type;
    clone.vocabulary = vocabulary;
    return clone;
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Comparator to sort String ignoring the case
   *
   * @author saquet (initial creation)
   * @author $Author$ (last modification)
   * @version $Revision$ $LastChangedDate$
   */
  public class SortIgnoreCase implements Comparator<Object> {
    /*
     * (non-Javadoc)
     *
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(Object o1, Object o2) {
      if ("".equals(o1)) {
        return 1;
      }
      return ((String) o1).compareToIgnoreCase((String) o2);
    }
  }
}
