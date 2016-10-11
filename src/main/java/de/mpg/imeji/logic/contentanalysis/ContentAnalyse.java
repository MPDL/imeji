package de.mpg.imeji.logic.contentanalysis;

import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.vo.TechnicalMetadata;

/**
 * The result of a content analyser
 * 
 * @author saquet
 *
 */
public class ContentAnalyse {
  private String fulltext;
  private List<TechnicalMetadata> technicalMetadata = new ArrayList<>();


  /**
   * @return the fulltext
   */
  public String getFulltext() {
    return fulltext;
  }

  /**
   * @param fulltext the fulltext to set
   */
  public void setFulltext(String fulltext) {
    this.fulltext = fulltext;
  }

  /**
   * @return the technicalMetadata
   */
  public List<TechnicalMetadata> getTechnicalMetadata() {
    return technicalMetadata;
  }

  /**
   * @param technicalMetadata the technicalMetadata to set
   */
  public void setTechnicalMetadata(List<TechnicalMetadata> technicalMetadata) {
    this.technicalMetadata = technicalMetadata;
  }



}
