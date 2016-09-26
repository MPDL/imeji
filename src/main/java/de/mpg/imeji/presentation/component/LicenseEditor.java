package de.mpg.imeji.presentation.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.faces.model.SelectItem;

import org.apache.commons.lang3.EnumUtils;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.License;

/**
 * Editor to edit the license of an item
 * 
 * @author saquet
 *
 */
public class LicenseEditor implements Serializable {
  private static final long serialVersionUID = -2942345495443979609L;


  /**
   * List of predefined licenses for imeji
   * 
   * @author saquet
   *
   */
  public enum ImejiLicenses {
    CC_BY("https://creativecommons.org/licenses/by/4.0/"), CC_BY_SA(
        "https://creativecommons.org/licenses/by-sa/4.0/"), PDDL(
            "http://opendatacommons.org/licenses/pddl/summary/"), ODC_By(
                "http://opendatacommons.org/licenses/by/summary/"), ODC_ODbL(
                    "http://opendatacommons.org/licenses/odbl/summary/"), CC0(
                        "https://creativecommons.org/publicdomain/zero/1.0/");

    private final String url;

    private ImejiLicenses(String url) {
      this.url = url;
    }
  }

  private List<SelectItem> licenseMenu;
  private String licenseName;
  private String licenseUrl;
  private static final String NO_LICENSE = "no_license";

  /**
   * Cosntructor
   */
  public LicenseEditor(Locale locale) {
    this.licenseMenu = new ArrayList<>();
    this.licenseName = Imeji.RESOURCE_BUNDLE.getLabel(NO_LICENSE, locale);
    licenseMenu.add(new SelectItem(this.licenseName));
    for (ImejiLicenses lic : ImejiLicenses.values()) {
      licenseMenu.add(new SelectItem(lic.name()));
    }
  }

  /**
   * Init the component for a specific item
   * 
   * @param item
   */
  public void init(Item item) {
    for (License l : item.getLicenses()) {
      if (l.getEnd() < 0) {
        this.licenseName = l.getName();
        this.licenseUrl = l.getUrl();
      }
    }
  }

  public List<SelectItem> getLicenseMenu() {
    return licenseMenu;
  }

  /**
   * Listener for when the license menu is changed
   */
  public void licenseMenuListener() {
    if (EnumUtils.isValidEnum(ImejiLicenses.class, licenseName)) {
      ImejiLicenses lic = ImejiLicenses.valueOf(licenseName);
      this.licenseUrl = lic.url;
    } else {
      this.licenseName = null;
      this.licenseUrl = null;
    }
  }

  /**
   * Get the license of the entered via the component
   * 
   * @param lic
   * @return
   */
  public License getLicense() {
    if (licenseName != null || licenseUrl != null) {
      License license = new License();
      license.setName(licenseName);
      license.setUrl(licenseUrl);
      license.setStart(System.currentTimeMillis());
      return license;
    }
    return null;
  }

  /**
   * @return the licenseName
   */
  public String getLicenseName() {
    return licenseName;
  }

  /**
   * @param licenseName the licenseName to set
   */
  public void setLicenseName(String licenseName) {
    this.licenseName = licenseName;
  }

  /**
   * @return the licenseUrl
   */
  public String getLicenseUrl() {
    return licenseUrl;
  }

  /**
   * @param licenseUrl the licenseUrl to set
   */
  public void setLicenseUrl(String licenseUrl) {
    this.licenseUrl = licenseUrl;
  }

  /**
   * @param licenseMenu the licenseMenu to set
   */
  public void setLicenseMenu(List<SelectItem> licenseMenu) {
    this.licenseMenu = licenseMenu;
  }
}
