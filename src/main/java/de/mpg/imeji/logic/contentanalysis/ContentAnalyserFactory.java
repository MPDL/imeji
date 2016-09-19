package de.mpg.imeji.logic.contentanalysis;

import de.mpg.imeji.logic.contentanalysis.impl.TikaContentAnalyser;

/**
 * Factory for Content Analysers
 * 
 * @author saquet
 *
 */
public class ContentAnalyserFactory {

  private ContentAnalyserFactory() {
    // avoid construction
  }

  /**
   * Return the default content analyser
   * 
   * @return
   */
  public static ContentAnalyser build() {
    return new TikaContentAnalyser();
  }

}
