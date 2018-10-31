package de.mpg.imeji.logic.core.content.extraction;

import de.mpg.imeji.logic.core.content.extraction.extractor.ContentExtractorInterface;
import de.mpg.imeji.logic.core.content.extraction.extractor.TikaContentExtractor;

/**
 * Factory for Content Analysers
 *
 * @author saquet
 *
 */
public class ContentExtractorFactory {

	private ContentExtractorFactory() {
		// avoid construction
	}

	/**
	 * Return the default content analyser
	 *
	 * @return
	 */
	public static ContentExtractorInterface build() {
		return new TikaContentExtractor();
	}

}
