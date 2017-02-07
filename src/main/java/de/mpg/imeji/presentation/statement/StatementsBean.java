package de.mpg.imeji.presentation.statement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.statement.StatementService;
import de.mpg.imeji.logic.statement.StatementUtil;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.presentation.beans.SuperBean;

/**
 * JSF Bean for the Statements page
 * 
 * @author saquet
 *
 */
@ManagedBean(name = "StatementsBean")
@ViewScoped
public class StatementsBean extends SuperBean {
  private static final long serialVersionUID = 3215418612370596545L;
  private static final Logger LOGGER = Logger.getLogger(StatementsBean.class);
  private StatementService service = new StatementService();
  private LinkedHashMap<String, Statement> statements = new LinkedHashMap<>();
  private List<String> defaultStatements = new ArrayList<>();

  @PostConstruct
  public void init() {
    try {
      List<Statement> list = service.retrieveAll();
      for (Statement s : list) {
        statements.putIfAbsent(s.getUri().toString(), s);
      }
      defaultStatements = StatementUtil.toStatementUriList(Imeji.CONFIG.getStatements());
    } catch (ImejiException e) {
      LOGGER.error("Error retrieving statements", e);
    }
  }

  /**
   * Delete the Statement for this index
   * 
   * @param index
   * @throws ImejiException
   */
  public void delete(String index) throws ImejiException {
    Statement s = ImejiFactory.newStatement().setIndex(index).build();
    s = service.retrieve(s.getUri().toString(), getSessionUser());
    service.delete(s, getSessionUser());
    removeFromDefaultStatements(index);
    init();
  }

  /**
   * @return the statements
   */
  public List<Statement> getStatements() {
    return new ArrayList<>(statements.values());
  }

  /**
   * Add the index to the default statements and save
   * 
   * @param index
   */
  public void addToDefaultStatements(String uri) {
    if (!isDefaultStatement(uri)) {
      defaultStatements.add(uri);
      saveDefaultStatements();
    }
  }

  /**
   * Remove the index from the default statements and save
   * 
   * @param index
   */
  public void removeFromDefaultStatements(String uri) {
    if (isDefaultStatement(uri)) {
      defaultStatements.remove(uri);
      saveDefaultStatements();
    }
  }

  /**
   * Set the default statements in the config and save the config
   */
  private void saveDefaultStatements() {
    Imeji.CONFIG.setStatements(getDefaultStatementsString());
    Imeji.CONFIG.saveConfig();
  }

  /**
   * Return the default Statements as String
   * 
   * @return
   */
  public String getDefaultStatementsString() {
    return defaultStatements.stream().distinct().map(s -> statements.get(s).getIndex())
        .collect(Collectors.joining(","));
  }

  /**
   * True if the index is contained in the default statements list
   * 
   * @param index
   * @return
   */
  public boolean isDefaultStatement(String uri) {
    return defaultStatements.contains(uri);
  }
}
