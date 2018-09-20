package de.mpg.imeji.presentation.lang;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.logging.log4j.Logger; 
import org.apache.logging.log4j.LogManager;

/**
 * Utility class for Iso638_1 languages vocabulary
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class Iso639_1Helper {
  private static final Logger LOGGER = LogManager.getLogger(Iso639_1Helper.class);
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
    list.add(new SelectItem("en", "English"));
    list.add(new SelectItem("de", "German"));
    list.add(new SelectItem("ja", "Japanese"));
    list.add(new SelectItem("es", "Spanish"));
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
