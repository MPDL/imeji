/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.lang;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.config.util.ProxyHelper;

/**
 * Utility class for Iso638_1 languages vocabulary
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class Iso639_1Helper {
  private static final Logger LOGGER = Logger.getLogger(Iso639_1Helper.class);
  private List<SelectItem> list = null;

  /**
   * Default constructor
   */
  public Iso639_1Helper() {
    list = new ArrayList<SelectItem>();
    // parseVocabularyString(getVocabularyString());
    initLanguageList();
  }

  /**
   * Instead of reading the date in CoNE, give a static list. Improves a performance on the start
   * page, by avoiding expensive http request .
   */
  private void initLanguageList() {
    list = new ArrayList<SelectItem>();
    list.add(new SelectItem("en", "en - English"));
    list.add(new SelectItem("de", "de - German"));
    list.add(new SelectItem("jp", "jp - Japanese"));
    list.add(new SelectItem("es", "es - Spanish"));
  }

  /**
   * Get the Iso638_1 languages vocabulary from CoNe with the options format
   *
   * @return
   */
  private String getVocabularyString() {
    try {
      HttpClient client = new HttpClient();
      String coneVocabularyPath = PropertyReader.getProperty("cone.isos639_1.all");
      if (isNullOrEmpty(coneVocabularyPath)) {
        LOGGER.info(
            "CONE Service Property for Language Vocabularies has not been set-up. Will use default vocabulary for languages."
                + "NOTE: This is not an error: for more information on setting cone, check http://imeji.org/?s=cone.isos639_1.all ");
        return null;

      }
      GetMethod getMethod = new GetMethod(coneVocabularyPath + "?format=options");
      ProxyHelper.executeMethod(client, getMethod);
      return IOUtils.toString(getMethod.getResponseBodyAsStream());
    } catch (Exception e) {
      LOGGER.error("Couldn't read ISO639_1 vocabulary, will use default one! Error: " + e);
      return null;
    }
  }

  /**
   * Parse the Result as defined in Cone
   *
   * @param v
   */
  private void parseVocabularyString(String v) {
    try {
      for (String l : v.split("\n")) {
        String[] s = l.split("\\|");
        list.add(new SelectItem(s[0], s[1]));
      }
    } catch (Exception e) {
      list = new ArrayList<SelectItem>();
      list.add(new SelectItem("en", "en - English"));
      list.add(new SelectItem("de", "de - German"));
    }
  }

  /**
   * getter
   *
   * @return
   */
  public List<SelectItem> getList() {
    return list;
  }

  /**
   * setter
   *
   * @param list
   */
  public void setList(List<SelectItem> list) {
    this.list = list;
  }
}
