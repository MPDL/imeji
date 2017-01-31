package de.mpg.imeji.presentation.statement;

import java.net.URI;
import java.util.ArrayList;
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
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.Statement;
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
  private List<Statement> statements = new ArrayList<>();
  private List<String> defaultStatements = new ArrayList<>();

  @PostConstruct
  public void init() {
    try {
      setStatements(service.retrieveAll());
      defaultStatements = StatementUtil.toStatementUriList(Imeji.CONFIG.getStatements());
    } catch (ImejiException e) {
      LOGGER.error("Error retrieving statements", e);
    }
  }

  /**
   * @return the statements
   */
  public List<Statement> getStatements() {
    return statements;
  }

  /**
   * @param statements the statements to set
   */
  public void setStatements(List<Statement> statements) {
    this.statements = statements;
  }

  /**
   * Add the index to the default statements and save
   * 
   * @param index
   */
  public void addToDefaultStatements(String index) {
    if (!isDefaultStatement(index)) {
      defaultStatements.add(ObjectHelper.getURI(Statement.class, index).toString());
      saveDefaultStatements();
    }
  }

  /**
   * Remove the index from the default statements and save
   * 
   * @param index
   */
  public void removeFromDefaultStatements(String index) {
    if (isDefaultStatement(index)) {
      defaultStatements.remove(ObjectHelper.getURI(Statement.class, index).toString());
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
    return defaultStatements.stream().map(s -> ObjectHelper.getId(URI.create(s)))
        .collect(Collectors.joining(","));
  }

  /**
   * True if the index is contained in the default statements list
   * 
   * @param index
   * @return
   */
  public boolean isDefaultStatement(String index) {
    return defaultStatements.contains(ObjectHelper.getURI(Statement.class, index).toString());
  }
}
