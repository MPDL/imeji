package de.mpg.imeji.logic.storage;

import de.mpg.imeji.logic.storage.impl.ExternalStorage;
import de.mpg.imeji.logic.storage.impl.InternalStorage;

/**
 * Factory to create a Storage client
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class StorageFactory {

	private StorageFactory() {
		// private Constructor
	}

	/**
	 * Factory for {@link Storage} implementations. Create a new {@link Storage}
	 * according to the passed name. If no known name is passed, return the external
	 * storage implementation
	 *
	 * @param name
	 * @return
	 */
	public static Storage create(String name) {
		if ("internal".equals(name)) {
			return new InternalStorage();
		}
		return new ExternalStorage();
	}
}
