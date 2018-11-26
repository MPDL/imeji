package de.mpg.imeji.logic.core.facade;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.concurrency.Locks;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.License;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.util.LicenseUtil;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticIndices;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.security.authorization.Authorization;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.ObjectHelper.ObjectType;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.workflow.WorkflowValidator;
import de.mpg.imeji.util.DateHelper;

/**
 * Facade to Release or Withdraw collections
 * 
 * @author saquet
 *
 */
public class WorkflowFacade implements Serializable {
	private static final long serialVersionUID = 3966446108673573909L;
	private final Authorization authorization = new Authorization();
	private final WorkflowValidator workflowValidator = new WorkflowValidator();
	private final ElasticIndexer collectionIndexer = new ElasticIndexer(ElasticIndices.folders.name());
	private final ElasticIndexer itemIndexer = new ElasticIndexer(ElasticIndices.items.name());

	/**
	 * Release a collection and its item
	 * 
	 * @param c
	 * @param user
	 * @param defaultLicense
	 * @throws ImejiException
	 */
	public void release(CollectionImeji c, User user, License defaultLicense) throws ImejiException {
		preValidateRelease(c, user, defaultLicense);
		final List<String> itemIds = getItemIds(c, user);
		preValidateCollectionItems(itemIds, user);
		// Create a list with the collectionId, all the subcollectionids and all itemIds
		List<String> ids = new ArrayList<>(new HierarchyService().findAllSubcollections(c.getId().toString()));
		ids.add(c.getId().toString());
		ids.addAll(itemIds);
		Calendar now = DateHelper.getCurrentDate();
		String sparql = ids.stream().map(id -> JenaCustomQueries.updateReleaseObject(id, now))
				.collect(Collectors.joining("; "));
		addLicense(ids, user, defaultLicense);
		ImejiSPARQL.execUpdate(sparql);
		collectionIndexer.partialUpdateIndexBatch(ElasticIndices.folders.name(),
				filterIdsByType(ids, ObjectType.COLLECTION).stream()
						.map(id -> new StatusPart(id, Status.RELEASED, now, null)).collect(Collectors.toList()));
		itemIndexer.partialUpdateIndexBatch(ElasticIndices.items.name(), filterIdsByType(ids, ObjectType.ITEM).stream()
				.map(id -> new StatusPart(id, Status.RELEASED, now, null)).collect(Collectors.toList()));
	}

	/**
	 * Release items
	 * 
	 * @param items
	 * @param user
	 * @param defaultLicense
	 * @throws ImejiException
	 */
	public void releaseItems(List<Item> items, User user, License defaultLicense) throws ImejiException {
		final List<String> ids = items.stream().map(item -> item.getId().toString()).collect(Collectors.toList());
		preValidateReleaseItems(items, user, defaultLicense);
		Calendar now = DateHelper.getCurrentDate();
		String sparql = ids.stream().map(id -> JenaCustomQueries.updateReleaseObject(id, now))
				.collect(Collectors.joining("; "));
		addLicense(ids, user, defaultLicense);
		ImejiSPARQL.execUpdate(sparql);
		itemIndexer.partialUpdateIndexBatch(ElasticIndices.items.name(),
				ids.stream().map(id -> new StatusPart(id, Status.RELEASED, now, null)).collect(Collectors.toList()));
	}

	/**
	 * Withdraw the collection and its items
	 * 
	 * @param c
	 * @param comment
	 * @param user
	 * @throws ImejiException
	 */
	public void withdraw(CollectionImeji c, String comment, User user) throws ImejiException {

		prevalidateWithdraw(c, comment, user);
		final List<String> itemIds = getItemIds(c, user);
		if (itemIds != null && itemIds.size() > 0) {
			preValidateCollectionItems(itemIds, user);
		}

		// Create a list with the collectionId, all the subcollectionIds and all itemIds
		List<String> ids = new ArrayList<>(new HierarchyService().findAllSubcollections(c.getId().toString()));
		ids.add(c.getId().toString());
		ids.addAll(itemIds);

		Calendar now = DateHelper.getCurrentDate();

		// Update Jena
		String sparql = ids.stream().map(id -> JenaCustomQueries.updateWitdrawObject(id, now, comment))
				.collect(Collectors.joining("; "));
		ImejiSPARQL.execUpdate(sparql);

		// Update ElasticSearch
		collectionIndexer.partialUpdateIndexBatch(ElasticIndices.folders.name(),
				filterIdsByType(ids, ObjectType.COLLECTION).stream()
						.map(id -> new StatusPart(id, Status.WITHDRAWN, now, comment)).collect(Collectors.toList()));
		itemIndexer.partialUpdateIndexBatch(ElasticIndices.items.name(), filterIdsByType(ids, ObjectType.ITEM).stream()
				.map(id -> new StatusPart(id, Status.WITHDRAWN, now, comment)).collect(Collectors.toList()));
	}

	/**
	 * Withdraw a list of items
	 * 
	 * @param items
	 * @param comment
	 * @param user
	 * @throws ImejiException
	 */
	public void withdrawItems(List<Item> items, String comment, User user) throws ImejiException {
		prevalidateWithdrawItems(items, comment, user);
		List<String> itemIds = items.stream().map(item -> item.getId().toString()).collect(Collectors.toList());
		preValidateCollectionItems(itemIds, user);
		Calendar now = DateHelper.getCurrentDate();
		String sparql = itemIds.stream().map(id -> JenaCustomQueries.updateWitdrawObject(id, now, comment))
				.collect(Collectors.joining("; "));
		ImejiSPARQL.execUpdate(sparql);
		itemIndexer.partialUpdateIndexBatch(ElasticIndices.items.name(), itemIds.stream()
				.map(id -> new StatusPart(id, Status.WITHDRAWN, now, comment)).collect(Collectors.toList()));
	}

	/**
	 * Add License to all items which have'nt one
	 * 
	 * @param c
	 * @param ids
	 * @param user
	 * @param license
	 * @throws ImejiException
	 */
	private void addLicense(List<String> ids, User user, License license) throws ImejiException {
		List<Item> items = (List<Item>) new ItemService().retrieveBatch(
				filterIdsByType(ids, ObjectType.ITEM).stream().collect(Collectors.toList()), Search.GET_ALL_RESULTS,
				Search.SEARCH_FROM_START_INDEX, user);
		List<LicensePart> licenseParts = items.stream().filter(item -> LicenseUtil.getActiveLicense(item) == null)
				.map(item -> new LicensePart(item.getId().toString(), license)).collect(Collectors.toList());
		if (!licenseParts.isEmpty()) {
			String sparql = licenseParts.stream().map(p -> JenaCustomQueries.updateAddLicensetoItem(p.id, license))
					.collect(Collectors.joining("; "));
			ImejiSPARQL.execUpdate(sparql);
			itemIndexer.partialUpdateIndexBatch(ElasticIndices.items.name(), licenseParts);
		}
	}

	/**
	 * Perform prevalidation on the collection to check if the user can proceed to
	 * the workflow operation
	 * 
	 * @param collection
	 * @param user
	 * @param defaultLicense
	 * @throws ImejiException
	 */
	private void preValidateRelease(CollectionImeji collection, User user, License defaultLicense)
			throws ImejiException {
		if (user == null) {
			throw new AuthenticationError(AuthenticationError.USER_MUST_BE_LOGGED_IN);
		}
		if (!authorization.administrate(user, collection)) {
			throw new NotAllowedError(NotAllowedError.NOT_ALLOWED);
		}
		if (collection == null) {
			throw new NotFoundException("collection object does not exists");
		}
		if (defaultLicense == null) {
			throw new UnprocessableError("A default license is needed to release a collection");
		}
		workflowValidator.isReleaseAllowed(collection);
	}

	/**
	 * Perform prevalidation on the collection to check if the user can proceed to
	 * the withdraw operation
	 * 
	 * @param collection
	 * @param user
	 * @throws ImejiException
	 */
	private void prevalidateWithdraw(CollectionImeji collection, String comment, User user) throws ImejiException {
		workflowValidator.isWithdrawAllowed(collection);
		if (user == null) {
			throw new AuthenticationError(AuthenticationError.USER_MUST_BE_LOGGED_IN);
		}
		if (!authorization.administrate(user, collection)) {
			throw new NotAllowedError(NotAllowedError.NOT_ALLOWED);
		}
		if (collection == null) {
			throw new NotFoundException("collection object does not exists");
		}
		if (StringHelper.isNullOrEmptyTrim(comment)) {
			throw new UnprocessableError("Missing discard comment");
		}
	}

	/**
	 * Prevalidate the witdthraw pf an item
	 * 
	 * @param items
	 * @param comment
	 * @param user
	 * @throws ImejiException
	 */
	private void prevalidateWithdrawItems(List<Item> items, String comment, User user) throws ImejiException {
		for (Item item : items) {
			workflowValidator.isWithdrawAllowed(item);
			if (user == null) {
				throw new AuthenticationError(AuthenticationError.USER_MUST_BE_LOGGED_IN);
			}
			if (!authorization.administrate(user, item)) {
				throw new NotAllowedError(NotAllowedError.NOT_ALLOWED);
			}
			if (StringHelper.isNullOrEmptyTrim(comment)) {
				throw new UnprocessableError("Missing discard comment");
			}
		}
	}

	/**
	 * Check if the items can be released
	 * 
	 * @param items
	 * @param user
	 * @param defaultLicense
	 * @throws ImejiException
	 */
	private void preValidateReleaseItems(List<Item> items, User user, License defaultLicense) throws ImejiException {
		for (Item item : items) {
			workflowValidator.isReleaseAllowed(item);
			if (user == null) {
				throw new AuthenticationError(AuthenticationError.USER_MUST_BE_LOGGED_IN);
			}
			if (!authorization.administrate(user, item)) {
				throw new NotAllowedError(NotAllowedError.NOT_ALLOWED);
			}
		}
		if (defaultLicense == null) {
			throw new UnprocessableError("A default license is needed to release a collection");
		}
	}

	/**
	 * Perform prevalidation on the collection items to check if the user can
	 * proceed to the workflow operation
	 * 
	 * @param itemIds
	 * @param user
	 * @throws ImejiException
	 */
	private void preValidateCollectionItems(List<String> itemIds, User user) throws ImejiException {
		if (hasImageLocked(itemIds, user)) {
			throw new UnprocessableError("Collection has locked items: can not be released");
		}
		if (itemIds.isEmpty()) {
			throw new UnprocessableError("An empty collection can not be released!");
		}
	}

	/**
	 * Return the ids of all items of the collection and its subcollection
	 * 
	 * @param c
	 * @param user
	 * @return
	 * @throws UnprocessableError
	 */
	private List<String> getItemIds(CollectionImeji c, User user) throws UnprocessableError {
		return new ItemService()
				.search(c.getId(), new SearchFactory().and(new SearchPair(SearchFields.filename, "*")).build(), null,
						user, Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX)
				.getResults();
	}

	/**
	 * Return the ids of the obejcts filtered by type
	 * 
	 * @param ids
	 * @param type
	 * @return
	 */
	private List<String> filterIdsByType(List<String> ids, ObjectType type) {
		return ids.stream().filter(id -> ObjectHelper.getObjectType(URI.create(id)) == type)
				.collect(Collectors.toList());
	}

	/**
	 * True if at least one {@link Item} is locked by another {@link User}
	 *
	 * @param uris
	 * @param user
	 * @return
	 */
	protected boolean hasImageLocked(List<String> uris, User user) {
		for (final String uri : uris) {
			if (Locks.isLocked(uri.toString(), user.getEmail())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Part object to update status
	 * 
	 * @author saquet
	 *
	 */
	public class StatusPart implements Serializable {
		private static final long serialVersionUID = 7167344369483590830L;
		private final Date modified;
		private final String status;
		private final String id;
		private final String comment;

		public StatusPart(String id, Status status, Calendar modified, String comment) {
			this.modified = modified.getTime();
			this.status = status.name();
			this.id = id;
			this.comment = comment;
		}

		public Date getModified() {
			return modified;
		}

		public String getStatus() {
			return status;
		}

		public String getId() {
			return id;
		}

		public String getComment() {
			return comment;
		}
	}

	/**
	 * Part Object to update item license
	 * 
	 * @author saquet
	 *
	 */
	public class LicensePart implements Serializable {
		private static final long serialVersionUID = -8457457810566054732L;
		private final String id;
		private final String license;

		public LicensePart(String id, License license) {
			this.id = id;
			this.license = !StringHelper.isNullOrEmptyTrim(license.getName()) ? license.getName() : license.getUrl();
		}

		public String getLicense() {
			return license;
		}

		public String getId() {
			return id;
		}

	}

}
