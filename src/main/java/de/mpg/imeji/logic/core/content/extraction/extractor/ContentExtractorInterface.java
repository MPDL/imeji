package de.mpg.imeji.logic.core.content.extraction.extractor;

import java.io.File;
import java.util.List;

import de.mpg.imeji.logic.core.content.extraction.ContentExtractionResult;
import de.mpg.imeji.logic.model.TechnicalMetadata;

/**
 * Interface for imeji content Analyser
 *
 * @author saquet
 *
 */
public interface ContentExtractorInterface {

  /**
   * Extract the fulltext of a file (if availabe)
   *
   * @param file
   * @return
   */
  public String extractFulltext(File file);

  /**
   * Extract the technical metadata of a a file
   *
   * @param file
   * @return
   */
  public List<TechnicalMetadata> extractTechnicalMetadata(File file);

  /**
   * Extract everything what can be extracted from file. Faster than extracting fulltext and
   * metadata separately
   *
   * @param file
   * @return
   */
  public ContentExtractionResult extractAll(File file);

}
