package de.mpg.imeji.logic.vo.factory;

import java.net.URI;

import de.mpg.imeji.logic.vo.CollectionImeji;

/**
 * Factory for {@link CollectionImeji}
 *
 * @author saquet
 *
 */
public class CollectionFactory {
  private final CollectionImeji collection = new CollectionImeji();

  public CollectionFactory() {
    // constructor
  }

  public CollectionImeji build() {
    return collection;
  }

  public CollectionFactory setId(String id) {
    collection.setId(URI.create(id));
    return this;
  }

}
