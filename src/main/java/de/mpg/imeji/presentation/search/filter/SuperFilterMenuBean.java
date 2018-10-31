package de.mpg.imeji.presentation.search.filter;

import java.util.List;
import java.util.stream.Collectors;

import javax.faces.model.SelectItem;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Super Bean for Filter menu implementation.
 *
 * @author saquet
 *
 */
public class SuperFilterMenuBean extends SuperBean {
	private static final long serialVersionUID = 5211495478085868441L;
	private static final Logger LOGGER = LogManager.getLogger(SuperFilterMenuBean.class);
	private List<SelectItem> menu;
	private final SearchQuery filterQuery;
	private final String filterQueryString;
	private String selectedQueryLabel;
	private String selectedQuery;
	private SearchFactory selectedFactory;

	public SuperFilterMenuBean() throws UnprocessableError {
		this.filterQueryString = UrlHelper.getParameterValue("filter") == null
				? ""
				: UrlHelper.getParameterValue("filter");
		this.filterQuery = SearchQueryParser.parseStringQuery(filterQueryString);
	}

	public void init(List<SelectItem> menu) {
		try {
			initSelected(menu);
			this.menu = menu.stream()
					.map(i -> new SelectItem(getFilterLink((SearchQuery) i.getValue(), i.getLabel()), i.getLabel()))
					.collect(Collectors.toList());
		} catch (final Exception e) {
			BeanHelper.error("Error parsing query in the URL");
			LOGGER.error("Error parsing query in the URL", e);
		}
	}

	private void initSelected(List<SelectItem> menu) {
		SearchFactory factoryCurrentQuery = new SearchFactory(filterQuery);
		for (SelectItem item : menu) {
			if (factoryCurrentQuery.contains((SearchQuery) item.getValue())) {
				selectedFactory = factoryCurrentQuery.remove((SearchQuery) item.getValue());
				selectedQuery = buildPageUrl(SearchQueryParser.transform2URL(selectedFactory.build()));
				selectedQueryLabel = item.getLabel();
			}
		}
		if (selectedFactory == null) {
			selectedFactory = factoryCurrentQuery;
		}
	}

	protected String getFilterLink(SearchQuery q, String label) {
		try {
			if (label.equals(selectedQueryLabel)) {
				return selectedQuery;
			} else {
				return buildPageUrl(SearchQueryParser.transform2URL(selectedFactory.clone().and(q).build()));
			}
		} catch (Exception e) {
			LOGGER.error("Error building filter query", e);
			return "";
		}
	}

	/**
	 * Build the page url with the passed filter query
	 * 
	 * @param filterQuery
	 * @return
	 */
	private String buildPageUrl(String filterQuery) {
		return getCurrentPage().copy().setParamValue("filter", filterQuery).getCompleteUrl();
	}

	public List<SelectItem> getMenu() {
		return menu;
	}

	public String getSelectedQueryLabel() {
		return selectedQueryLabel;
	}

	public String getSelectedQuery() {
		return selectedQuery;
	}
}
