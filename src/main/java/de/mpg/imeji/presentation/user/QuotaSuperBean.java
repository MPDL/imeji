package de.mpg.imeji.presentation.user;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.lang3.math.NumberUtils;

import de.mpg.imeji.presentation.beans.ConfigurationBean;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Super class to implements quota methods, which can be reused by JSF Bean displaying quota menu
 * <br/>
 * NOTE: This is NOT a JSF Bean
 * 
 * @author bastiens
 *
 */
public class QuotaSuperBean {

  private String quota = ConfigurationBean.getDefaultQuotaStatic();
  private List<SelectItem> quotaMenu;

  /**
   * default Constructor
   */
  public QuotaSuperBean() {
    quotaMenu = new ArrayList<>();
    SessionBean session = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    for (String limit : ConfigurationBean.getQuotaLimitsStaticAsList()) {
      if (NumberUtils.isNumber(limit)) {
        quotaMenu.add(new SelectItem(limit));
      } else {
        quotaMenu.add(new SelectItem(limit, session.getLabel(limit)));
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
}
