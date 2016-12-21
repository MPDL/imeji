package de.mpg.imeji.presentation.metadata;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Edit the {@link Metadata} of a single {@link Item}
 * 
 * @author saquet
 *
 */
@ManagedBean(name = "EditMetadataItemBean")
@ViewScoped
public class EditMetadataItemBean extends EditMetadataBean {
  private static final long serialVersionUID = 4116466458089234630L;
  private static Logger LOGGER = Logger.getLogger(EditMetadataItemBean.class);

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

  /**
   * Return the current {@link ItemMetadataInputComponent} to be edited
   * 
   * @return
   */
  public ItemMetadataInputComponent getItem() {
    return getItems().get(0);
  }

}
