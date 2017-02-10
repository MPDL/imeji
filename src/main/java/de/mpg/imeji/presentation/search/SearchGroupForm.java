// package de.mpg.imeji.presentation.search;
//
// import java.io.Serializable;
// import java.net.URI;
// import java.util.ArrayList;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Locale;
// import java.util.Set;
//
// import javax.faces.model.SelectItem;
//
// import org.apache.log4j.Logger;
//
// import de.mpg.imeji.exceptions.ImejiException;
// import de.mpg.imeji.exceptions.UnprocessableError;
// import de.mpg.imeji.logic.collection.CollectionService;
// import de.mpg.imeji.logic.config.Imeji;
// import de.mpg.imeji.logic.search.model.SearchGroup;
// import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
// import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
// import de.mpg.imeji.logic.search.model.SearchOperators;
// import de.mpg.imeji.logic.search.model.SearchPair;
// import de.mpg.imeji.logic.search.model.SearchQuery;
// import de.mpg.imeji.logic.vo.CollectionImeji;
// import de.mpg.imeji.logic.vo.Statement;
// import de.mpg.imeji.logic.vo.User;
//
/// **
// * A {@link SearchGroupForm} is a group of {@link SearchMetadataForm}
// *
// * @author saquet (initial creation)
// * @author $Author$ (last modification)
// * @version $Revision$ $LastChangedDate$
// */
// public class SearchGroupForm implements Serializable {
// private static final long serialVersionUID = -498245563765543283L;
// private static final Logger LOGGER = Logger.getLogger(SearchGroupForm.class);
// private List<SearchMetadataForm> elements;
// private String collectionId;
// private List<SelectItem> collectionsMenu;
// private List<SelectItem> statementMenu;
//
// /**
// * Default Constructor
// *
// * @throws ImejiException
// */
// public SearchGroupForm(List<Statement> statements) {
// initStatementsMenu(statements);
// reset();
// }
//
// private void reset() {
// collectionId = null;
// elements = new ArrayList<SearchMetadataForm>();
// elements.add(new SearchMetadataForm());
// collectionsMenu = new ArrayList<SelectItem>();
// }
//
// /**
// * Constructor for a {@link SearchGroup} and {@link MetadataProfile}
// *
// * @param searchGroup
// * @param profile
// * @param collectionId
// * @throws ImejiException
// */
// public SearchGroupForm(SearchGroup searchGroup, User user, List<Statement> statements)
// throws ImejiException {
// this(statements);
// }
//
// /**
// * Return the {@link SearchGroupForm} as a {@link SearchGroup}
// *
// * @return
// */
// public SearchGroup getAsSearchGroup() {
// try {
// final SearchGroup groupWithAllMetadata = new SearchGroup();
// for (final SearchMetadataForm e : elements) {
// groupWithAllMetadata.addGroup(e.getAsSearchGroup());
// groupWithAllMetadata.addLogicalRelation(e.getLogicalRelation());
// }
// if (collectionId != null && !"".equals(collectionId)) {
// final SearchGroup searchGroup = new SearchGroup();
// searchGroup.addPair(new SearchPair(SearchFields.col, SearchOperators.EQUALS,
// collectionId.toString(), false));
// searchGroup.addLogicalRelation(LOGICAL_RELATIONS.AND);
// searchGroup.addGroup(groupWithAllMetadata);
// return searchGroup;
// }
// return groupWithAllMetadata;
// } catch (final UnprocessableError e) {
// LOGGER.error("Error transforming search group form to searchgroup", e);
// return new SearchGroup();
// }
//
// }
//
// /**
// * Validate the Search Group according to the user entries
// *
// * @throws UnprocessableError
// */
// public void validate() throws UnprocessableError {
// final Set<String> messages = new HashSet<>();
// for (final SearchMetadataForm el : elements) {
// try {
// el.validate();
// } catch (final UnprocessableError e) {
// messages.addAll(e.getMessages());
// }
// }
// if (!messages.isEmpty()) {
// throw new UnprocessableError(messages);
// }
// }
//
// /**
// * Initialize the {@link Statement} for the select menu in the form
// *
// * @param p
// * @throws ImejiException
// */
// public void initStatementsMenu(List<Statement> statements) {
// statementMenu = new ArrayList<>(statements.size());
// for (final Statement st : statements) {
// statementMenu.add(new SelectItem(st.getIndex(), st.getIndex()));
// }
// }
//
// /**
// * Load all the {@link CollectionImeji} using a {@link MetadataProfile} and return it as menu for
// * the searchgroup
// *
// * @param p
// * @return
// * @throws ImejiException
// */
// private List<SelectItem> getCollectionsMenu(Locale locale, User user) throws ImejiException {
// final CollectionService collectionService = new CollectionService();
// final SearchQuery q = new SearchQuery();
// final List<SelectItem> l = new ArrayList<SelectItem>();
// l.add(new SelectItem(null,
// Imeji.RESOURCE_BUNDLE.getLabel("adv_search_collection_restrict", locale)));
// for (final String uri : collectionService.search(q, null, user, -1, 0).getResults()) {
// final CollectionImeji c = collectionService.retrieveLazy(URI.create(uri), user);
// l.add(new SelectItem(c.getId().toString(), c.getTitle()));
// }
// return l;
// }
//
// public int getSize() {
// return elements.size();
// }
//
// public List<SearchMetadataForm> getSearchElementForms() {
// return elements;
// }
//
// public void setSearchElementForms(List<SearchMetadataForm> elements) {
// this.elements = elements;
// }
//
// public List<SelectItem> getStatementMenu() {
// return statementMenu;
// }
//
// public void setStatementMenu(List<SelectItem> statementMenu) {
// this.statementMenu = statementMenu;
// }
//
// public String getCollection() {
// return collectionId;
// }
//
// public void setCollection(String collection) {
// this.collectionId = collection;
// }
//
// public List<SelectItem> getCollectionsMenu() {
// return collectionsMenu;
// }
//
// public void setCollectionsMenu(List<SelectItem> collectionsMenu) {
// this.collectionsMenu = collectionsMenu;
// }
//
// }
