package de.mpg.imeji.logic.search.elasticsearch.model;

import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.model.ImejiLicenses;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.License;
import de.mpg.imeji.logic.model.Metadata;
import de.mpg.imeji.logic.model.util.LicenseUtil;
import de.mpg.imeji.logic.util.StorageUtils;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * The object which is indexed in Elastic search <br/>
 * !!! IMPORTANT !!!<br/>
 * This File must be synchronized with
 * resources/elasticsearch/ElasticItemsMapping.json
 *
 * @author bastiens
 *
 */
public final class ElasticItem extends ElasticProperties {
	private final String folder;
	private final String name;
	private final String license;
	private final String filetype;
	private final String fileextension;
	private final long size;
	private final List<ElasticMetadata> metadata = new ArrayList<>();

	/**
	 * Constructor with an {@link Item}
	 *
	 * @param item
	 */
	public ElasticItem(Item item) {
		super(item);
		this.folder = item.getCollection() != null ? item.getCollection().toString() : null;
		this.name = item.getFilename();
		this.license = getLicenseName(item);
		this.size = item.getFileSize();
		this.filetype = item.getFiletype();
		this.fileextension = StorageUtils.getExtensionFromFileName(this.name);
		if (item.getMetadata() != null) {
			for (final Metadata md : item.getMetadata()) {
				metadata.add(new ElasticMetadata(md));
			}
		}
	}

	private String getLicenseName(Item item) {
		final License license = LicenseUtil.getActiveLicense(item);
		if (license != null) {
			return !StringHelper.isNullOrEmptyTrim(license.getName()) ? license.getName() : license.getUrl();
		}
		return ImejiLicenses.NO_LICENSE;
	}

	/**
	 * @return the filetype
	 */
	public String getFiletype() {
		return filetype;
	}

	/**
	 * 
	 * @return the file extension
	 */
	public String getFileextension() {
		return this.fileextension;
	}
	/**
	 * @return the metadata
	 */
	public List<ElasticMetadata> getMetadata() {
		return metadata;
	}

	/**
	 * @return the folder
	 */
	public String getFolder() {
		return folder;
	}

	public long getSize() {
		return size;
	}

	public String getLicense() {
		return license;
	}

	public String getName() {
		return name;
	}

}
