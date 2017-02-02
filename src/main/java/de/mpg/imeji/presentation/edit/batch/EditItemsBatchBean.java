package de.mpg.imeji.presentation.edit.batch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.presentation.edit.EditMetadataAbstract;
import de.mpg.imeji.presentation.edit.SelectStatementWithInputComponent;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * JSF Bean for the Edit batch page
 * 
 * @author saquet
 *
 */
@ManagedBean(name = "EditItemsBatchBean")
@ViewScoped
public class EditItemsBatchBean extends EditMetadataAbstract {
  private static final long serialVersionUID = -288843834798781232L;
  private static final Logger LOGGER = Logger.getLogger(EditItemsBatchBean.class);
  private String collectionId;
  private String query;
  private SelectStatementWithInputComponent input;
  private List<Item> items = new ArrayList<>();

  @PostConstruct
  public void init() {
    collectionId = UrlHelper.getParameterValue("col");
    query = UrlHelper.getParameterValue("q") == null ? "" : UrlHelper.getParameterValue("q");
    setInput(new SelectStatementWithInputComponent(statementMap));
  }


  /**
   * Append a new metadata to all items
   */
  public void append() {
    try {
      retrieveItems();
      save();
    } catch (Exception e) {
      BeanHelper.error("Error saving editor: " + e.getMessage());
      LOGGER.error("Error saving batch editor");
    }
  }

  /**
   * Add the metadata to items which don't have any value for this statement
   */
  public void fill() {
    try {
      retrieveItems();
      save();
    } catch (Exception e) {
      BeanHelper.error("Error saving editor: " + e.getMessage());
      LOGGER.error("Error saving batch editor");
    }
  }

  /**
   * Overwrite all metadata for the current statement
   */
  public void overwrite() {
    try {
      retrieveItems();
      save();
    } catch (Exception e) {
      BeanHelper.error("Error saving editor: " + e.getMessage());
      LOGGER.error("Error saving batch editor");
    }
  }

  private void retrieveItems() throws ImejiException, IOException {
    SearchQuery q = SearchQueryParser.parseStringQuery(query);
    if (collectionId != null) {
      items =
          itemService.searchAndRetrieve(ObjectHelper.getURI(CollectionImeji.class, collectionId), q,
              null, getSessionUser(), 0, -1);
    } else {
      items = itemService.searchAndRetrieve(q, null, getSessionUser(), -1, 0);
    }
  }

  @Override
  public List<Item> toItemList() {
    return items;
  }

  @Override
  public List<Statement> getAllStatements() {
    return Arrays.asList(input.getInput().getStatement());
  }

  /**
   * @return the input
   */
  public SelectStatementWithInputComponent getInput() {
    return input;
  }

  /**
   * @param input the input to set
   */
  public void setInput(SelectStatementWithInputComponent input) {
    this.input = input;
  }

}
