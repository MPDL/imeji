package de.mpg.imeji.logic.search.elasticsearch.model;

import de.mpg.imeji.logic.vo.predefinedMetadata.ConePerson;
import de.mpg.imeji.logic.vo.predefinedMetadata.Date;
import de.mpg.imeji.logic.vo.predefinedMetadata.Geolocation;
import de.mpg.imeji.logic.vo.predefinedMetadata.License;
import de.mpg.imeji.logic.vo.predefinedMetadata.Link;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;
import de.mpg.imeji.logic.vo.predefinedMetadata.Publication;
import de.mpg.imeji.logic.vo.predefinedMetadata.Text;
import de.mpg.imeji.logic.vo.util.ObjectHelper;

/**
 * The indexed {@link Metadata}<br/>
 * !!! IMPORTANT !!!<br/>
 * This File must be synchronized with resources/elasticsearch/ElasticItemsMapping.json
 *
 * @author bastiens
 *
 */
public final class ElasticMetadata extends ElasticPerson {
  private final String statement;
  private final String text;
  private final double number;
  private final String uri;
  private final String type;
  private final String location;

  public ElasticMetadata(ConePerson p) {
    super(p.getPerson());
    this.statement = ObjectHelper.getId(p.getStatement());
    this.type = p.getTypeNamespace();
    this.text = null;
    this.number = Double.NaN;
    this.uri = null;
    this.location = null;
  }

  /**
   * Constructor with a {@link Metadata}
   *
   * @param md
   */
  public ElasticMetadata(Metadata md) {
    super();
    this.statement = ObjectHelper.getId(md.getStatement());
    this.type = md.getTypeNamespace();
    if (md instanceof Text) {
      this.text = ((Text) md).getText();
      this.number = Double.NaN;
      this.uri = null;
      this.location = null;
    } else if (md instanceof de.mpg.imeji.logic.vo.predefinedMetadata.Number) {
      this.number = ((de.mpg.imeji.logic.vo.predefinedMetadata.Number) md).getNumber();
      this.text = null;
      this.uri = null;
      this.location = null;
    } else if (md instanceof Date) {
      this.text = ((Date) md).getDate();
      this.number = ((Date) md).getTime();
      this.uri = null;
      this.location = null;
    } else if (md instanceof Link) {
      this.text = ((Link) md).getLabel();
      this.number = Double.NaN;
      this.uri = ((Link) md).getUri() != null ? ((Link) md).getUri().toString() : null;
      this.location = null;
    } else if (md instanceof ConePerson) {
      this.text = null;
      this.number = Double.NaN;
      this.uri = null;
      this.location = null;
    } else if (md instanceof Geolocation) {
      this.text = ((Geolocation) md).getName();
      this.number = Double.NaN;
      this.uri = null;
      this.location = ((Geolocation) md).getLatitude() + "," + ((Geolocation) md).getLongitude();
    } else if (md instanceof License) {
      this.text = ((License) md).getLicense();
      this.number = Double.NaN;
      this.uri = ((License) md).getExternalUri() == null ? null
          : ((License) md).getExternalUri().toString();
      this.location = null;
    } else if (md instanceof Publication) {
      this.text = ((Publication) md).getCitation();
      this.number = Double.NaN;
      this.uri =
          ((Publication) md).getUri() == null ? null : ((Publication) md).getUri().toString();
      this.location = null;
    } else {
      text = null;
      this.number = Double.NaN;
      this.uri = null;
      this.location = null;
    }
  }

  /**
   * @return the statement
   */
  public String getStatement() {
    return statement;
  }


  /**
   * @return the text
   */
  public String getText() {
    return text;
  }

  /**
   * @return the number
   */
  public double getNumber() {
    return number;
  }



  /**
   * @return the uri
   */
  public String getUri() {
    return uri;
  }


  /**
   * @return the type
   */
  public String getType() {
    return type;
  }


  /**
   * @return the location
   */
  public String getLocation() {
    return location;
  }


}
