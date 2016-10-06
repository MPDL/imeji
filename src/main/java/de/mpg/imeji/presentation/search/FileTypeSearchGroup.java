package de.mpg.imeji.presentation.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.lang3.StringUtils;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.config.ImejiFileTypes.Type;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;

/**
 * Search group for the filetypes
 * 
 * @author saquet
 *
 */
public class FileTypeSearchGroup implements Serializable {
  private static final long serialVersionUID = 1439809243185106214L;
  private List<String> selected;
  private final SearchPair pair =
      new SearchPair(SearchFields.filetype, SearchOperators.REGEX, "", false);
  private List<SelectItem> menu;

  public FileTypeSearchGroup(Locale locale) {
    initMenu(locale);
  }

  public FileTypeSearchGroup(String value, Locale locale) {
    this(locale);
    initSelected(value, locale);
  }

  private void initMenu(Locale locale) {
    menu = new ArrayList<>();
    for (Type type : Imeji.CONFIG.getFileTypes().getTypes()) {
      menu.add(new SelectItem(type.getName(locale.getLanguage())));
    }
  }

  /**
   * Init the selected file types according the query
   */
  private void initSelected(String value, Locale locale) {
    selected = new ArrayList<String>();
    for (String t : value.split(" OR ")) {
      Type type = Imeji.CONFIG.getFileTypes().getType(t);
      if (type != null) {
        selected.add(type.getName(locale.getLanguage()));
      }
      selected.add(t);
    }
  }


  @SuppressWarnings("unchecked")
  public void listener(ValueChangeEvent event) {
    selected = (List<String>) event.getNewValue();
    pair.setValue(StringUtils.join(selected, " OR "));
  }

  /**
   * True if the search is empty
   * 
   * @return
   */
  public boolean isEmpty() {
    return selected == null || selected.isEmpty();
  }

  /**
   * @return the selected
   */
  public List<String> getSelected() {
    return selected;
  }

  /**
   * @param selected the selected to set
   */
  public void setSelected(List<String> selected) {
    this.selected = selected;
  }

  /**
   * @return the pair
   */
  public SearchPair getPair() {
    return pair;
  }

  /**
   * @return the menu
   */
  public List<SelectItem> getMenu() {
    return menu;
  }

  /**
   * @param menu the menu to set
   */
  public void setMenu(List<SelectItem> menu) {
    this.menu = menu;
  }
}
