package de.mpg.imeji.presentation.filter;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.history.HistoryPage;

@ManagedBean(name = "SearchFilterBean")
@ViewScoped
public class SearchFilterBean extends SuperBean {
  private static final long serialVersionUID = -5218953160575937596L;
  public boolean hasQuery;

  @PostConstruct
  public void init() {
    this.hasQuery = !StringHelper.isNullOrEmptyTrim(UrlHelper.getParameterValue("q"));
  }

  /**
   * Get the URL to remove all Filter and Search
   * 
   * @return
   */
  public String getClearFilterAndSearchQuery() {
    HistoryPage page = getHistory().getCurrentPage();
    page.setParamValue("q", "");
    return page.getCompleteUrl();
  }

  public boolean isHasQuery() {
    return hasQuery;
  }

  public void setHasQuery(boolean hasQuery) {
    this.hasQuery = hasQuery;
  }

}
