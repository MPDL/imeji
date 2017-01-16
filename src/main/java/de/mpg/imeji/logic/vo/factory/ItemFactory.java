package de.mpg.imeji.logic.vo.factory;

import java.net.URI;

import de.mpg.imeji.logic.vo.Item;

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
}
