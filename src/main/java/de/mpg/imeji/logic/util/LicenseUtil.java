package de.mpg.imeji.logic.util;

import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.License;

/**
 * Utility Class for {@link License}
 * 
 * @author saquet
 *
 */
public class LicenseUtil {

  private LicenseUtil() {
    // avoid construction
  }

  /**
   * Return all the active licenses
   * 
   * @param licenses
   * @return
   */
  public static List<License> getActiveLicenses(List<License> licenses) {
    List<License> actives = new ArrayList<>();
    for (License lic : licenses) {
      if (lic.getEnd() < 0) {
        actives.add(lic);
      }
    }
    return actives;
  }

  /**
   * Remove all duplicates (only for active licenses)
   * 
   * @param licenses
   * @return
   */
  public static List<License> removeDuplicates(List<License> licenses) {
    List<License> noDuplicates = new ArrayList<>();
    List<License> actives = getActiveLicenses(licenses);
    for (License active : actives) {
      if (!containsLicense(active, noDuplicates)) {
        noDuplicates.add(active);
      }
    }
    noDuplicates.addAll(getRevokedLicenses(licenses));
    return noDuplicates;
  }

  /**
   * Return al licenses which have an end
   * 
   * @param licenses
   * @return
   */
  public static List<License> getRevokedLicenses(List<License> licenses) {
    List<License> revoked = new ArrayList<>();
    for (License lic : licenses) {
      if (lic.getEnd() > 0) {
        revoked.add(lic);
      }
    }
    return revoked;
  }

  /**
   * True if the license is already in the list of licenses
   * 
   * @param license
   * @param licenses
   * @return
   */
  public static boolean containsLicense(License license, List<License> licenses) {
    for (License lic : licenses) {
      if (isSame(lic, license)) {
        return true;
      }
    }
    return false;
  }

  /**
   * True if 2 licenses are same
   * 
   * @param lic1
   * @param lic2
   * @return
   */
  public static boolean isSame(License lic1, License lic2) {
    return lic1 != null && lic2 != null && !lic1.isEmtpy() && lic1.getName().equals(lic2.getName())
        && lic1.getUrl().equals(lic2.getUrl());
  }

  /**
   * Return the current license of an item. Null if no current license found
   * 
   * @param item
   * @return
   * @throws ImejiException
   */
  public static License getActiveLicense(Item item) {
    List<License> actives = getActiveLicenses(item.getLicenses());
    if (actives.isEmpty()) {
      return null;
    } else {
      return getNewerLicense(actives);
    }
  }

  /**
   * Return the newer license between from the list (i.e the last created)
   * 
   * @param lic1
   * @param lic2
   * @return
   */
  public static License getNewerLicense(List<License> licenses) {
    License newer = licenses.get(0);
    for (License lic : licenses) {
      if (lic.getStart() > newer.getStart() || lic.getStart() < 0) {
        newer = lic;
      }
    }
    return newer;
  }
}
