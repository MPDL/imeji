package de.mpg.imeji.presentation.metadata.editItem;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.presentation.metadata.EditMetadataAbstract;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Edit the {@link Metadata} of a single {@link Item}
 * 
 * @author saquet
 *
 */
@ManagedBean(name = "EditMetadataItemBean")
@ViewScoped
public class EditMetadataItemBean extends EditMetadataAbstract {
  private static final long serialVersionUID = 4116466458089234630L;
  private static Logger LOGGER = Logger.getLogger(EditMetadataItemBean.class);
  private List<ItemMetadataInputComponent> items = new ArrayList<>();

  public EditMetadataItemBean() {
    super();
  }

  @PostConstruct
  public void init() {
    String id = UrlHelper.getParameterValue("id");
    try {
      Item item = itemService.retrieve(ObjectHelper.getURI(Item.class, id), getSessionUser());
      getItems().add(new ItemMetadataInputComponent(item, statementMap));
    } catch (ImejiException e) {
      BeanHelper.error("Error retrieving item");
      LOGGER.error("Error retrieving Item with id " + id, e);
    }
  }

  @Override
  public List<Item> toItemList() {
    List<Item> itemList = new ArrayList<>();
    for (ItemMetadataInputComponent component : items) {
      itemList.add(component.toItem());
    }
    return itemList;
  }


  /**
   * @return the items
   */
  public List<ItemMetadataInputComponent> getItems() {
    return items;
  }

  /**
   * @param items the items to set
   */
  public void setItems(List<ItemMetadataInputComponent> items) {
    this.items = items;
  }

  /**
   * Return the current {@link ItemMetadataInputComponent} to be edited
   * 
   * @return
   */
  public ItemMetadataInputComponent getItem() {
    return getItems().get(0);
  }

}
