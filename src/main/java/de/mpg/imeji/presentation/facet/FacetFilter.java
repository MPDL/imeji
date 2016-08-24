/// **
// * License: src/main/resources/license/escidoc.license
// */
// package de.mpg.imeji.presentation.facet;
//
// import java.net.URI;
// import java.util.Locale;
//
// import org.apache.log4j.Logger;
//
// import de.mpg.imeji.logic.search.SearchQueryParser;
// import de.mpg.imeji.logic.search.model.SearchQuery;
// import de.mpg.imeji.presentation.beans.MetadataLabels;
//
/// **
// * {@link Facet} with extended
// *
// * @author saquet
// */
// public class FacetFilter extends Facet {
// private String query = "";
// private URI collectionID;
// private String removeQuery = "";
// private SearchQuery searchQuery;
//
// /**
// * Constructor
// *
// * @param label
// * @param query
// * @param count
// * @param type
// * @param metadataURI
// */
// public FacetFilter(String label, String query, int count, FacetType type, URI metadataURI,
// Locale locale, MetadataLabels metadataLabels) {
// super(null, label, count, type, metadataURI, locale, metadataLabels);
// this.query = query;
// init();
// }
//
// /**
// * Initialize the {@link FacetFilter}
// */
// public void init() {
// try {
// if (FacetType.SEARCH == getType()) {
// searchQuery = SearchQueryParser.parseStringQuery(query);
// }
// } catch (Exception e) {
// Logger.getLogger(FacetFilter.class).error("Some issues during Filter initialization", e);
// }
// }
//
// /**
// * Getter
// *
// * @return
// */
// public URI getCollectionID() {
// return collectionID;
// }
//
// /**
// * Setter
// *
// * @param collectionID
// */
// public void setCollectionID(URI collectionID) {
// this.collectionID = collectionID;
// }
//
// @Override
// public int getCount() {
// return count;
// }
//
// @Override
// public void setCount(int count) {
// this.count = count;
// }
//
// /**
// * Getter
// *
// * @return
// */
// public String getQuery() {
// return query;
// }
//
// /**
// * setter
// *
// * @param query
// */
// public void setQuery(String query) {
// this.query = query;
// }
//
// /**
// * getter
// *
// * @return
// */
// public String getRemoveQuery() {
// return removeQuery;
// }
//
// /**
// * setter
// *
// * @param removeQuery
// */
// public void setRemoveQuery(String removeQuery) {
// this.removeQuery = removeQuery;
// }
//
// /**
// * getter
// *
// * @return
// */
// public SearchQuery getSearchQuery() {
// return searchQuery;
// }
//
// /**
// * setter
// *
// * @param searchQuery
// */
// public void setSearchQuery(SearchQuery searchQuery) {
// this.searchQuery = searchQuery;
// }
// }
