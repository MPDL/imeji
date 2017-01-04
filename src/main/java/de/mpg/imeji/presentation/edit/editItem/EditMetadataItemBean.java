package de.mpg.imeji.presentation.edit.editItem;

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
import de.mpg.imeji.presentation.edit.EditMetadataAbstract;
import de.mpg.imeji.presentation.edit.SelectStatementComponent;
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
  private List<RowComponent> rows = new ArrayList<>();
  private Item item;

  public EditMetadataItemBean() {
    super();
  }

  @PostConstruct
  public void init() {
    final String id = UrlHelper.getParameterValue("id");
    try {
      this.item = itemService.retrieve(ObjectHelper.getURI(Item.class, id), getSessionUser());
      for (final Metadata metadata : item.getMetadata()) {
        rows.add(new RowComponent(metadata, statementMap));
      }
    } catch (final ImejiException e) {
      BeanHelper.error("Error retrieving item");
      LOGGER.error("Error retrieving Item with id " + id, e);
    }
  }

  @Override
  public List<Item> toItemList() {
    final List<Item> itemList = new ArrayList<>();
    final List<Metadata> metadataList = new ArrayList<>();
    for (final RowComponent row : rows) {
      metadataList.add(row.getInput().getMetadata());
    }
    item.setMetadata(metadataList);
    itemList.add(item);
    return itemList;
  }

  @Override
  public List<SelectStatementComponent> getAllStatements() {
    final List<SelectStatementComponent> l = new ArrayList<>();
    for (final RowComponent row : rows) {
      l.add(row);
    }
    return l;
  }

  /**
   * Add a new empty row
   */
  public void addMetadata() {
    rows.add(new RowComponent(statementMap));
  }

  /**
   * Remove a metadata
   *
   * @param index
   */
  public void removeMetadata(int index) {
    rows.remove(index);
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

}
