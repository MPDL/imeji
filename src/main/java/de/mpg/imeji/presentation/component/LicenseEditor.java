package de.mpg.imeji.presentation.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.faces.model.SelectItem;

import org.apache.commons.lang3.EnumUtils;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.config.ImejiLicenses;
import de.mpg.imeji.logic.util.LicenseUtil;
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
      licenseMenu.add(new SelectItem(lic.name(), lic.getLabel()));
    }
    init(item);
  }

  /**
   * Constructor for the batch edit
   * 
   * @param locale
   */
  public LicenseEditor(Locale locale, boolean privat) {
    this.licenseMenu = new ArrayList<>();
    this.showInput = false;
    if (privat) {
      this.licenseName = Imeji.RESOURCE_BUNDLE.getLabel(NO_LICENSE, locale);
      licenseMenu.add(new SelectItem(Imeji.RESOURCE_BUNDLE.getLabel(NO_LICENSE, locale)));
    } else {
      this.licenseName = Imeji.CONFIG.getDefaultLicense();
    }
    for (ImejiLicenses lic : ImejiLicenses.values()) {
      licenseMenu.add(new SelectItem(lic.name(), lic.getLabel()));
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
      this.licenseUrl = lic.getUrl();
      this.licenseLabel = lic.getLabel();
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
      license = new License(ImejiLicenses.valueOf(licenseName));
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
