package de.mpg.imeji.logic.db.writer;

/**
 * Factory for {@link Writer}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class WriterFactory {
	/**
	 * Create a {@link Writer} for a model
	 *
	 * @param modelURI
	 * @return
	 */
	public static Writer create(String modelURI) {
		return new JenaWriter(modelURI);
	}
}
