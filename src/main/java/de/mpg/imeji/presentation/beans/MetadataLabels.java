/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.beans;

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.statement.StatementService;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Statement;

/**
 * Utility class for the labels of the {@link Metadata}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class MetadataLabels implements Serializable {
  private static final long serialVersionUID = -5672593145712801376L;
  private String lang = "en";
  private Map<URI, String> labels = new HashMap<URI, String>();
  private Map<URI, String> internationalizedLabels = new HashMap<URI, String>();
  private static final Logger LOGGER = Logger.getLogger(MetadataLabels.class);

  public MetadataLabels(List<Item> items, Locale locale) {
    lang = locale.getLanguage();
  }

  public MetadataLabels(Item item, Locale locale) {
    lang = locale.getLanguage();
  }

  public MetadataLabels(Locale locale) {
    lang = locale.getLanguage();
    try {
      init(new StatementService().searchAndRetrieve(null, null, Imeji.adminUser, -1, 0));
    } catch (ImejiException e) {
      LOGGER.error("Error initializing metadatalables", e);
    }
  }


  /**
   * Initialize the labels for one {@link MetadataProfile}
   *
   * @param profile
   * @throws Exception
   */
  private void init(List<Statement> statements) {
    labels = new HashMap<URI, String>();
    internationalizedLabels = new HashMap<URI, String>();
    for (Statement s : statements) {
      for (String name : s.getNames()) {
        labels.put(s.getUri(), name);
        internationalizedLabels.put(s.getUri(), name);
      }
    }
  }

  /**
   * getter
   *
   * @return
   */
  public Map<URI, String> getLabels() {
    return labels;
  }

  /**
   * setter
   *
   * @param labels
   */
  public void setLabels(Map<URI, String> labels) {
    this.labels = labels;
  }

  public String getLang() {
    return lang;
  }

  /**
   * getter
   *
   * @return
   */
  public Map<URI, String> getInternationalizedLabels() {
    return internationalizedLabels;
  }

  /**
   * setter
   *
   * @param internationalizedLabels
   */
  public void setInternationalizedLabels(Map<URI, String> internationalizedLabels) {
    this.internationalizedLabels = internationalizedLabels;
  }
}
