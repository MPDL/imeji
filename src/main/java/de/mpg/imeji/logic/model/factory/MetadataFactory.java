package de.mpg.imeji.logic.model.factory;

import de.mpg.imeji.logic.model.Metadata;
import de.mpg.imeji.logic.model.Person;
import de.mpg.imeji.logic.util.ObjectHelper;

/**
 * Factory for {@link Metadata}
 *
 * @author saquet
 *
 */
public class MetadataFactory {

	private final Metadata metadata = new Metadata();

	public MetadataFactory() {
		// default constructor
	}

	/**
	 * Construct a new metadata out of an existing metadata
	 *
	 * @param metadata
	 */
	public MetadataFactory(Metadata metadata) {
		ObjectHelper.copyAllFields(metadata, this.metadata);
	}

	public Metadata build() {
		return metadata;
	}

	public MetadataFactory setStatementId(String statementId) {
		metadata.setIndex(statementId);
		return this;
	}

	public MetadataFactory setText(String text) {
		metadata.setText(text);
		return this;
	}

	public MetadataFactory setNumber(double number) {
		metadata.setNumber(number);
		return this;
	}

	public MetadataFactory setUrl(String url) {
		metadata.setUrl(url);
		return this;
	}

	public MetadataFactory setPerson(Person person) {
		metadata.setPerson(person);
		return this;
	}

	public MetadataFactory setLatitude(double latitude) {
		metadata.setLatitude(latitude);
		return this;
	}

	public MetadataFactory setLongitude(double longitude) {
		metadata.setLongitude(longitude);
		return this;
	}

	public MetadataFactory setName(String name) {
		metadata.setName(name);
		return this;
	}

	public MetadataFactory setTitle(String title) {
		metadata.setTitle(title);
		return this;
	}

	public MetadataFactory setDate(String date) {
		metadata.setDate(date);
		return this;
	}
}
