package de.mpg.imeji.presentation.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.commons.lang3.EnumUtils;

import de.mpg.imeji.logic.config.ImejiLicenses;
import de.mpg.imeji.logic.vo.License;

/**
 * Backing bean for the LicenseViewer component
 * 
 * @author saquet
 *
 */
@ManagedBean(name = "LicenseViewer")
@RequestScoped
public class LicenseViewer {

  /**
   * Return a license according to its name. The license must be an {@link ImejiLicenses}
   * 
   * @param name
   * @return
   */
  public License getLicense(String name) {
    if (EnumUtils.isValidEnum(ImejiLicenses.class, name)) {
      return new License(ImejiLicenses.valueOf(name));
    }
    return null;
  }

  /**
   * True if the license is active, i.e. not ended
   * 
   * @param license
   * @return
   */
  public boolean isActive(License license) {
    return license != null && license.getEnd() < 0;
  }

  /**
   * Sort the license by the start date
   * 
   * @param licenses
   * @return
   */
  public List<License> getLicensesSortedByDate(List<License> licenses) {
    List<License> sorted = new ArrayList<>(licenses);
    Collections.sort(sorted, (l1, l2) -> l1.getStart() > l2.getStart() ? -1 : 1);
    return sorted;

  }
}
