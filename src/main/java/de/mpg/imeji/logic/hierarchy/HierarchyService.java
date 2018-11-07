package de.mpg.imeji.logic.hierarchy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.hierarchy.Hierarchy.Node;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * Service to manage the hierarchy between imeji objects
 * 
 * @author saquet
 *
 */
public class HierarchyService implements Serializable {
	private static final long serialVersionUID = -3479895793901732353L;

	private static Hierarchy hierarchy = new Hierarchy();
	private Map<String, String> collectionsNameMap = new HashMap<>();

	public HierarchyService() {
		if (hierarchy == null) {
			hierarchy = new Hierarchy();
			hierarchy.init();
		}
	}

	/**
	 * Reload the hierarchy
	 */
	public static void reloadHierarchy() {
		hierarchy.init();
	}

	/**
	 * REturn the complete Hierarchy
	 * 
	 * @return
	 */
	public Hierarchy getFullHierarchy() {
		return hierarchy;
	}

	/**
	 * Find all Subcollection of the collection/subcollection
	 * 
	 * @param collectionUri
	 * @return
	 */
	public List<String> findAllSubcollections(String collectionUri) {
		return hierarchy.getTree().get(collectionUri) != null
				? hierarchy.getTree().get(collectionUri).stream()
						.flatMap(s -> Stream.concat(Stream.of(s), findAllSubcollections(s).stream()))
						.collect(Collectors.toList())
				: new ArrayList<>();
	}

	/**
	 * Return a list with this collectionUri and all uris of its subcollections
	 * 
	 * @param collectionUri
	 * @return
	 */
	public List<String> addAllSubcollections(String collectionUri) {
		List<String> l = findAllSubcollections(collectionUri);
		l.add(collectionUri);
		return l;
	}

	/**
	 * True if the collectionId is a child of the parentId
	 * 
	 * @param collectionId
	 * @param parentId
	 * @return
	 */
	public boolean isChildOf(String collectionId, String parentId) {
		return findAllSubcollections(parentId).contains(collectionId);
	}

	/**
	 * Find the list of all parents of the objects
	 * 
	 * @param o
	 * @return
	 */
	public List<String> findAllParents(Object o) {
		List<String> l = new ArrayList<>();
		String uri = getParentUri(o);
		if (uri != null) {
			l.addAll(findAllParents(uri));
			l.add(uri);
			return l;
		}
		return l;
	}

	/**
	 * Return the list of all parents of the object with this uri
	 * 
	 * @param parentUri
	 * @return
	 */
	public List<String> findAllParents(String uri) {
		List<String> l = new ArrayList<>();
		String parent = getParent(uri);
		if (parent != null) {
			l.addAll(findAllParents(parent));
			l.add(parent);
		}
		return l;
	}

	/**
	 * Same as findAllParents(String uri) , but wraps the results with the
	 * collection names
	 * 
	 * @param uri
	 * @return
	 */
	public List<CollectionUriNameWrapper> findAllParentsWithNames(String uri, boolean includeSelft) {
		List<CollectionUriNameWrapper> l = findAllParents(uri).stream().map(id -> new CollectionUriNameWrapper(id))
				.collect(Collectors.toList());
		if (includeSelft) {
			l.add(new CollectionUriNameWrapper(uri));
		}
		return l;
	}

	/**
	 * Return the Parent uri of the passed uri
	 * 
	 * @param uri
	 * @return
	 */
	public String getParent(String uri) {
		Node n = hierarchy.getNodes().get(uri);
		return n != null ? n.getParent() : null;
	}

	/**
	 * Return the last parent of the object
	 * 
	 * @param o
	 * @return
	 */
	public String getLastParent(Object o) {
		String uri = getParentUri(o);
		if (uri != null) {
			return getLastParent(uri);
		}
		return null;
	}

	/**
	 * Get the last parent of the object according to its first parent
	 * 
	 * @param firstParent
	 * @return
	 */
	public String getLastParent(String firstParent) {
		Node parentNode = hierarchy.getNodes().get(firstParent);
		if (parentNode != null) {
			return getLastParent(parentNode.getParent());
		}
		return firstParent;
	}

	/**
	 * If the object is an Item or a collection and has a parent, return the uri of
	 * its parent
	 * 
	 * @param o
	 * @return
	 */
	private String getParentUri(Object o) {
		if (o instanceof Item) {
			return ((Item) o).getCollection().toString();
		}
		if (o instanceof CollectionImeji) {
			return ((CollectionImeji) o).getCollection() != null
					? ((CollectionImeji) o).getCollection().toString()
					: null;
		}
		return null;
	}

	/**
	 * Return the name and the uri of a collection in one wrapper
	 * 
	 * @author saquet
	 *
	 */
	public class CollectionUriNameWrapper {
		private final String uri;
		private final String name;

		public CollectionUriNameWrapper(String uri) {
			this.uri = uri;
			this.name = initCollectionName(uri);
		}

		/**
		 * Check if the collection is in the collectionMap, else search the name and add
		 * it to the map
		 * 
		 * @param uri
		 * @return
		 */
		private String initCollectionName(String uri) {
			String name = collectionsNameMap.get(uri);
			if (StringHelper.isNullOrEmptyTrim(name)) {
				name = findCollectionName(uri);
				collectionsNameMap.put(uri, name);
			}
			return name;
		}

		/**
		 * Search in the db the name of the collection
		 * 
		 * @param uri
		 * @return
		 */
		private String findCollectionName(String uri) {
			List<String> l = ImejiSPARQL.exec(JenaCustomQueries.selectCollectionName(uri), Imeji.collectionModel);
			return l.isEmpty() ? "" : l.get(0);
		}

		public String getName() {
			return name;
		}

		public String getUri() {
			return uri;
		}
	}
}
