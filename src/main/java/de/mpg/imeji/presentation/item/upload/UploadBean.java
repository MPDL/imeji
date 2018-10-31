package de.mpg.imeji.presentation.item.upload;

import static de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS.AND;
import static de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS.OR;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Grant.GrantType;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;
import de.mpg.imeji.logic.security.authorization.Authorization;
import de.mpg.imeji.logic.security.authorization.util.SecurityUtil;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.collection.tree.Tree;
import de.mpg.imeji.presentation.session.BeanHelper;

@ManagedBean(name = "UploadBean")
@ViewScoped
public class UploadBean extends SuperBean {
	private static final long serialVersionUID = 4632180647351059603L;
	private static final Logger LOGGER = LogManager.getLogger(UploadBean.class);
	private List<CollectionImeji> collections = new ArrayList<>();
	private String query = "";
	private Tree tree;

	public UploadBean() {

	}

	public void init() {
		try {
			if (getSessionUser() != null) {
				filterCollections();
				if (collections.isEmpty() && SecurityUtil.authorization().hasCreateCollectionGrant(getSessionUser())) {
					redirect(getNavigation().getCreateCollectionUrl() + "?showUpload=1");
				} else if (collections.isEmpty()
						&& !SecurityUtil.authorization().hasCreateCollectionGrant(getSessionUser())) {
					BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("cannot_create_collection", getLocale()));
				}
			}
		} catch (Exception e) {
			BeanHelper.error("Error initializing page: " + e.getMessage());
			// LOGGER.error("Error initializing upload page", e);
		}
	}

	public void filterCollections() throws ImejiException {
		SearchFactory factory = new SearchFactory();
		factory.addElement(new SearchPair(SearchFields.title, query + "*"), OR);
		if (!new Authorization().isSysAdmin(getSessionUser())) {
			factory.addElement(new SearchPair(SearchFields.role, GrantType.EDIT.name().toLowerCase()), AND);
		}
		collections = new CollectionService().searchAndRetrieve(factory.build(),
				new SortCriterion(SearchFields.modified, SortOrder.DESCENDING), getSessionUser(),
				Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX);
		tree = new Tree(collections);
	}

	/**
	 * @return the collections
	 */
	public List<CollectionImeji> getCollections() {
		return collections;
	}

	/**
	 * @param collections
	 *            the collections to set
	 */
	public void setCollections(List<CollectionImeji> collections) {
		this.collections = collections;
	}

	/**
	 * @return the query
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * @param query
	 *            the query to set
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	/**
	 * @return the tree
	 */
	public Tree getTree() {
		return tree;
	}

	/**
	 * @param tree
	 *            the tree to set
	 */
	public void setTree(Tree tree) {
		this.tree = tree;
	}

}
