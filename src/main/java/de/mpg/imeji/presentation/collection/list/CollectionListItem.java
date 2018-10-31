package de.mpg.imeji.presentation.collection.list;

import java.io.Serializable;
import java.net.URI;

import javax.faces.event.ValueChangeEvent;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Person;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.presentation.util.CommonUtils;

/**
 * Item of the collections page.
 *
 * @author saquet
 */
public class CollectionListItem implements Serializable {
	private static final long serialVersionUID = -782035871566935720L;
	private static final Logger LOGGER = LogManager.getLogger(CollectionListItem.class);
	private String title = "";
	private String description = "";
	private String descriptionFull = "";
	private String authors = "";
	private int size = 0;
	private String status = Status.PENDING.toString();
	private String id = null;
	private URI uri = null;
	private String discardComment = "";
	private String lastModificationDate = null;
	private String selectedGrant;
	private boolean isOwner = false;
	private CollectionImeji collection;
	private String logoUrl;
	private final String fileUrl;
	/**
	 * Maximum number of character displayed in the list for the description
	 */
	private static final int DESCRIPTION_MAX_SIZE = 330; // 430

	/**
	 * Construct a new {@link CollectionListItem} with a {@link CollectionImeji}
	 *
	 * @param collection
	 * @param user
	 */
	public CollectionListItem(CollectionImeji collection, User user, String fileUrl) {
		this.fileUrl = fileUrl;
		try {
			this.collection = collection;
			title = collection.getTitle();
			description = CommonUtils.removeTags(collection.getDescription());
			descriptionFull = description;
			if (description != null && description.length() > DESCRIPTION_MAX_SIZE) {
				description = description.substring(0, DESCRIPTION_MAX_SIZE) + "...";
			}
			for (final Person p : collection.getPersons()) {
				if (!"".equals(authors)) {
					authors += "; ";
				}
				authors += p.getFamilyName() + ", " + p.getGivenName();
			}
			uri = collection.getId();
			setId(ObjectHelper.getId(uri));
			status = collection.getStatus().toString();
			discardComment = collection.getDiscardComment();
			// creationDate = collection.getCreated().getTime().toString();
			lastModificationDate = collection.getModified().getTime().toString();
			// initializations
			final SearchResult result = searchFirstCollectionItem(collection, user);
			size = result.getNumberOfItems();
			initLogo(collection, result);
			if (user != null) {
				isOwner = collection.getCreatedBy().equals(user.getId());
			}
		} catch (final Exception e) {
			LOGGER.error("Error creating collectionListItem", e);
		}
	}

	/**
	 * Find the Logo of the collection. If no logo defined, use the first file of
	 * the collection
	 *
	 * @param collection
	 * @param user
	 * @throws ImejiException
	 * @throws Exception
	 */
	private void initLogo(CollectionImeji collection, SearchResult result) throws ImejiException, Exception {
		if (collection.getLogoUrl() != null) {
			this.logoUrl = collection.getLogoUrl().toString();
		} else if (result.getNumberOfItems() > 0) {
			logoUrl = buildContentUrl(result.getResults().get(0));
		}
	}

	/**
	 * Build the content Url for this item
	 * 
	 * @param itemUri
	 * @return
	 */
	private String buildContentUrl(String itemUri) {
		final String itemId = ObjectHelper.getId(URI.create(itemUri));
		return fileUrl + "?item=" + itemId + "&resolution=thumbnail";
	}

	private SearchResult searchFirstCollectionItem(CollectionImeji collection, User user) throws UnprocessableError {
		return new ItemService().search(collection.getId(),
				new SearchFactory().and(new SearchPair(SearchFields.filename, "*")).build(), null, Imeji.adminUser, 1,
				0);
	}

	/**
	 * Listener for the discard comment
	 *
	 * @param event
	 */
	public void discardCommentListener(ValueChangeEvent event) {
		if (event.getNewValue() != null && event.getNewValue().toString().trim().length() > 0) {
			setDiscardComment(event.getNewValue().toString().trim());
		}
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAuthors() {
		return authors;
	}

	public void setAuthors(String authors) {
		this.authors = authors;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDiscardComment() {
		return discardComment;
	}

	public void setDiscardComment(String discardComment) {
		this.discardComment = discardComment;
	}

	public String getLastModificationDate() {
		return lastModificationDate;
	}

	public void setLastModificationDate(String lastModificationDate) {
		this.lastModificationDate = lastModificationDate;
	}

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public String getDescriptionFull() {
		return descriptionFull;
	}

	public void setDescriptionFull(String descriptionFull) {
		this.descriptionFull = descriptionFull;
	}

	public String getSelectedGrant() {
		return selectedGrant;
	}

	public void setSelectedGrant(String selectedGrant) {
		this.selectedGrant = selectedGrant;
	}

	public boolean isOwner() {
		return isOwner;
	}

	public void setOwner(boolean isOwner) {
		this.isOwner = isOwner;
	}

	public CollectionImeji getCollection() {
		return collection;
	}

	public String getLogoUrl() {
		return logoUrl;
	}
}
