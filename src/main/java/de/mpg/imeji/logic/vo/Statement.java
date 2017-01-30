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
  private String id;
  private StatementType type = StatementType.TEXT;
  private URI uri;
  @j2jLiteral("http://imeji.org/terms/index")
  private String index;
  @j2jLiteral("http://purl.org/dc/terms/type")
  private String typeString = type.name();
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
   * @param index the index to set
   */
  public void setIndex(String index) {
    this.index = index;
    setId(index);
  }

  public URI getVocabulary() {
    return vocabulary;
  }

  public void setVocabulary(URI vocabulary) {
    this.vocabulary = vocabulary;
  }

  public Collection<String> getLiteralConstraints() {
    final List<String> constraints = new ArrayList<String>(literalConstraints);
    Collections.sort(constraints, new SortIgnoreCase());
    literalConstraints = constraints;
    return literalConstraints;
  }

  public void setLiteralConstraints(Collection<String> literalConstraints) {
    this.literalConstraints = literalConstraints;
  }

  public void setUri(URI uri) {
    this.uri = uri;
    this.id = ObjectHelper.getId(uri);
  }

  public URI getUri() {
    return uri;
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
    this.uri = ObjectHelper.getURI(Statement.class, id);
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
    return clone;
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
