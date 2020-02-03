package de.mpg.imeji.logic.model;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLazyList;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.model.aspects.AccessMember;
import de.mpg.imeji.logic.model.aspects.ChangeMember;
import de.mpg.imeji.logic.model.aspects.ChangeMember.ActionType;
import de.mpg.imeji.logic.model.aspects.CloneURI;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.ObjectHelper.ObjectType;

/**
 * imeji item. Can be an image, a video, a sound, etc.
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource("http://imeji.org/terms/item")
@j2jModel("item")
@j2jId(getMethod = "getId", setMethod = "setId")
public class Item extends Properties implements Serializable, CollectionElement, CloneURI, AccessMember {
  private static final long serialVersionUID = 3989965275269803885L;
  @j2jResource("http://imeji.org/terms/collection")
  private URI collection;
  @j2jLiteral("http://imeji.org/terms/filename")
  private String filename;
  @j2jLiteral("http://imeji.org/terms/filetype")
  private String filetype;
  @j2jLiteral("http://imeji.org/terms/fileSize")
  private long fileSize;
  @j2jLazyList("http://imeji.org/terms/license")
  private List<License> licenses = new ArrayList<>();
  @j2jLazyList("http://imeji.org/terms/metadata")
  private List<Metadata> metadata = new ArrayList<>();

  private static final Logger LOGGER = LogManager.getLogger(Item.class);


  /**
   * Default constructor
   */
  public Item() {

  }

  public Item(Item im) {
    setId(null);
    ObjectHelper.copyAllFields(im, this);
  }


  @Override
  public Item cloneURI() {
    Item newItem = new Item();
    newItem.setId(this.getId());
    return newItem;
  }


  @Override
  public void accessMember(ChangeMember changeMember) {

    // member field that can be set unsynchronized: 
    // (a) add a license to the license list if it is empty [in release]
    // (b) set new (parent) collection

    super.accessMember(changeMember);

    Field fieldToSet = changeMember.getField();
    Object valueToSet = changeMember.getValue();
    ActionType action = changeMember.getAction();
    try {
      Field licensesField = Item.class.getDeclaredField("licenses");
      Field parentCollectionField = Item.class.getDeclaredField("collection");
      // (a) licenses
      if (fieldToSet.equals(licensesField) && this.licenses.isEmpty() && valueToSet instanceof License && action.equals(ActionType.ADD)) {
        this.licenses.add((License) valueToSet);
      } else if (fieldToSet.equals(parentCollectionField) && action.equals(ActionType.EDIT) && valueToSet instanceof URI) {
        this.collection = (URI) valueToSet;
      } else {
        LOGGER.debug("Did not edit member in Item. Please check and implement your case.");
      }
    } catch (NoSuchFieldException | SecurityException e) {
      LOGGER.error("Did not edit member in Item. Member does not exist", e);
    }
  }


  public void setCollection(URI collection) {
    this.collection = collection;
  }

  public URI getCollection() {
    return collection;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getFilename() {
    return filename;
  }

  public void setFiletype(String filetype) {
    this.filetype = filetype;
  }

  public String getFiletype() {
    return filetype;
  }

  /**
   *
   * @return
   */
  public long getFileSize() {
    return fileSize;
  }

  /**
   *
   * @return human readable file size
   */
  public String getFileSizeHumanReadable() {
    return FileUtils.byteCountToDisplaySize(fileSize);
  }

  /**
   *
   * @param fileSize
   */
  public void setFileSize(long fileSize) {
    this.fileSize = fileSize;
  }

  public List<License> getLicenses() {
    return licenses;
  }

  public void setLicenses(List<License> licenses) {
    this.licenses = licenses;
  }

  public List<Metadata> getMetadata() {
    return metadata;
  }

  public void setMetadata(List<Metadata> metadata) {
    this.metadata = metadata;
  }

  @Override
  public String getName() {
    return filename;
  }

  @Override
  public String getUri() {
    return getId().toString();
  }

  @Override
  public ObjectType getType() {
    return ObjectType.ITEM;
  }



}
