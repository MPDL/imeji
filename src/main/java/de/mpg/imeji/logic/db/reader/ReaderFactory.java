package de.mpg.imeji.logic.db.reader;

/**
 * Factory for {@link Reader}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ReaderFactory {
	/**
	 * Create a Reader according to the uri of the model
	 *
	 * @param modelURI
	 * @return
	 */
	public static Reader create(String modelURI) {
		return new JenaReader(modelURI);
	}
}
