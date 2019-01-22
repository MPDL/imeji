package de.mpg.imeji.presentation.search.advanced.group;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.model.SelectItem;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.Metadata;
import de.mpg.imeji.logic.model.SearchMetadataFields;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.factory.MetadataFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.search.model.SearchMetadata;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.presentation.item.edit.MetadataInputComponent;

/**
 * An Entry of the {@link MetadataSearchGroup}
 * 
 * @author saquet
 *
 */
public class MetadataSearchGroupEntry implements Serializable {
  private static final long serialVersionUID = 8935087888435844900L;
  private String statementIndex;
  private final Map<String, Statement> statements;
  private Metadata metadata;
  private String distance;
  private SearchOperators operator;
  private Statement statement;
  private boolean not = false;
  private LOGICAL_RELATIONS logicalRelation = LOGICAL_RELATIONS.OR;
  private List<SelectItem> operatorMenu;
  private List<SelectItem> distanceMenu;
  private MetadataInputComponent input;

  /**
   * 
   * @param statements
   * @param locale
   */
  public MetadataSearchGroupEntry(Statement statement, final Map<String, Statement> statements, Locale locale) {
    this.statements = statements;
    this.statement = statement;
    this.metadata = new MetadataFactory().setStatementId(statement.getIndex()).build();
    this.setInput(new MetadataInputComponent(metadata, statement));
    initOperatorMenu(locale);
  }

  /**
   * Return the current entry as SearchGroup
   * 
   * @return
   * @throws UnprocessableError
   */
  public SearchElement getSearchElement() throws UnprocessableError {
    List<SearchElement> els = getAllSearchElements();
    if (els.size() == 1) {
      return els.get(0);
    } else {
      return new SearchFactory().and(getAllSearchElements()).buildAsGroup();
    }
  }

  /**
   * Change the statement in the select statement menu
   */
  public void changeStatement() {
    statement = statements.get(statementIndex);
    this.metadata = new MetadataFactory().setStatementId(statement.getIndex()).build();
    this.setInput(new MetadataInputComponent(metadata, statement));
    initOperatorMenu(Locale.ENGLISH);
  }

  /**
   * Get the current Metadata Entry as a List of SearchElements
   * 
   * @param metadata
   * @param operator
   * @param distance
   * @return
   */
  private List<SearchElement> getAllSearchElements() {
    List<SearchElement> l = new LinkedList<>();
    Metadata metadata = input.getMetadata();
    if (!StringHelper.isNullOrEmptyTrim(metadata.getText())) {
      l.add(new SearchMetadata(statement.getIndexFormatted(), SearchMetadataFields.text, operator, metadata.getText(), not));
    }
    if (!StringHelper.isNullOrEmptyTrim(metadata.getName())) {
      this.operator = SearchOperators.EQUALS;
      l.add(new SearchMetadata(statement.getIndexFormatted(), SearchMetadataFields.placename, operator, metadata.getName(), not));
    }
    if (!StringHelper.isNullOrEmptyTrim(metadata.getTitle())) {
      l.add(new SearchMetadata(statement.getIndexFormatted(), SearchMetadataFields.title, operator, metadata.getTitle(), not));
    }
    if (!StringHelper.isNullOrEmptyTrim(metadata.getDate())) {
      l.add(new SearchMetadata(statement.getIndexFormatted(), SearchMetadataFields.date, operator, metadata.getDate(), not));
    }
    if (!StringHelper.isNullOrEmptyTrim(metadata.getUrl())) {
      l.add(new SearchMetadata(statement.getIndexFormatted(), SearchMetadataFields.url, operator, metadata.getUrl(), not));
    }
    if (!Double.isNaN(metadata.getNumber())) {
      l.add(new SearchMetadata(statement.getIndexFormatted(), SearchMetadataFields.number, operator, Double.toString(metadata.getNumber()),
          not));
    }
    if (!Double.isNaN(metadata.getLatitude()) && !Double.isNaN(metadata.getLongitude())) {
      this.operator = SearchOperators.EQUALS;
      // first, remove the search by name, since geosearch is currently done
      l = new LinkedList<>();
      l.add(new SearchMetadata(statement.getIndexFormatted(), SearchMetadataFields.coordinates, SearchOperators.EQUALS,
          Double.toString(metadata.getLatitude()) + "," + Double.toString(metadata.getLongitude())
              + (StringHelper.isNullOrEmptyTrim(distance) ? "" : "," + distance),
          not));
    }
    if (metadata.getPerson() != null) {
      if (!StringHelper.isNullOrEmptyTrim(metadata.getPerson().getFamilyName())) {
        l.add(new SearchMetadata(statement.getIndexFormatted(), SearchMetadataFields.familyname, operator,
            metadata.getPerson().getFamilyName(), not));
      }
      if (!StringHelper.isNullOrEmptyTrim(metadata.getPerson().getGivenName())) {
        l.add(new SearchMetadata(statement.getIndexFormatted(), SearchMetadataFields.givenname, operator,
            metadata.getPerson().getGivenName(), not));
      }
      if (!metadata.getPerson().getOrganizations().isEmpty()) {
        l.add(new SearchMetadata(statement.getIndexFormatted(), SearchMetadataFields.organisation, operator,
            metadata.getPerson().getOrganizationString(), not));
      }
    }
    return l;
  }

  /**
   * Initialize the operatory Menut
   * 
   * @param locale
   */
  private void initOperatorMenu(Locale locale) {
    operatorMenu = new ArrayList<SelectItem>();
    distanceMenu = null;
    if (statement == null) {
      return;
    }
    switch (statement.getType()) {
      case DATE:
        operatorMenu.add(new SelectItem(SearchOperators.EQUALS, "="));
        operatorMenu.add(new SelectItem(SearchOperators.GREATER, ">="));
        operatorMenu.add(new SelectItem(SearchOperators.LESSER, "<="));
        break;
      case NUMBER:
        operatorMenu.add(new SelectItem(SearchOperators.EQUALS, "="));
        operatorMenu.add(new SelectItem(SearchOperators.GREATER, ">="));
        operatorMenu.add(new SelectItem(SearchOperators.LESSER, "<="));
        break;
      case GEOLOCATION:
        operatorMenu = null;
        distanceMenu = new ArrayList<>();
        distanceMenu.add(new SelectItem("0", "="));
        distanceMenu.add(new SelectItem("100m", "< 100 m"));
        distanceMenu.add(new SelectItem("1km", "< 1 km"));
        distanceMenu.add(new SelectItem("5km", "< 5 km"));
        distanceMenu.add(new SelectItem("10km", "< 10 km"));
        distanceMenu.add(new SelectItem("50km", "< 50 km"));
        distanceMenu.add(new SelectItem("100km", "< 100 km"));
        distanceMenu.add(new SelectItem("500km", "< 500 km"));
        break;
      default:
        operatorMenu.add(new SelectItem(SearchOperators.EQUALS, "--"));
        operatorMenu.add(new SelectItem(SearchOperators.EQUALS, Imeji.RESOURCE_BUNDLE.getLabel("exactly", locale)));
    }
  }

  /**
   * Change the Statement by setting a new index
   * 
   * @param index
   */
  public void changeStatementIndex(String index) {
    this.statementIndex = index;
    this.setStatement(statements.get(index));
    this.metadata = new MetadataFactory().setStatementId(statementIndex).build();
  }

  /**
   * @return the statements
   */
  public Map<String, Statement> getStatements() {
    return statements;
  }

  /**
   * @return the metadata
   */
  public Metadata getMetadata() {
    return metadata;
  }

  /**
   * @param metadata the metadata to set
   */
  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }

  /**
   * @return the statementIndex
   */
  public String getStatementIndex() {
    return statementIndex;
  }

  /**
   * @param statementIndex the statementIndex to set
   */
  public void setStatementIndex(String statementIndex) {
    this.statementIndex = statementIndex;
  }

  /**
   * @return the statement
   */
  public Statement getStatement() {
    return statement;
  }

  /**
   * @param statement the statement to set
   */
  public void setStatement(Statement statement) {
    this.statement = statement;
  }

  /**
   * @return the distance
   */
  public String getDistance() {
    return distance;
  }

  /**
   * @param distance the distance to set
   */
  public void setDistance(String distance) {
    this.distance = distance;
  }

  /**
   * @return the operator
   */
  public SearchOperators getOperator() {
    return operator;
  }

  /**
   * @param operator the operator to set
   */
  public void setOperator(SearchOperators operator) {
    this.operator = operator;
  }

  /**
   * @return the not
   */
  public boolean isInverse() {
    return not;
  }

  /**
   * @param not the not to set
   */
  public void setInverse(boolean not) {
    this.not = not;
  }

  /**
   * @return the logicalRelation
   */
  public LOGICAL_RELATIONS getLogicalRelation() {
    return logicalRelation;
  }

  /**
   * @param logicalRelation the logicalRelation to set
   */
  public void setLogicalRelation(LOGICAL_RELATIONS logicalRelation) {
    this.logicalRelation = logicalRelation;
  }

  /**
   * @return the operatorMenu
   */
  public List<SelectItem> getOperatorMenu() {
    return operatorMenu;
  }

  /**
   * @param operatorMenu the operatorMenu to set
   */
  public void setOperatorMenu(List<SelectItem> operatorMenu) {
    this.operatorMenu = operatorMenu;
  }

  /**
   * @return the input
   */
  public MetadataInputComponent getInput() {
    return input;
  }

  /**
   * @param input the input to set
   */
  public void setInput(MetadataInputComponent input) {
    this.input = input;
  }

  /**
   * @return the distanceMenu
   */
  public List<SelectItem> getDistanceMenu() {
    return distanceMenu;
  }

  /**
   * @param distanceMenu the distanceMenu to set
   */
  public void setDistanceMenu(List<SelectItem> distanceMenu) {
    this.distanceMenu = distanceMenu;
  }

}
