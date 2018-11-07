package de.mpg.imeji.logic.export;

import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.User;

/**
 * Export of data
 *
 * @author saquet
 */
public abstract class ExportAbstract {
	protected final User user;
	protected long size = -1;
	protected String name;
	private final Map<String, Integer> itemsPerCollection = new HashMap<String, Integer>();

	public ExportAbstract(User user) {
		this.user = user;
	}

	/**
	 * Perform the export to the {@link OutputStream}
	 *
	 * @param out
	 * @param sr
	 *
	 */
	public abstract void export(OutputStream out) throws ImejiException;

	/**
	 * Return the Mime-type of the http response
	 *
	 * @return
	 */
	public abstract String getContentType();

	/**
	 * Return the number of Items downloaded according to their collection
	 * 
	 * @return
	 */
	public Map<String, Integer> getExportedItemsPerCollection() {
		return itemsPerCollection;
	}

	protected void createItemsPerCollection(Collection<Item> items) {
		for (Item item : items) {
			if (itemsPerCollection.containsKey(item.getCollection().toString())) {
				final int newVal = itemsPerCollection.get(item.getCollection().toString()).intValue() + 1;
				itemsPerCollection.put(item.getCollection().toString(), Integer.valueOf(newVal));
			} else {
				itemsPerCollection.put(item.getCollection().toString(), new Integer(1));
			}
		}
	}

	/**
	 * The size of the export
	 * 
	 * @return
	 */
	public String getSize() {
		return Long.toString(size);
	}

	/**
	 * Return the name of the export
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}
}
