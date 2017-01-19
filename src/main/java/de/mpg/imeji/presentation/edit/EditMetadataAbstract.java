package de.mpg.imeji.presentation.edit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.statement.StatementService;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Bean to edit {@link Metadata} for a list of {@link Item}
 *
 * @author saquet
 *
 */
public abstract class EditMetadataAbstract extends SuperBean {
  private static final long serialVersionUID = -8870761990852602492L;
  private static final Logger LOGGER = Logger.getLogger(EditMetadataAbstract.class);
  protected ItemService itemService = new ItemService();
  protected StatementService statementService = new StatementService();
  private final List<SelectItem> statementMenu = new ArrayList<>();
  protected Map<String, Statement> statementMap = new HashMap<>();

  public EditMetadataAbstract() {
    final StatementService statementService = new StatementService();
    try {
      for (final Statement s : statementService.searchAndRetrieve(null, null, getSessionUser(), -1,
          0)) {
        statementMenu.add(new SelectItem(s.getIndex()));
        statementMap.put(s.getIndex(), s);
      }
    } catch (final ImejiException e) {
      BeanHelper.error("Error retrieving statements");
      LOGGER.error("Error retrieving statements", e);
    }
  }

  /**
   * Save the items
   */
  public void save() {
    try {
      statementService.createBatch(getNewStatements(), getSessionUser());
      itemService.updateBatch(toItemList(), getSessionUser());
      BeanHelper.addMessage(Imeji.RESOURCE_BUNDLE.getMessage("success_items_save", getLocale()));
    } catch (final ImejiException e) {
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

  /**
   * Return all Statement used by all items as a {@link SelectStatementComponent} list
   *
   * @return
   */
  public abstract List<SelectStatementWithInputComponent> getAllStatements();

  /**
   * Return all statements which are not already existing
   *
   * @return
   */
  private List<Statement> getNewStatements() {
    final List<Statement> newStatements = new ArrayList<>();
    for (final SelectStatementComponent component : getAllStatements()) {
      if (!component.isExists()) {
        newStatements.add(component.asStatement());
      }
    }
    return newStatements;
  }


  public List<SelectItem> getStatementMenu() {
    return statementMenu;
  }
}
