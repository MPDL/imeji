package de.mpg.imeji.presentation.beans;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.navigation.Navigation;
import de.mpg.imeji.presentation.navigation.history.HistoryPage;

/**
 * This bean is a utility Bean. It can be extended to get some basic session information
 *
 * @author bastiens
 *
 */
@ManagedBean(name = "SuperBean")
@ViewScoped
public class SuperBean implements Serializable {
  private static final long serialVersionUID = -5167729051940514378L;
  @ManagedProperty(value = "#{SessionBean.user}")
  private User sessionUser;
  @ManagedProperty(value = "#{InternationalizationBean.locale}")
  private Locale locale;
  @ManagedProperty(value = "#{Navigation}")
  private Navigation navigation;
  @ManagedProperty(value = "#{HistorySession.previousPage}")
  private HistoryPage previousPage;
  @ManagedProperty(value = "#{HistorySession.currentPage}")
  private HistoryPage currentPage;
  @ManagedProperty(value = "#{SessionBean.selected}")
  private List<String> selectedItems;
  private final String backUrl;

  public SuperBean() {
    this.backUrl = UrlHelper.getParameterValue("back");
  }

  protected void resetSelectedItems() {
    selectedItems.clear();
  }

  /**
   * Redirect to the passed url
   *
   * @param url
   * @throws IOException
   */
  protected void redirect(String url) throws IOException {
    FacesContext.getCurrentInstance().getExternalContext().redirect(url);
  }

  /**
   * Go to the previsous page, either as defined in the url or via the history
   * 
   * @throws IOException
   */
  protected void goBack() throws IOException {
    redirect(!StringHelper.isNullOrEmptyTrim(backUrl) ? URLDecoder.decode(backUrl, "UTF-8")
        : getPreviousPage().getCompleteUrl());
  }

  /**
   * Reload current Page
   * 
   * @throws IOException
   */
  protected void reload() throws IOException {
    redirect(getCurrentPage().getCompleteUrl());
  }

  /**
   * 
   * @param str
   * @return
   * @throws UnsupportedEncodingException
   */
  public String UTF8(String str) {
    try {
      return URLEncoder.encode(str, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return str;
    }
  }

  /**
   * @return the navigation
   */
  public Navigation getNavigation() {
    return navigation;
  }

  /**
   * @param navigation the navigation to set
   */
  public void setNavigation(Navigation navigation) {
    this.navigation = navigation;
  }

  /**
   * @return the sessionUser
   */
  public User getSessionUser() {
    return sessionUser;
  }

  /**
   * @param sessionUser the sessionUser to set
   */
  public void setSessionUser(User sessionUser) {
    this.sessionUser = sessionUser;
  }

  /**
   * @return the locale
   */
  public Locale getLocale() {
    return locale;
  }

  /**
   * @param locale the locale to set
   */
  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  /**
   * @return the previousPage
   */
  public HistoryPage getPreviousPage() {
    return previousPage;
  }

  /**
   * @param previousPage the previousPage to set
   */
  public void setPreviousPage(HistoryPage previousPage) {
    this.previousPage = previousPage;
  }

  /**
   * @return the currentPage
   */
  public HistoryPage getCurrentPage() {
    return currentPage;
  }

  /**
   * @param currentPage the currentPage to set
   */
  public void setCurrentPage(HistoryPage currentPage) {
    this.currentPage = currentPage;
  }

  public String getBackUrl() {
    return backUrl;
  }

  /**
   * @return the selected
   */
  public List<String> getSelectedItems() {
    return selectedItems;
  }

  /**
   * @param selected the selected to set
   */
  public void setSelectedItems(List<String> selected) {
    this.selectedItems = selected;
  }


}
