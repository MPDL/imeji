package de.mpg.imeji.presentation.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.service.item.ItemService;
import de.mpg.imeji.logic.service.statement.StatementService;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.metadata.editItem.ItemMetadataInputComponent;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Bean to edit {@link Metadata} for a list of {@link Item}
 * 
 * @author saquet
 *
 */
public abstract class EditMetadataAbstract extends SuperBean {
  private static final long serialVersionUID = -8870761990852602492L;
  private static final Logger LOGGER = Logger.getLogger(EditItemMetadataBean.class);
  protected ItemService itemService = new ItemService();
  private List<SelectItem> statementMenu = new ArrayList<>();
  protected Map<String, Statement> statementMap = new HashMap<>();


  public EditMetadataAbstract() {
    StatementService statementService = new StatementService();
    try {
      for (Statement s : statementService.searchAndRetrieve(null, null, getSessionUser(), -1, 0)) {
        statementMenu.add(new SelectItem(s.getIndex()));
        statementMap.put(s.getIndex(), s);
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
  public abstract List<Item> toItemList();



  public List<SelectItem> getStatementMenu() {
    return statementMenu;
  }
}
