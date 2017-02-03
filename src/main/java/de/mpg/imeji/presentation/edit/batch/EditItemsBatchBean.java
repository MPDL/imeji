package de.mpg.imeji.presentation.edit.batch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
import de.mpg.imeji.logic.vo.Metadata;
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
  private SelectStatementWithInputComponent statementSelector;
  private List<Item> items = new ArrayList<>();

  @PostConstruct
  public void init() {
    collectionId = UrlHelper.getParameterValue("col");
    statementSelector = new SelectStatementWithInputComponent(statementMap);
  }


  /**
   * Append a new metadata to all items
   */
  public void append() {
    try {
      retrieveItems();
      items.stream()
          .forEach(item -> item.getMetadata().add(statementSelector.getInput().getMetadata()));
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
      items = retrieveItems();
      Metadata metadata = statementSelector.getInput().getMetadata();
      System.out
          .println("Filling: " + metadata.getStatementId() + " with value " + metadata.getText());
      items.stream()
          .filter(item -> item.getMetadata().stream()
              .noneMatch(md -> md.getStatementId().equals(statementSelector.getIndex())))
          .sequential().forEach(item -> item.getMetadata().add(metadata));
      System.out.println("result");
      for (Item item : items) {
        System.out.println("Item:" + item.getFilename());
        for (Metadata m : item.getMetadata()) {
          System.out.println(m.getUri() + "-" + m.getStatementId() + "-" + m.getText());
        }
      }
      save();
      init();
    } catch (Exception e) {
      BeanHelper.error("Error saving editor: " + e.getMessage());
      LOGGER.error("Error saving batch editor");
    }
  }

  private boolean hasStatement(Item item, Statement s) {
    return item.getMetadata().stream().anyMatch(md -> md.getStatementId().equals(s.getIndex()));
  }

  /**
   * Overwrite all metadata for the current statement
   */
  public void overwrite() {
    try {
      retrieveItems();
      items.stream()
          .peek(item -> item.getMetadata().stream()
              .filter(md -> !md.getStatementId().equals(statementSelector.getIndex()))
              .collect(Collectors.toList()))
          .forEach(item -> item.getMetadata().add(statementSelector.getInput().getMetadata()));
      save();
    } catch (Exception e) {
      BeanHelper.error("Error saving editor: " + e.getMessage());
      LOGGER.error("Error saving batch editor");
    }
  }


  private List<Item> retrieveItems() throws ImejiException, IOException {
    SearchQuery q = SearchQueryParser.parseStringQuery(query);
    if (collectionId != null) {
      return itemService.searchAndRetrieve(ObjectHelper.getURI(CollectionImeji.class, collectionId),
          q, null, getSessionUser(), 0, -1);
    } else {
      return itemService.searchAndRetrieve(q, null, getSessionUser(), -1, 0);
    }
  }

  @Override
  public List<Item> toItemList() {
    return items;
  }

  @Override
  public List<Statement> getAllStatements() {
    return Arrays.asList(statementSelector.getInput().getStatement());
  }


  /**
   * @return the statementSelector
   */
  public SelectStatementWithInputComponent getStatementSelector() {
    return statementSelector;
  }

  /**
   * @param statementSelector the statementSelector to set
   */
  public void setStatementSelector(SelectStatementWithInputComponent statementSelector) {
    this.statementSelector = statementSelector;
  }


}
