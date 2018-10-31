package de.mpg.imeji.presentation.item.edit.batch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Metadata;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.item.edit.EditMetadataAbstract;
import de.mpg.imeji.presentation.item.edit.MetadataInputComponent;
import de.mpg.imeji.presentation.item.edit.SelectStatementComponent;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * JSF Bean for the Edit batch page
 * 
 * @author saquet
 *
 */
@ManagedBean(name = "EditItemsBatchBean")
@ViewScoped
public class EditItemsBatchBean extends EditMetadataAbstract {
	private static final long serialVersionUID = -288843834798781232L;
	private static final Logger LOGGER = LogManager.getLogger(EditItemsBatchBean.class);
	private String collectionId;
	private CollectionImeji collection;
	private String query;
	private String backUrl;
	private SelectStatementComponent statementSelector;
	private MetadataInputComponent input;
	private List<Item> items = new ArrayList<>();

	@PostConstruct
	public void init() {
		collectionId = UrlHelper.getParameterValue("col");
		initCollection();
		this.setBackUrl(getNavigation().getCollectionUrl() + collectionId);
		reset();
	}

	private void initCollection() {
		try {
			collection = new CollectionService().retrieveLazy(ObjectHelper.getURI(CollectionImeji.class, collectionId),
					getSessionUser());
		} catch (ImejiException e) {
			LOGGER.error("error retrieving collection", e);
		}
	}

	public void initInput() {
		Statement statement = statementSelector.asStatement();
		input = new MetadataInputComponent(ImejiFactory.newMetadata(statement).build(), statement);
	}

	/**
	 * Reset the editor
	 */
	public void reset() {
		statementSelector = new SelectStatementComponent(statementMap);
		input = null;
	}

	/**
	 * Append a new metadata to all items
	 */
	public void append() {
		try {
			retrieveItems();
			items.stream().forEach(item -> item.getMetadata().add(getMetadata()));
			save();
			reset();
			BeanHelper.addMessage(Imeji.RESOURCE_BUNDLE.getMessage("success_items_save", getLocale()));
		} catch (UnprocessableError e) {
			BeanHelper.error(e, getLocale());
		} catch (ImejiException | IOException e) {
			BeanHelper.error("Error saving editor: " + e.getMessage());
			LOGGER.error("Error saving batch editor");
		}
	}

	/**
	 * Add the metadata to items which don't have any value for this statement
	 */
	public void fill() {
		try {
			retrieveItems();
			items.stream()
					.filter(item -> item.getMetadata().stream()
							.noneMatch(md -> md.getIndex().equals(statementSelector.getIndex())))
					.sequential().forEach(item -> item.getMetadata().add(getMetadata()));
			save();
			reset();
			BeanHelper.addMessage(Imeji.RESOURCE_BUNDLE.getMessage("success_items_save", getLocale()));
		} catch (UnprocessableError e) {
			BeanHelper.error(e, getLocale());
		} catch (ImejiException | IOException e) {
			BeanHelper.error("Error saving editor: " + e.getMessage());
			LOGGER.error("Error saving batch editor");
		}
	}

	/**
	 * Overwrite all metadata for the current statement
	 */
	public void overwrite() {
		try {
			retrieveItems();
			items.stream()
					.peek(item -> item.setMetadata(item.getMetadata().stream()
							.filter(md -> !md.getIndex().equals(statementSelector.getIndex()))
							.collect(Collectors.toList())))
					.forEach(item -> item.getMetadata().add(getMetadata()));
			save();
			reset();
			BeanHelper.addMessage(Imeji.RESOURCE_BUNDLE.getMessage("success_items_save", getLocale()));
		} catch (UnprocessableError e) {
			BeanHelper.error(e, getLocale());
		} catch (ImejiException | IOException e) {
			BeanHelper.error("Error saving editor: " + e.getMessage());
			LOGGER.error("Error saving batch editor");
		}
	}

	/**
	 * Return a copy of the Metadata in the
	 * {@link SelectStatementWithInputComponent}
	 * 
	 * @return
	 */
	private Metadata getMetadata() {
		return input.getMetadata().copy();
	}

	/**
	 * Retrieve the current items
	 * 
	 * @return
	 * @throws ImejiException
	 * @throws IOException
	 */
	private void retrieveItems() throws ImejiException, IOException {
		SearchQuery q = SearchQueryParser.parseStringQuery(query);
		if (collectionId != null) {
			items = itemService.searchAndRetrieve(ObjectHelper.getURI(CollectionImeji.class, collectionId), q, null,
					getSessionUser(), Search.SEARCH_FROM_START_INDEX, Search.GET_ALL_RESULTS);
		} else {
			items = itemService.searchAndRetrieve(q, null, getSessionUser(), Search.GET_ALL_RESULTS,
					Search.SEARCH_FROM_START_INDEX);
		}
	}

	@Override
	public List<Item> toItemList() {
		return items;
	}

	@Override
	public List<Statement> getAllStatements() {
		return Arrays.asList(input.getStatement());
	}

	/**
	 * @return the statementSelector
	 */
	public SelectStatementComponent getStatementSelector() {
		return statementSelector;
	}

	/**
	 * @param statementSelector
	 *            the statementSelector to set
	 */
	public void setStatementSelector(SelectStatementComponent statementSelector) {
		this.statementSelector = statementSelector;
	}

	/**
	 * @return the backUrl
	 */
	public String getBackUrl() {
		return backUrl;
	}

	/**
	 * @param backUrl
	 *            the backUrl to set
	 */
	public void setBackUrl(String backUrl) {
		this.backUrl = backUrl;
	}

	/**
	 * @return the input
	 */
	public MetadataInputComponent getInput() {
		return input;
	}

	/**
	 * @param input
	 *            the input to set
	 */
	public void setInput(MetadataInputComponent input) {
		this.input = input;
	}

	/**
	 * @return the collection
	 */
	public CollectionImeji getCollection() {
		return collection;
	}

	/**
	 * @param collection
	 *            the collection to set
	 */
	public void setCollection(CollectionImeji collection) {
		this.collection = collection;
	}
}
