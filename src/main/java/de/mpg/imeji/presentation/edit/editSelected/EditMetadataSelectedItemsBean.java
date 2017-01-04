package de.mpg.imeji.presentation.edit.editSelected;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.presentation.edit.EditMetadataAbstract;
import de.mpg.imeji.presentation.edit.SelectStatementComponent;
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
  private List<SelectStatementComponent> columns = new ArrayList<>();
  private List<RowComponent> rows = new ArrayList<>();
  private SelectStatementComponent newStatement;

  public EditMetadataSelectedItemsBean() {
    super();
    this.newStatement = new SelectStatementComponent(statementMap);
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
    final Map<String, SelectStatementComponent> map = new HashMap<>();
    for (final Item item : items) {
      for (final Metadata md : item.getMetadata()) {
        map.putIfAbsent(md.getStatementId(),
            new SelectStatementComponent(md.getStatementId(), statementMap));
      }
    }
    columns = new ArrayList<>(map.values());
  }

  @Override
  public List<Item> toItemList() {
    final List<Item> l = new ArrayList<>();
    for (final RowComponent row : rows) {
      l.add(row.toItem());
    }
    return l;
  }

  @Override
  public List<SelectStatementComponent> getAllStatements() {
    return columns;
  }


  /**
   * Add a column to the table
   */
  public void addColumn() {
    columns.add(newStatement);
    for (final RowComponent row : rows) {
      row.addCell(newStatement.asStatement());
    }
    newStatement = new SelectStatementComponent(statementMap);
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
  public void setNewStatement(SelectStatementComponent newStatement) {
    this.newStatement = newStatement;
  }

  public SelectStatementComponent getNewStatement() {
    return newStatement;
  }

  /**
   * @return the columns
   */
  public List<SelectStatementComponent> getColumns() {
    return columns;
  }

  /**
   * @param columns the columns to set
   */
  public void setColumns(List<SelectStatementComponent> columns) {
    this.columns = columns;
  }

}
