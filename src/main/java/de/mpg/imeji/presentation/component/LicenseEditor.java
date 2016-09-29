package de.mpg.imeji.presentation.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.faces.model.SelectItem;

import org.apache.commons.lang3.EnumUtils;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.util.LicenseUtil;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.License;
import de.mpg.imeji.logic.vo.Properties.Status;

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
    CC_BY("Attribution 4.0 International (CC BY 4.0)",
        "https://creativecommons.org/licenses/by/4.0/"), CC_BY_SA(
            "Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)",
            "https://creativecommons.org/licenses/by-sa/4.0/"), PDDL(
                "ODC Public Domain Dedication and Licence",
                "http://opendatacommons.org/licenses/pddl/summary/"), ODC_By(
                    "Open Data Commons Attribution License (ODC-By) v1.0",
                    "http://opendatacommons.org/licenses/by/summary/"), ODC_ODbL(
                        "Open Database License (ODbL) v1.0",
                        "http://opendatacommons.org/licenses/odbl/summary/"), CC0(
                            "Public Domain Dedication (CC0 1.0)",
                            "https://creativecommons.org/publicdomain/zero/1.0/");

    private final String url;
    private final String label;

    private ImejiLicenses(String label, String url) {
      this.url = url;
      this.label = label;
    }
  }

  private List<SelectItem> licenseMenu;
  private String licenseName;
  private String licenseLabel;
  private String licenseUrl;
  private boolean showInput = false;
  private String customLicenseName;
  private String customLicenseUrl;
  private static final String NO_LICENSE = "no_license";

  /**
   * Constructor
   * 
   * @throws ImejiException
   */
  public LicenseEditor(Locale locale, Item item) {
    License active = LicenseUtil.getActiveLicense(item);
    this.licenseMenu = new ArrayList<>();
    this.showInput =
        !(active == null || EnumUtils.isValidEnum(ImejiLicenses.class, active.getName()));
    if (!showInput) {
      this.licenseName =
          active == null ? Imeji.RESOURCE_BUNDLE.getLabel(NO_LICENSE, locale) : active.getName();
    } else {
      this.customLicenseName = active.getName();
      this.customLicenseUrl = active.getUrl();
    }
    if (item.getStatus().equals(Status.PENDING)) {
      licenseMenu.add(new SelectItem(Imeji.RESOURCE_BUNDLE.getLabel(NO_LICENSE, locale)));
    }
    for (ImejiLicenses lic : ImejiLicenses.values()) {
      licenseMenu.add(new SelectItem(lic.name(), lic.label));
    }
    init(item);
  }

  /**
   * Constructor for the batch edit
   * 
   * @param locale
   */
  public LicenseEditor(Locale locale) {
    this.licenseMenu = new ArrayList<>();
    this.showInput = false;
    this.licenseName = Imeji.RESOURCE_BUNDLE.getLabel(NO_LICENSE, locale);
    licenseMenu.add(new SelectItem(Imeji.RESOURCE_BUNDLE.getLabel(NO_LICENSE, locale)));
    for (ImejiLicenses lic : ImejiLicenses.values()) {
      licenseMenu.add(new SelectItem(lic.name(), lic.label));
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
        this.licenseLabel = l.getLabel();
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
      this.licenseLabel = lic.label;
    } else {
      this.licenseName = null;
      this.licenseUrl = null;
      this.licenseLabel = null;
    }
  }

  /**
   * Get the license of the entered via the component
   * 
   * @param lic
   * @return
   */
  public License getLicense() {
    License license = new License();
    if (showInput) {
      license.setName(customLicenseName);
      license.setUrl(customLicenseUrl);
      license.setLabel(customLicenseName);
    } else if (EnumUtils.isValidEnum(ImejiLicenses.class, licenseName)) {
      license.setName(licenseName);
      license.setUrl(licenseUrl);
      license.setLabel(licenseLabel);
    }
    return license;
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

  /**
   * @return the licenseLabel
   */
  public String getLicenseLabel() {
    return licenseLabel;
  }

  /**
   * @param licenseLabel the licenseLabel to set
   */
  public void setLicenseLabel(String licenseLabel) {
    this.licenseLabel = licenseLabel;
  }

  public void toggleShowInput() {
    showInput = showInput ? false : true;
  }

  /**
   * @return the showInput
   */
  public boolean isShowInput() {
    return showInput;
  }

  /**
   * @param showInput the showInput to set
   */
  public void setShowInput(boolean showInput) {
    this.showInput = showInput;
  }

  /**
   * @return the customLicenseName
   */
  public String getCustomLicenseName() {
    return customLicenseName;
  }

  /**
   * @param customLicenseName the customLicenseName to set
   */
  public void setCustomLicenseName(String customLicenseName) {
    this.customLicenseName = customLicenseName;
  }

  /**
   * @return the customLicenseUrl
   */
  public String getCustomLicenseUrl() {
    return customLicenseUrl;
  }

  /**
   * @param customLicenseUrl the customLicenseUrl to set
   */
  public void setCustomLicenseUrl(String customLicenseUrl) {
    this.customLicenseUrl = customLicenseUrl;
  }
}
