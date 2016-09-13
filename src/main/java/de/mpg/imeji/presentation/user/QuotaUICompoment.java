package de.mpg.imeji.presentation.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.faces.model.SelectItem;

import org.apache.commons.lang3.math.NumberUtils;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.util.QuotaUtil;
import de.mpg.imeji.logic.vo.User;

/**
 * Super class to implements quota methods, which can be reused by JSF Bean displaying quota menu
 * <br/>
 *
 * @author bastiens
 *
 */

public class QuotaUICompoment implements Serializable {
  private static final long serialVersionUID = -8215734534906829819L;
  private String quota = Imeji.CONFIG.getDefaultQuota();
  private List<SelectItem> quotaMenu;
  private final String humanReadableQuota;

  /**
   * default Constructor
   */
  public QuotaUICompoment(User user, Locale locale) {
    this.quotaMenu = new ArrayList<>();
    this.humanReadableQuota = QuotaUtil.getQuotaHumanReadable(user.getQuota(), Locale.ENGLISH);
    this.quota = user.getQuota() > 0 ? humanReadableQuota.replace("GB", "").trim() : quota;
    for (String limit : Imeji.CONFIG.getQuotaLimitsAsList()) {
      if (NumberUtils.isNumber(limit)) {
        quotaMenu.add(new SelectItem(limit.trim()));
      } else {
        quotaMenu.add(new SelectItem(limit.trim(), Imeji.RESOURCE_BUNDLE.getLabel(limit, locale)));
      }
    }
  }

  /**
   *
   * @return the quota
   */
  public String getQuota() {
    return quota;
  }

  /**
   * @param quota the quota to set
   */
  public void setQuota(String quota) {
    this.quota = quota;
  }

  /**
   * @return the quotaMenu
   */
  public List<SelectItem> getQuotaMenu() {
    return quotaMenu;
  }

  /**
   * @param quotaMenu the quotaMenu to set
   */
  public void setQuotaMenu(List<SelectItem> quotaMenu) {
    this.quotaMenu = quotaMenu;
  }

  public String getHumanReadableQuota() {
    return humanReadableQuota;
  }
}
