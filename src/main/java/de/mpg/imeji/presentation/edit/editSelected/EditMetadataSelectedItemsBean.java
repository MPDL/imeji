package de.mpg.imeji.presentation.edit.editSelected;

import java.util.ArrayList;
import java.util.HashMap;
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
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.presentation.edit.EditMetadataAbstract;
import de.mpg.imeji.presentation.edit.SelectStatementWithInputComponent;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Bean for the page "Edit selected items metadata"
 *
 * @author saquet
 *
 */
@ManagedBean(name = "EditMetadataSelectedItemsBean")
@ViewScoped
public class EditMetadataSelectedItemsBean extends EditMetadataAbstract {
  private static final long serialVersionUID = -5474571536513587078L;
  private static final Logger LOGGER = Logger.getLogger(EditMetadataSelectedItemsBean.class);
  @ManagedProperty(value = "#{SessionBean.selected}")
  private List<String> selectedItemsIds = new ArrayList<>();
  private List<SelectStatementWithInputComponent> columns = new ArrayList<>();
  private List<RowComponent> rows = new ArrayList<>();
  private SelectStatementWithInputComponent newStatement;
  private List<String> displayedColumns = new ArrayList<>();

  public EditMetadataSelectedItemsBean() {
    super();
    this.newStatement = new SelectStatementWithInputComponent(statementMap);
  }

  @PostConstruct
  public void init() {
    try {
      final List<Item> itemList = retrieveItems();
      initColumns(itemList);
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
      rows.add(new RowComponent(item, statementMap, columns));
    }
  }

  /**
   * Initialize the columns of the editor
   */
  private void initColumns(List<Item> items) {
    final Map<String, SelectStatementWithInputComponent> map = new HashMap<>();
    for (final Item item : items) {
      for (final Metadata md : item.getMetadata()) {
        map.putIfAbsent(md.getStatementId(),
            new SelectStatementWithInputComponent(md.getStatementId(), statementMap));
      }
    }
    columns = new ArrayList<>(map.values());
    displayedColumns = columns.stream().map(SelectStatementWithInputComponent::getIndex)
        .collect(Collectors.toList());

  }

  @Override
  public List<Item> toItemList() {
    return rows.stream().map(RowComponent::toItem).collect(Collectors.toList());
  }

  @Override
  public List<SelectStatementWithInputComponent> getAllStatements() {
    return columns;
  }

  /**
   * Filter all statements which are already set into one column
   * 
   * @return
   */
  public List<SelectItem> getFilteredStatementMenu() {
    return getStatementMenu().stream().filter(s -> !displayedColumns.contains(s.getLabel()))
        .collect(Collectors.toList());
  }

  /**
   * Add the Metadata defined in the column to all cell of this column
   * 
   * @param column
   */
  public void addMetadataToAll(SelectStatementWithInputComponent column) {
    rows.stream().forEach(
        row -> row.addCell(column.getInput().getStatement(), column.getInput().getMetadata()));
  }

  /**
   * Add the Metadata defined in the column to all cell of this column
   * 
   * @param column
   */
  public void addMetadataToColumn(int index) {
    rows.stream().forEach(
        row -> row.getCells().get(index).addValue(columns.get(index).getInput().getMetadata()));
    columns.get(index).setInput(null);
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
    newStatement.setInput(null);
    columns.add(newStatement);
    for (final RowComponent row : rows) {
      row.addCell(newStatement.asStatement());
    }
    displayedColumns.add(newStatement.getIndex());
    newStatement = new SelectStatementWithInputComponent(statementMap);
  }

  /**
   * Remove a column from the table
   * 
   * @param index
   */
  public void removeColumn(String index) {
    if (!StringHelper.isNullOrEmptyTrim(index)) {
      rows.stream().forEach(row -> row.removeCell(index));
      columns = columns.stream().filter(column -> !index.equals(column.getIndex()))
          .collect(Collectors.toList());
    }
  }

  public String getBackUrl() {
    return getHistory().getPreviousPage().getCompleteUrlWithHistory();
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
   * @param newStatement the newStatement to set
   */
  public void setNewStatement(SelectStatementWithInputComponent newStatement) {
    this.newStatement = newStatement;
  }

  public SelectStatementWithInputComponent getNewStatement() {
    return newStatement;
  }

  /**
   * @return the columns
   */
  public List<SelectStatementWithInputComponent> getColumns() {
    return columns;
  }

  /**
   * @param columns the columns to set
   */
  public void setColumns(List<SelectStatementWithInputComponent> columns) {
    this.columns = columns;
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

}
