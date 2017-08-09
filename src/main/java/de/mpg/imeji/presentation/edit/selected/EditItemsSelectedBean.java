package de.mpg.imeji.presentation.edit.selected;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.authorization.Authorization;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.statement.StatementService;
import de.mpg.imeji.logic.statement.StatementUtil;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.StatementType;
import de.mpg.imeji.presentation.edit.EditMetadataAbstract;
import de.mpg.imeji.presentation.edit.SelectStatementComponent;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Bean for the page "Edit selected items metadata"
 *
 * @author saquet
 *
 */
@ManagedBean(name = "EditItemsSelectedBean")
@ViewScoped
public class EditItemsSelectedBean extends EditMetadataAbstract {
  private static final long serialVersionUID = -5474571536513587078L;
  private static final Logger LOGGER = Logger.getLogger(EditItemsSelectedBean.class);
  @ManagedProperty(value = "#{SessionBean.selected}")
  private List<String> selectedItemsIds = new ArrayList<>();
  private List<HeaderComponent> headers = new ArrayList<>();
  private List<RowComponent> rows = new ArrayList<>();
  private SelectStatementComponent newStatement;
  private List<String> displayedColumns = new ArrayList<>();
  private List<String> notAllowedItemNames = new ArrayList<>();

  public EditItemsSelectedBean() throws ImejiException {
    super();
    this.newStatement = new SelectStatementComponent(statementMap);
  }

  @PostConstruct
  public void init() {
    try {
      Authorization auth = new Authorization();
      List<Item> itemList = retrieveItems();
      notAllowedItemNames = itemList.stream().filter(i -> !auth.update(getSessionUser(), i))
          .map(i -> i.getFilename()).collect(Collectors.toList());
      itemList = itemList.stream().filter(i -> auth.update(getSessionUser(), i))
          .collect(Collectors.toList());
      if (!notAllowedItemNames.isEmpty()) {
        BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("edit_selected_not_allowed", getLocale())
            .replaceAll("XXX_NUMBER_XXX", notAllowedItemNames.size() + ""));
      }
      initHeaders(itemList);
      initRows(itemList);
    } catch (final ImejiException e) {
      BeanHelper.error("Error initialiting page:" + e.getCause());
      LOGGER.error("Error initializing bean", e);
    }
  }

  /**
   * Initialize the rows of the editor
   *
   * @param items
   */
  private void initRows(List<Item> items) {
    for (final Item item : items) {
      rows.add(new RowComponent(item, statementMap, headers));
    }
  }

  @Override
  public void save() {
    try {
      super.save();
      goBack();
      BeanHelper.addMessage(Imeji.RESOURCE_BUNDLE.getMessage("success_items_save", getLocale()));
    } catch (UnprocessableError e) {
      BeanHelper.error(e, getLocale());
    } catch (ImejiException e1) {
      LOGGER.error("Edit updating items", e1);
      BeanHelper.error(e1.getMessage());
    } catch (IOException e) {
      LOGGER.error("Error redirect after save", e);
    }
  }

  /**
   * Initialize the columns of the editor for this items
   */
  private void initHeaders(List<Item> items) {
    // Create a Map of the columns from the existing Metadata of the item
    final Map<String, HeaderComponent> headerMap =
        items.stream().flatMap(item -> item.getMetadata().stream())
            .filter(md -> md.getIndex().length() > 0).collect(Collectors.toMap(Metadata::getIndex,
                md -> new HeaderComponent(statementMap.get(md.getIndex())), (s1, s2) -> s1));

    // Add the default Statement to the columns
    headerMap.putAll(getDefaultStatements().values().stream().collect(
        Collectors.toMap(Statement::getIndex, st -> new HeaderComponent(st), (s1, s2) -> s1)));

    // Get the Column Map as a List sorted by index
    headers = headerMap.values().stream().sorted(
        (c1, c2) -> c1.getStatement().getIndex().compareToIgnoreCase(c2.getStatement().getIndex()))
        .collect(toList());

    // Add all Columns to the displayed columns
    displayedColumns = headers.stream().map(h -> h.getStatement().getIndex()).collect(toList());
  }

  @Override
  public List<Item> toItemList() {
    return rows.stream().map(RowComponent::toItem).collect(toList());
  }


  @Override
  public List<Statement> getAllStatements() {
    return rows.stream().flatMap(row -> row.getCells().stream())
        .filter(cell -> cell.getInputs() != null)
        .collect(
            Collectors.toMap(CellComponent::getIndex, cell -> cell.getStatement(), (a, b) -> a))
        .values().stream().collect(toList());
  }

  /**
   * Filter all statements which are already set into one column
   *
   * @return
   */
  public List<SelectItem> getFilteredStatementMenu() {
    return getStatementMenu().stream()
        .filter(s -> !headers.stream()
            .filter(h -> StatementUtil.indexEquals(h.getStatement().getIndex(), s.getLabel()))
            .findAny().isPresent())
        .collect(toList());
  }

  /**
   * Change the column name: <br/>
   * * Change the statement index of the column<br/>
   * * Change the Statement of all Entries of this column
   * 
   * @param position
   */
  public void changeColumnName(HeaderComponent header) {
    if (isValidStatementName(header.getInputName(), header.getStatement().getType())) {
      Statement newStatement = header.getStatement().clone();
      newStatement.setIndex(header.getInputName());
      // Change the statement name for all rows
      rows.stream().forEach(r -> r.changeStatement(header.getStatement().getIndex(), newStatement));
      // Change the statement of the header
      header.getStatement().setIndex(header.getInputName());
      // Remove the old name to the displayed statement
      displayedColumns.add(newStatement.getIndex());
      header.setEdit(false);
      header.setInvalidName(false);
    } else {
      header.setInvalidName(true);
    }
  }

  private boolean isValidStatementName(String newName, StatementType type) {
    try {
      Statement s = statementService.retrieveByIndex(newName, getSessionUser());
      return s.getType().equals(type);
    } catch (ImejiException e) {
      return true;
    }
  }

  /**
   * Add the Metadata defined in the column to all cell of this column
   *
   * @param column
   */
  public void addMetadataToAll(HeaderComponent header) {
    rows.stream().forEach(
        row -> row.addCell(header.getInput().getStatement(), header.getInput().getMetadata()));
  }

  /**
   * Add the Metadata defined in the column to all cell of this column
   *
   * @param column
   */
  public void addMetadataToColumn(int index) {
    rows.stream().forEach(
        row -> row.getCells().get(index).addValue(headers.get(index).getInput().getMetadata()));
    headers.get(index).setInput(null);
  }


  /**
   * Remove all metadata of one column
   *
   * @param column
   */
  public void clearColumn(int index) {
    rows.stream().forEach(row -> row.getCells().get(index).getInputs().clear());
  }

  /**
   * Add a column to the table
   */
  public void addColumn() {
    if (newStatement.getIndex() != null && !"".equals(newStatement.getIndex())) {
      Statement statement = newStatement.asStatement();
      headers.add(new HeaderComponent(statement));
      rows.stream().forEach(r -> r.addCell(statement));
      displayedColumns.add(statement.getIndex());
      newStatement = new SelectStatementComponent(statementMap);
    }
  }

  /**
   * Remove a column from the table
   *
   * @param index
   */
  public void removeColumn(String index) {
    if (!StringHelper.isNullOrEmptyTrim(index)) {
      rows.stream().forEach(row -> row.removeCell(index));
      headers = headers.stream().filter(h -> !index.equals(h.getStatement().getIndex()))
          .collect(Collectors.toList());
    }
  }

  public String getBackUrl() {
    return !StringHelper.isNullOrEmptyTrim(super.getBackUrl()) ? super.getBackUrl()
        : getPreviousPage().getCompleteUrlWithHistory();
  }

  /**
   * Retrieve the Items
   *
   * @return
   * @throws ImejiException
   */
  private List<Item> retrieveItems() throws ImejiException {
    return (List<Item>) itemService.retrieveBatch(selectedItemsIds, -1, 0, getSessionUser());
  }

  /**
   * @return the selectedItemsIds
   */
  public List<String> getSelectedItemsIds() {
    return selectedItemsIds;
  }

  /**
   * @param selectedItemsIds the selectedItemsIds to set
   */
  public void setSelectedItemsIds(List<String> selectedItemsIds) {
    this.selectedItemsIds = selectedItemsIds;
  }

  /**
   * @return the rows
   */
  public List<RowComponent> getRows() {
    return rows;
  }

  /**
   * @param rows the rows to set
   */
  public void setRows(List<RowComponent> rows) {
    this.rows = rows;
  }

  /**
   * @return the displayedColumns
   */
  public List<String> getDisplayedColumns() {
    return displayedColumns;
  }

  /**
   * @param displayedColumns the displayedColumns to set
   */
  public void setDisplayedColumns(List<String> displayedColumns) {
    this.displayedColumns = displayedColumns;
  }

  /**
   * @return the headers
   */
  public List<HeaderComponent> getHeaders() {
    return headers;
  }

  /**
   * @param headers the headers to set
   */
  public void setHeaders(List<HeaderComponent> headers) {
    this.headers = headers;
  }

  public void resetNewStatement() {
    this.newStatement = new SelectStatementComponent(statementMap);
  }

  /**
   * @return the newStatement
   */
  public SelectStatementComponent getNewStatement() {
    return newStatement;
  }

  /**
   * @param newStatement the newStatement to set
   */
  public void setNewStatement(SelectStatementComponent newStatement) {
    this.newStatement = newStatement;
  }

  public List<String> getNotAllowedItemNames() {
    return notAllowedItemNames;
  }

  public void setNotAllowedItemNames(List<String> notAllowedItemNames) {
    this.notAllowedItemNames = notAllowedItemNames;
  }


  public boolean isDefaultStatement(String index) {
    try {
      Statement s = new StatementService().retrieveByIndex(index, getSessionUser());
      return StatementUtil.toStatementUriList(Imeji.CONFIG.getStatements())
          .contains(s.getUri().toString());
    } catch (Exception e) {
      return false;
    }
  }


}
