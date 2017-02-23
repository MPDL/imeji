package de.mpg.imeji.logic.vo;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.io.FileUtils;
import org.joda.time.chrono.AssembledChronology.Fields;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLazyList;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.util.ObjectHelper;

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
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "item", namespace = "http://imeji.org/terms/")
public class Item extends Properties implements Serializable {
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

  /**
   * Default constructor
   */
  public Item() {

  }

  public Item(Item im) {
    setId(null);
    ObjectHelper.copyAllFields(im, this);
    // copyInFields(im);
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
   * Return the same Ite but empty (same id and same collection). Used for patch update
   *
   * @return
   */
  public Item copyEmpty() {
    final Item emptyItem = new Item();
    emptyItem.setId(this.getId());
    emptyItem.setCollection(collection);
    return emptyItem;
  }

  /**
   * Copy all {@link Fields} of an {@link Item} (including {@link Metadata}) to the current
   * {@link Item}
   *
   * @param copyFrom
   */
  protected void copyInFields(Item copyFrom) {
    final Class<? extends Item> copyFromClass = copyFrom.getClass();
    final Class<? extends Item> copyToClass = this.getClass();
    for (final Method methodFrom : copyFromClass.getDeclaredMethods()) {
      String setMethodName = null;
      if (methodFrom.getName().startsWith("get")) {
        setMethodName = "set" + methodFrom.getName().substring(3, methodFrom.getName().length());
      } else if (methodFrom.getName().startsWith("is")) {
        setMethodName = "set" + methodFrom.getName().substring(2, methodFrom.getName().length());
      }
      if (setMethodName != null) {
        try {
          final Method methodTo = copyToClass.getMethod(setMethodName, methodFrom.getReturnType());
          try {
            methodTo.invoke(this, methodFrom.invoke(copyFrom, (Object) null));
          } catch (final Exception e) {
            // LOGGER.error("Could not copy field from method: " +
            // methodFrom.getName(), e);
          }
        }
        // No setter, do nothing.
        catch (final NoSuchMethodException e) {
        }
      }
    }
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


}
