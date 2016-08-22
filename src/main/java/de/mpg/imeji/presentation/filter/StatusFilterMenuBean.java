package de.mpg.imeji.presentation.filter;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;

@ManagedBean(name = "StatusFilterMenuBean")
@ViewScoped
public class StatusFilterMenuBean extends SuperFilterMenuBean {
  private static final long serialVersionUID = -820658514106886929L;

  @PostConstruct
  public void init() {
    try {
      init(initMenu());
    } catch (UnprocessableError e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private List<SelectItem> initMenu() throws UnprocessableError {
    List<SelectItem> menu = new ArrayList<SelectItem>();
    menu.add(new SelectItem(
        SearchQueryParser.transform2URL(transformSearchElementToQuery(
            new SearchPair(SearchFields.status, SearchOperators.REGEX, "private", false))),
        Imeji.RESOURCE_BUNDLE.getLabel("only_private", getLocale())));
    menu.add(new SelectItem(
        SearchQueryParser.transform2URL(transformSearchElementToQuery(
            new SearchPair(SearchFields.status, SearchOperators.REGEX, "public", false))),
        Imeji.RESOURCE_BUNDLE.getLabel("only_public", getLocale())));
    menu.add(new SelectItem(
        SearchQueryParser.transform2URL(transformSearchElementToQuery(
            new SearchPair(SearchFields.status, SearchOperators.REGEX, "discarded", false))),
        Imeji.RESOURCE_BUNDLE.getLabel("only_withdrawn", getLocale())));
    return menu;
  }

}
