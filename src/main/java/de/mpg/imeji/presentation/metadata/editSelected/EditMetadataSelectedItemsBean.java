package de.mpg.imeji.presentation.metadata.editSelected;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.StatementType;
import de.mpg.imeji.logic.vo.factory.StatementFactory;
import de.mpg.imeji.presentation.metadata.EditMetadataAbstract;
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
  private List<String> columns = new ArrayList<>();
  private List<RowComponent> rows = new ArrayList<>();
  private String newStatementIndex;

  public EditMetadataSelectedItemsBean() {
    super();
  }

  @PostConstruct
  public void init() {
    try {
      List<Item> itemList = retrieveItems();
      initColumns(itemList);
      initRows(itemList);
    } catch (ImejiException e) {
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
    for (Item item : items) {
      rows.add(new RowComponent(item, statementMap, columns));
    }
  }

  /**
   * Initialize the columns of the editor
   */
  private void initColumns(List<Item> items) {
    for (Item item : items) {
      for (Metadata md : item.getMetadata()) {
        if (!columns.contains(md.getStatementId())) {
          columns.add(md.getStatementId());
        }
      }
    }
  }

  @Override
  public List<Item> toItemList() {
    List<Item> l = new ArrayList<>();
    for (RowComponent row : rows) {
      l.add(row.toItem());
    }
    return l;
  }

  /**
   * Add a column to the table
   */
  public void addColumn() {
    Statement statement = getNewStatement();
    columns.add(newStatementIndex);
    for (RowComponent row : rows) {
      row.addCell(statement);
    }
    newStatementIndex = null;
  }

  /**
   * Return the new {@link Statement} according to the newStatementIndex. If the statement doens't
   * exist, create a new one
   * 
   * @return
   */
  private Statement getNewStatement() {
    Statement statement = statementMap.get(newStatementIndex);
    if (statement == null) {
      statement = new StatementFactory().addName(newStatementIndex).setIndex(newStatementIndex)
          .setType(StatementType.TEXT).build();
    }
    return statement;
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
   * @return the columns
   */
  public List<String> getColumns() {
    return columns;
  }

  /**
   * @param columns the columns to set
   */
  public void setColumns(List<String> columns) {
    this.columns = columns;
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
   * @return the newStatement
   */
  public String getNewStatementIndex() {
    return newStatementIndex;
  }

  /**
   * @param newStatement the newStatement to set
   */
  public void setNewStatementIndex(String index) {
    this.newStatementIndex = index;
  }

}
