package de.mpg.imeji.logic.search.elasticsearch.model;

import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.TechnicalMetadata;

/**
 * Elastic Object for contentVO
 *
 * @author saquet
 *
 */
public class ElasticContent {
	private final String checksum;
	private final long width;
	private final long height;
	private final List<ElasticTechnicalMetadata> technical = new ArrayList<>();
	private final String fulltext;

	/**
	 * Constructor
	 *
	 * @param contentVO
	 */
	public ElasticContent(ContentVO contentVO) {
		this.height = contentVO.getHeight();
		this.width = contentVO.getWidth();
		this.fulltext = contentVO.getFulltext();
		this.checksum = contentVO.getChecksum();
		for (final TechnicalMetadata md : contentVO.getTechnicalMetadata()) {
			technical.add(new ElasticTechnicalMetadata(md));
		}
	}

	/**
	 * @return the checksum
	 */
	public String getChecksum() {
		return checksum;
	}

	/**
	 * @return the width
	 */
	public long getWidth() {
		return width;
	}

	/**
	 * @return the height
	 */
	public long getHeight() {
		return height;
	}

	/**
	 * @return the technical
	 */
	public List<ElasticTechnicalMetadata> getTechnical() {
		return technical;
	}

	/**
	 * @return the fulltext
	 */
	public String getFulltext() {
		return fulltext;
	}

}
