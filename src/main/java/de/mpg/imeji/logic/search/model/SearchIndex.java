// package de.mpg.imeji.logic.search.model;
//
// import java.io.Serializable;
// import java.util.ArrayList;
// import java.util.List;
//
// import de.mpg.imeji.logic.vo.Statement;
//
/// **
// * Element of a {@link SearchPair}, defines the index of the searched elements
// *
// * @author saquet (initial creation)
// * @author $Author$ (last modification)
// * @version $Revision$ $LastChangedDate$
// */
// public class SearchIndex implements Serializable {
// private static final long serialVersionUID = 8909950834646667282L;
//
// private String name;
// private String namespace;
// private SearchFields field;
// private SearchIndex parent;
// private List<SearchIndex> children = new ArrayList<SearchIndex>();
// private boolean listType = false;
//
// /**
// * Simple {@link SearchIndex} without namespace.
// *
// * @param name
// * @param namespace
// */
// public SearchIndex(SearchFields field) {
// this.name = field.name();
// this.field = field;
// }
//
// /**
// * Construct a new {@link SearchIndex} with a name and a namespace
// *
// * @param name
// * @param namespace
// */
// public SearchIndex(String name, String namespace) {
// this.name = name;
// this.namespace = namespace;
// this.field = SearchFields.valueOf(name);
// }
//
// /**
// * Construct a new {@link SearchIndex} with a name and a namespace and parent {@link SearchIndex}
// *
// * @param name
// * @param namespace
// * @param parent
// */
// public SearchIndex(String name, String namespace, SearchIndex parent) {
// this(name, namespace);
// this.parent = parent;
// if (parent != null && !parent.getChildren().contains(this)) {
// parent.getChildren().add(this);
// }
// }
//
// /**
// * Return all the necessary {@link SearchIndex} to search for a {@link Metadata} defined with a
// * {@link Statement}
// *
// * @param st
// * @return
// */
// public static List<SearchIndex> getAllIndexForStatement(Statement st) {
// final List<SearchIndex> list = new ArrayList<SearchIndex>();
// switch (st.getType()) {
// case DATE:
// list.add(SearchIndexes.getIndex(SearchFields.time));
// break;
// case GEOLOCATION:
// list.add(SearchIndexes.getIndex(SearchFields.location));
// break;
// case NUMBER:
// list.add(SearchIndexes.getIndex(SearchFields.number));
// break;
// case PERSON:
// list.add(SearchIndexes.getIndex(SearchFields.person_family));
// list.add(SearchIndexes.getIndex(SearchFields.person_given));
// list.add(SearchIndexes.getIndex(SearchFields.person_org));
// break;
// case TEXT:
// list.add(SearchIndexes.getIndex(SearchFields.text));
// break;
// case URL:
// list.add(SearchIndexes.getIndex(SearchFields.url));
// list.add(SearchIndexes.getIndex(SearchFields.label));
// break;
// }
// return list;
// }
//
// public boolean hasParent() {
// return getParent() != null;
// }
//
// public SearchIndex getParent() {
// return parent;
// }
//
// public void setParent(SearchIndex parent) {
// this.parent = parent;
// }
//
// public void setChildren(List<SearchIndex> children) {
// this.children = children;
// }
//
// public List<SearchIndex> getChildren() {
// return children;
// }
//
// public void setListType(boolean listType) {
// this.listType = listType;
// }
//
// public boolean isListType() {
// return listType;
// }
//
// public void setNamespace(String namespace) {
// this.namespace = namespace;
// }
//
// public String getNamespace() {
// return namespace;
// }
//
// public void setName(String name) {
// this.name = name;
// }
//
// public String getName() {
// return name;
// }
//
// /**
// * @return the field
// */
// public SearchFields getField() {
// return field;
// }
//
// /**
// * @param field the field to set
// */
// public void setField(SearchFields field) {
// this.field = field;
// }
// }
