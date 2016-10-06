package de.mpg.imeji.presentation.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.lang3.StringUtils;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.config.ImejiLicenses;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;

/**
 * Search group for licenses
 * 
 * @author saquet
 *
 */
public class LicenseSearchGroup implements Serializable {
  private static final long serialVersionUID = -2822491289836043116L;
  private List<String> selected = new ArrayList<>();
  private List<SelectItem> menu;

  public LicenseSearchGroup(Locale locale) {
    initMenu(locale);
  }

  /**
   * Init the search group with a search value
   * 
   * @param value
   */
  public LicenseSearchGroup(String value, Locale locale) {
    this(locale);
    selected = Arrays.asList(value.split(" OR "));
  }

  /**
   * return the grooup as a search pair
   * 
   * @return
   */
  public SearchPair asSearchPair() {
    return new SearchPair(SearchFields.license, SearchOperators.REGEX,
        StringUtils.join(selected, " OR "), false);
  }

  public boolean isEmpty() {
    return selected.isEmpty();
  }

  private void initMenu(Locale locale) {
    menu = new ArrayList<>();
    menu.add(new SelectItem(ImejiLicenses.NO_LICENSE,
        Imeji.RESOURCE_BUNDLE.getLabel("no_license", locale)));
    for (ImejiLicenses lic : ImejiLicenses.values()) {
      menu.add(new SelectItem(lic.name(), lic.getLabel()));
    }
  }

  @SuppressWarnings("unchecked")
  public void selectedListener(ValueChangeEvent event) {
    selected = (List<String>) event.getNewValue();
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
