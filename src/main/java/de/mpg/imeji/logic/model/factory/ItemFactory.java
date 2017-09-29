package de.mpg.imeji.logic.model.factory;

import java.io.File;
import java.net.URI;
import java.util.List;

import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.License;
import de.mpg.imeji.logic.util.StorageUtils;

/**
 * Factory for Item
 *
 * @author saquet
 *
 */
public class ItemFactory {

  private final Item item = new Item();

  public ItemFactory() {

  }

  public Item build() {
    return item;
  }

  public ItemFactory setId(String id) {
    item.setId(URI.create(id));
    return this;
  }

  public ItemFactory setFilename(String filename) {
    item.setFilename(filename);
    return this;
  }

  public ItemFactory setFile(File f) {
    item.setFileSize(f.length());
    item.setFiletype(StorageUtils.getMimeType(f));
    return this;
  }

  public ItemFactory setCollection(String collectionId) {
    item.setCollection(URI.create(collectionId));
    return this;
  }

  public ItemFactory setLicenses(List<License> list) {
    item.setLicenses(list);
    return this;
  }
}
