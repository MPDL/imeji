package de.mpg.imeji.presentation.search.simple;

import java.io.IOException;
import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.util.UrlHelper;

/**
 * Java bean for the simple search
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "QuickSearchBean")
@RequestScoped
public class QuickSearchBean implements Serializable {
  private static final Logger LOGGER = Logger.getLogger(QuickSearchBean.class);
  private static final long serialVersionUID = 1599497861175666068L;
  private String searchString;

  /**
   * Method when search is submitted
   *
   * @return
   * @throws IOException
   */
  public QuickSearchBean() {
    final String q = UrlHelper.getParameterValue("q");
    try {
      if (SearchQueryParser.isSimpleSearch(SearchQueryParser.parseStringQuery(q))) {
        this.searchString = q;
      } else {
        searchString = "";
      }
    } catch (final UnprocessableError e) {
      LOGGER.error("Error parsing query", e);
    }
  }

  /**
   * setter
   *
   * @param searchString
   */
  public void setSearchString(String searchString) {
    this.searchString = searchString;
  }

  /**
   * getter
   *
   * @return
   */
  public String getSearchString() {
    return searchString;
  }
}
