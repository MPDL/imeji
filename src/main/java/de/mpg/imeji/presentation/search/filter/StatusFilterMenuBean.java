package de.mpg.imeji.presentation.search.filter;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;

@ManagedBean(name = "StatusFilterMenuBean")
@ViewScoped
public class StatusFilterMenuBean extends SuperFilterMenuBean {
  private static final long serialVersionUID = -820658514106886929L;
  private static final Logger LOGGER = LogManager.getLogger(StatusFilterMenuBean.class);

  public StatusFilterMenuBean() throws UnprocessableError {
    super();
  }

  @PostConstruct
  public void init() {
    try {
      init(initMenu(!StringHelper.isNullOrEmptyTrim(UrlHelper.getParameterValue("collectionId"))));
    } catch (final UnprocessableError e) {
      LOGGER.error("Error initializing StatusFilterMenuBean", e);
    }
  }

  private List<SelectItem> initMenu(boolean isCollectionFilter) throws UnprocessableError {
    final List<SelectItem> menu = new ArrayList<SelectItem>();
    if (!isCollectionFilter) {
      menu.add(new SelectItem(SearchQuery.toSearchQuery(new SearchPair(SearchFields.status, SearchOperators.EQUALS, "private", false)),
          Imeji.RESOURCE_BUNDLE.getLabel("only_private", getLocale())));
    }
    menu.add(new SelectItem(SearchQuery.toSearchQuery(new SearchPair(SearchFields.status, SearchOperators.EQUALS, "public", false)),
        Imeji.RESOURCE_BUNDLE.getLabel("published", getLocale())));
    menu.add(new SelectItem(SearchQuery.toSearchQuery(new SearchPair(SearchFields.status, SearchOperators.EQUALS, "discarded", false)),
        Imeji.RESOURCE_BUNDLE.getLabel("only_withdrawn", getLocale())));
    return menu;
  }
}
