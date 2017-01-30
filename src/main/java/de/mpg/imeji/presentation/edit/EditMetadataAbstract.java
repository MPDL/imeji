package de.mpg.imeji.presentation.edit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.collection.CollectionService;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.statement.StatementService;
import de.mpg.imeji.logic.vo.CollectionImeji;
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
  private List<SelectItem> statementMenu = new ArrayList<>();
  protected Map<String, Statement> statementMap = new HashMap<>();

  public EditMetadataAbstract() {
    final StatementService statementService = new StatementService();
    try {
      statementMap = statementListToMap(
          statementService.searchAndRetrieve(null, null, getSessionUser(), -1, 0));
      statementMenu =
          statementMap.keySet().stream().map(s -> new SelectItem(s)).collect(Collectors.toList());
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
   * Return the default Statement for the current items
   * 
   * @return
   */
  protected void addDefaultStatements(Map<String, Statement> statementMap) {
    try {
      // add from config
      statementMap.putAll(statementListToMap(retrieveInstanceDefaultStatements()));
      // add from item collections
      for (CollectionImeji c : getItemsCollections()) {
        statementMap.putAll(getStatementMapForCollection(c));
      }
    } catch (Exception e) {
      LOGGER.error("Error adding default statement to editor", e);
    }
  }

  /**
   * Retrieve the default statements defined for the whole instance
   * 
   * @return
   * @throws ImejiException
   */
  private List<Statement> retrieveInstanceDefaultStatements() throws ImejiException {
    return statementService.retrieveBatch(Arrays.asList(Imeji.CONFIG.getStatements().split(",")),
        getSessionUser());
  }

  /**
   * Return the statement map for one collection as defined as default statement for this collection
   * 
   * @param c
   * @return
   * @throws ImejiException
   */
  protected Map<String, Statement> getStatementMapForCollection(CollectionImeji c)
      throws ImejiException {
    if (c.getStatements() == null) {
      return new HashMap<>();
    }
    return statementListToMap(statementService
        .retrieveBatch(Arrays.asList(c.getStatements().split(",")), getSessionUser()));
  }

  /**
   * Transform a list of statement to a map of statement with its id as key
   * 
   * @param l
   * @return
   */
  private Map<String, Statement> statementListToMap(List<Statement> l) {
    return l.stream().collect(Collectors.toMap(Statement::getId, Function.identity()));
  }

  /**
   * Retrieve all the collections of the c
   * 
   * @return
   * @throws ImejiException
   */
  protected List<CollectionImeji> getItemsCollections() throws ImejiException {
    Set<String> colIds = new HashSet<>();
    for (Item item : toItemList()) {
      colIds.add(item.getCollection().toString());
    }
    return new CollectionService().retrieve(colIds.stream().collect(Collectors.toList()),
        getSessionUser());
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
