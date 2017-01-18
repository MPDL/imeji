/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.export;

import java.io.OutputStream;
import java.util.Map;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.vo.User;

/**
 * Export of data
 *
 * @author saquet
 */
public abstract class ExportAbstract {
  protected final User user;
  protected long size = -1;
  protected String name;

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
  public abstract Map<String, Integer> getExportedItemsPerCollection();

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
