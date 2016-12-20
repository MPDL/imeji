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

import de.mpg.imeji.logic.statement.StatementService;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.util.LocalizedString;

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
    init(new StatementService().searchAndRetrieve());
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
      boolean hasInternationalizedLabel = false;
      boolean hasEnglishLabel = false;
      String labelFallBack = null;
      for (LocalizedString ls : s.getLabels()) {
        if (ls.getLang().equals("en")) {
          labels.put(s.getUri(), ls.getValue());
          hasEnglishLabel = true;
        }
        if (ls.getLang().equals(lang)) {
          internationalizedLabels.put(s.getUri(), ls.getValue());
          hasInternationalizedLabel = true;
        }
        labelFallBack = ls.getValue();
      }
      if (!hasEnglishLabel) {
        labels.put(s.getUri(), labelFallBack);
      }
      if (!hasInternationalizedLabel) {
        internationalizedLabels.put(s.getUri(), labels.get(s.getId()));
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
