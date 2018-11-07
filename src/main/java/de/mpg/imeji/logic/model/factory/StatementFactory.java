package de.mpg.imeji.logic.model.factory;

import java.net.URI;
import java.util.List;

import de.mpg.imeji.logic.model.Metadata;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.StatementType;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * Factory for {@link Statement}
 *
 * @author saquet
 *
 */
public class StatementFactory {

	private final Statement statement = new Statement();

	public StatementFactory initFromMetadata(Metadata md) {
		setIndex(md.getIndex());
		if (!StringHelper.isNullOrEmptyTrim(md.getDate())) {
			setType(StatementType.DATE);
		} else if (!Double.isNaN(md.getNumber())) {
			setType(StatementType.NUMBER);
		} else if (!StringHelper.isNullOrEmptyTrim(md.getTitle()) || !StringHelper.isNullOrEmptyTrim(md.getUrl())) {
			setType(StatementType.URL);
		} else if (!StringHelper.isNullOrEmptyTrim(md.getName())
				|| (!Double.isNaN(md.getLatitude()) && !Double.isNaN(md.getLongitude()))) {
			setType(StatementType.GEOLOCATION);
		} else if (md.getPerson() != null && !StringHelper.isNullOrEmptyTrim(md.getPerson().getFamilyName())) {
			setType(StatementType.PERSON);
		} else {
			setType(StatementType.TEXT);
		}
		return this;
	}

	/**
	 * Build the statement
	 *
	 * @return
	 */
	public Statement build() {
		return statement;
	}

	public StatementFactory setUri(URI uri) {
		statement.setUri(uri);
		return this;
	}

	public StatementFactory setIndex(String index) {
		statement.setIndex(index);
		return this;
	}

	public StatementFactory setType(StatementType type) {
		statement.setType(type);
		return this;
	}

	public StatementFactory setNamespace(String namespace) {
		statement.setNamespace(namespace);
		return this;
	}

	public StatementFactory setVocabulary(URI vocabulary) {
		statement.setVocabulary(vocabulary);
		return this;
	}

	public StatementFactory setLiteralsConstraints(List<String> literalConstraints) {
		statement.setLiteralConstraints(literalConstraints);
		return this;
	}
}
