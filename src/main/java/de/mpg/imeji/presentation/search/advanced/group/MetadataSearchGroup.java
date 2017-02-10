package de.mpg.imeji.presentation.search.advanced.group;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
  private final List<MetadataSearchGroupEntry> entries;


  public MetadataSearchGroup(Locale locale) {
    entries = new ArrayList<>();
    try {
      statementMap = StatementUtil.statementListToMap(new StatementService().retrieveAll());
      addEntry(0, locale);
    } catch (ImejiException e) {
      LOGGER.error("Error reading statements", e);
    }
  }

  @Override
  public SearchElement toSearchElement() {
    SearchFactory factory = new SearchFactory();
    for (MetadataSearchGroupEntry entry : entries) {
      try {
        factory.addElement(entry.getSearchGroup(), entry.getLogicalRelation());
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
  public void addEntry(int position, Locale locale) {
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


}