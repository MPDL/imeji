package de.mpg.imeji.logic.search.elasticsearch.model;

import de.mpg.imeji.logic.model.ContainerAdditionalInfo;

/**
 * Elastic Object for {@link ContainerAdditionalInfo}
 *
 * @author bastiens
 *
 */
public final class ElasticContainerAdditionalInfo {
	private final String label;
	private final String text;
	private final String url;

	public ElasticContainerAdditionalInfo(ContainerAdditionalInfo info) {
		this.label = info.getLabel();
		this.text = info.getText();
		this.url = info.getUrl();
	}

	public String getLabel() {
		return label;
	}

	public String getText() {
		return text;
	}

	public String getUrl() {
		return url;
	}
}
