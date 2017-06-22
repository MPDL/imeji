package de.mpg.imeji.logic.search.elasticsearch.model;

import de.mpg.imeji.logic.util.DateFormatter;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.factory.StatementFactory;


/**
 * The indexed {@link Metadata}<br/>
 * !!! IMPORTANT !!!<br/>
 * This File must be synchronized with resources/elasticsearch/ElasticItemsMapping.json
 *
 * @author bastiens
 *
 */
public final class ElasticMetadata extends ElasticPerson {
  private final String index;
  private final String text;
  private final String name;
  private final String title;
  private double number;
  private double time;
  private String date;
  private final String uri;
  private String location;


  /**
   * Constructor with a {@link Metadata}
   *
   * @param md
   */
  public ElasticMetadata(Metadata md) {
    super(md.getPerson());
    this.index = new StatementFactory().setIndex(md.getIndex()).build().getIndexUrlEncoded();
    this.text = md.getText();
    this.name = md.getName();
    this.title = md.getTitle();
    if (!Double.isNaN(md.getNumber())) {
      this.number = md.getNumber();
    }
    this.uri = md.getUrl();
    if (!StringHelper.isNullOrEmptyTrim(md.getDate())) {
      this.time = DateFormatter.getTime(md.getDate());
      this.date = md.getDate();
    }
    if (!Double.isNaN(md.getLatitude()) && !Double.isNaN(md.getLongitude())) {
      this.location = md.getLatitude() + "," + md.getLongitude();
    }
  }

  /**
   * @return the statement
   */
  public String getIndex() {
    return index;
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
   * @return the location
   */
  public String getLocation() {
    return location;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @return the time
   */
  public double getTime() {
    return time;
  }

  public String getDate() {
    return date;
  }

}
