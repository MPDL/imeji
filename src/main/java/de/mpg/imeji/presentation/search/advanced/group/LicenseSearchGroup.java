package de.mpg.imeji.presentation.search.advanced.group;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.model.ImejiLicenses;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;

/**
 * Search group for licenses
 *
 * @author saquet
 *
 */
public class LicenseSearchGroup extends AbstractAdvancedSearchFormGroup implements Serializable {
  private static final long serialVersionUID = -2822491289836043116L;
  private static final Logger LOGGER = LogManager.getLogger(LicenseSearchGroup.class);
  private List<String> selected = new ArrayList<>();
  private List<SelectItem> menu;
  private String hasLicense = "all";

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

  @Override
  public SearchElement toSearchElement() {
    if ("true".equals(hasLicense)) {
      return new SearchPair(SearchFields.license, SearchOperators.EQUALS, "*", false);
    } else if ("false".equals(hasLicense)) {
      return new SearchPair(SearchFields.license, SearchOperators.EQUALS, "no_license", false);
    } else {
      try {
        return new SearchFactory().or(selected.stream().map(s -> new SearchPair(SearchFields.license, s)).collect(Collectors.toList()))
            .buildAsGroup();
      } catch (UnprocessableError e) {
        LOGGER.error("Error building license query", e);
        return new SearchPair();
      }
    }
  }

  @Override
  public void validate() {
    // TODO Auto-generated method stub

  }

  public boolean isEmpty() {
    return "all".equals(hasLicense);
  }

  private void initMenu(Locale locale) {
    menu = new ArrayList<>();
    for (final ImejiLicenses lic : ImejiLicenses.values()) {
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

  /**
   * @return the hasLicense
   */
  public String getHasLicense() {
    return hasLicense;
  }

  /**
   * @param hasLicense the hasLicense to set
   */
  public void setHasLicense(String hasLicense) {
    this.hasLicense = hasLicense;
  }

}
