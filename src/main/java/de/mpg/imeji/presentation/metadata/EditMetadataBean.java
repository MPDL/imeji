package de.mpg.imeji.presentation.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.service.item.ItemService;
import de.mpg.imeji.logic.service.statement.StatementService;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Bean to edit {@link Metadata} for a list of {@link Item}
 * 
 * @author saquet
 *
 */
@ManagedBean(name = "EditMetadataBean")
@ViewScoped
public class EditMetadataBean extends SuperBean {
  private static final long serialVersionUID = -8870761990852602492L;
  private static final Logger LOGGER = Logger.getLogger(EditItemMetadataBean.class);
  private List<ItemMetadataInputComponent> items = new ArrayList<>();
  protected ItemService itemService = new ItemService();
  private List<SelectItem> statementMenu = new ArrayList<>();
  protected Map<String, Statement> statementMap = new HashMap<>();
  private Statement statement;
  private Metadata metadata;

  public EditMetadataBean() {
    StatementService statementService = new StatementService();
    try {
      for (Statement s : statementService.searchAndRetrieve(null, null, getSessionUser(), -1, 0)) {
        if (statement == null) {
          statement = s;
          metadata = ImejiFactory.newMetadata(statement).build();
        }
        statementMenu.add(new SelectItem(s.getId(), s.getIndex()));
        statementMap.put(statement.getId(), statement);
      }
    } catch (ImejiException e) {
      BeanHelper.error("Error retrieving statements");
      LOGGER.error("Error retrieving statements", e);
    }
  }

  /**
   * Save the items
   */
  public void save() {
    try {
      itemService.updateBatch(toItemList(), getSessionUser());
    } catch (ImejiException e) {
      BeanHelper.error("Error editing metadata");
      LOGGER.error("Edit updating items", e);
    }
  }

  /**
   * Convert the List of {@link ItemMetadataInputComponent} to a list of {@link Item}
   * 
   * @return
   */
  private List<Item> toItemList() {
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

  public List<SelectItem> getStatementMenu() {
    return statementMenu;
  }

  /**
   * @return the statement
   */
  public Statement getStatement() {
    return statement;
  }

  /**
   * @param statement the statement to set
   */
  public void setStatement(Statement statement) {
    this.statement = statement;
  }

  /**
   * @return the metadata
   */
  public Metadata getMetadata() {
    return metadata;
  }

  /**
   * @param metadata the metadata to set
   */
  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }
}
