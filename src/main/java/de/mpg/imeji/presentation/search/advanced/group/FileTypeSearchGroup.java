package de.mpg.imeji.presentation.search.advanced.group;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.ImejiFileTypes.Type;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.search.model.SearchPair;

/**
 * Search group for the filetypes
 *
 * @author saquet
 *
 */
public class FileTypeSearchGroup extends AbstractAdvancedSearchFormGroup implements Serializable {
  private static final long serialVersionUID = 1439809243185106214L;
  private static final Logger LOGGER = Logger.getLogger(FileTypeSearchGroup.class);
  private List<String> selected = new ArrayList<>();
  private List<SelectItem> menu;

  public FileTypeSearchGroup(Locale locale) {
    initMenu(locale);
  }

  public FileTypeSearchGroup(String value, Locale locale) {
    this(locale);
    initSelected(value, locale);
  }


  @Override
  public SearchElement toSearchElement() {
    try {
      return new SearchFactory().or(selected.stream()
          .map(s -> new SearchPair(SearchFields.filetype, s)).collect(Collectors.toList()))
          .buildAsGroup();
    } catch (UnprocessableError e) {
      LOGGER.error("Error building file type query", e);
      return new SearchPair();
    }
  }

  @Override
  public void validate() {

  }

  private void initMenu(Locale locale) {
    menu = new ArrayList<>();
    for (final Type type : Imeji.CONFIG.getFileTypes().getTypes()) {
      menu.add(new SelectItem(type.getName(locale.getLanguage())));
    }
  }

  /**
   * Init the selected file types according the query
   */
  private void initSelected(String value, Locale locale) {
    selected = new ArrayList<String>();
    for (final String t : value.split(" OR ")) {
      final Type type = Imeji.CONFIG.getFileTypes().getType(t);
      if (type != null) {
        selected.add(type.getName(locale.getLanguage()));
      }
      selected.add(t);
    }
  }


  @SuppressWarnings("unchecked")
  public void listener(ValueChangeEvent event) {
    selected = (List<String>) event.getNewValue();
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
