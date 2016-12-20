package de.mpg.imeji.logic.search.elasticsearch.model;

import de.mpg.imeji.logic.vo.Metadata;


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


  /**
   * Constructor with a {@link Metadata}
   *
   * @param md
   */
  public ElasticMetadata(Metadata md) {
    super(md.getPerson());
    this.statement = md.getStatementId();
    this.type = null;// TODO
    this.text = md.getText();
    this.number = md.getNumber();
    this.uri = md.getUrl();
    this.location = md.getLatitude() + "," + md.getLongitude();
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
