package de.mpg.imeji.presentation.search.advanced.group;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.statement.StatementService;
import de.mpg.imeji.logic.statement.StatementUtil;
import de.mpg.imeji.logic.vo.Statement;

/**
 * The Search Group of the advanced Serach for the metadata Search
 * 
 * @author saquet
 *
 */
public class MetadataSearchGroup extends AbstractAdvancedSearchFormGroup implements Serializable {
  private static final long serialVersionUID = -3036463292453331294L;
  private static final Logger LOGGER = Logger.getLogger(MetadataSearchGroup.class);
  private Map<String, Statement> statementMap;
  private List<Statement> statementList;
  private final List<MetadataSearchGroupEntry> entries;
  private final Locale locale;


  public MetadataSearchGroup(Locale locale) {
    this.entries = new ArrayList<>();
    this.locale = locale;
    try {
      statementMap = StatementUtil.statementListToMap(new StatementService().retrieveAll());
      statementList = statementMap.values().stream()
          .sorted((s1, s2) -> s1.getIndex().compareToIgnoreCase(s2.getIndex()))
          .collect(Collectors.toList());
      addEntry(0);
    } catch (ImejiException e) {
      LOGGER.error("Error reading statements", e);
    }
  }

  @Override
  public SearchElement toSearchElement() {
    SearchFactory factory = new SearchFactory();
    for (MetadataSearchGroupEntry entry : entries) {
      try {
        factory.addElement(entry.getSearchElement(), entry.getLogicalRelation());
      } catch (UnprocessableError e) {
        LOGGER.error("Error Adding a MetadataSearchGroupEntry to the query", e);
      }
    }
    return factory.buildAsGroup();
  }

  @Override
  public void validate() {
    // TODO Auto-generated method stub
  }

  public Map<String, Statement> getStatementMap() {
    return statementMap;
  }

  /**
   * 
   * @param position
   */
  public void addEntry(int position) {
    entries.add(position, new MetadataSearchGroupEntry(statementMap, locale));
  }

  /**
   * 
   * @param position
   */
  public void removeEntry(int position) {
    entries.remove(position);
  }

  /**
   * @return the entries
   */
  public List<MetadataSearchGroupEntry> getEntries() {
    return entries;
  }

  /**
   * @return the statementList
   */
  public List<Statement> getStatementList() {
    return statementList;
  }

  /**
   * @param statementList the statementList to set
   */
  public void setStatementList(List<Statement> statementList) {
    this.statementList = statementList;
  }


}
