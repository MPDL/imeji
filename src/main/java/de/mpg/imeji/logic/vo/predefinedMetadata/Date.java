/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.vo.predefinedMetadata;

import java.net.URI;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.mpg.imeji.j2j.annotations.j2jDataType;
import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.ImejiNamespaces;
import de.mpg.imeji.logic.util.DateFormatter;

/**
 * The Date {@link Metadata}. Should be used for {@link Metadata} related to a date
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource(ImejiNamespaces.METADATA)
@j2jDataType("http://imeji.org/terms/metadata#date")
@j2jId(getMethod = "getId", setMethod = "setId")
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "date", namespace = ImejiNamespaces.METADATA)
@XmlType(propOrder = {"date", "time", "statement"})
public class Date extends Metadata {
  private static final long serialVersionUID = 8791083521970179964L;
  @j2jLiteral("http://imeji.org/terms/date")
  private String date;
  @j2jLiteral("http://imeji.org/terms/time")
  private long time;

  @j2jResource("http://imeji.org/terms/statement")
  private URI statement;

  public Date() {
    // constructor
  }

  @XmlElement(name = "date", namespace = "http://imeji.org/terms/")
  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    if (date != null && !"".equals(date)) {
      time = DateFormatter.getTime(date);
      this.date = date;
    }
  }

  @XmlElement(name = "time", namespace = "http://imeji.org/terms/")
  public long getTime() {
    return time;
  }

  public void setTime(long dateTime) {
    this.time = dateTime;
  }

  @Override
  @XmlElement(name = "statement", namespace = "http://imeji.org/terms/")
  public URI getStatement() {
    return statement;
  }

  @Override
  public void setStatement(URI namespace) {
    this.statement = namespace;
  }

  @Override
  public void copy(Metadata metadata) {
    if (metadata instanceof Date) {
      setPos(metadata.getPos());
      setDate(((Date) metadata).getDate());
      setStatement(((Date) metadata).getStatement());
    }
  }

  @Override
  public String asFulltext() {
    return date;
  }

  @Override
  public void clean() {
    // nothing to do
  }
}
