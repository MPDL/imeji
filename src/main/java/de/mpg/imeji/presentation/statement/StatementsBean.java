package de.mpg.imeji.presentation.statement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.statement.StatementService;
import de.mpg.imeji.logic.statement.StatementUtil;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

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
  private Set<String> notUsed = new HashSet<>();
  private List<String> defaultStatements = new ArrayList<>();

  @PostConstruct
  public void init() {
    resetSelectedItems();
    try {
      List<Statement> list = service.retrieveAll();
      for (Statement s : list) {
        statements.putIfAbsent(s.getUri().toString(), s);
      }
      defaultStatements = StatementUtil.toStatementUriList(Imeji.CONFIG.getStatements());
      notUsed = service.retrieveNotUsedStatements().stream().map(s -> s.getIndex())
          .collect(Collectors.toSet());
    } catch (ImejiException e) {
      LOGGER.error("Error retrieving statements", e);
    }
  }

  /**
   * Delete the Statement for this index
   * 
   * @param index
   * @throws ImejiException
   * @throws IOException
   */
  public void delete() throws ImejiException, IOException {
    try {
      final String uri = FacesContext.getCurrentInstance().getExternalContext()
          .getRequestParameterMap().get("uri");
      Statement s = service.retrieve(uri, getSessionUser());
      service.delete(s, getSessionUser());
      removeFromDefaultStatements(s.getUri().toString());
      redirect(getNavigation().getApplicationUrl() + "statements");
    } catch (Exception e) {
      LOGGER.error("Error deleting statement", e);
      BeanHelper.error("Error deleting statement: " + e.getMessage());
    }

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
    return defaultStatements.stream().distinct().filter(s -> statements.containsKey(s))
        .map(s -> statements.get(s).getIndex()).collect(Collectors.joining(","));
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

  /**
   * @return the notUsed
   */
  public Set<String> getNotUsed() {
    return notUsed;
  }

  /**
   * @param notUsed the notUsed to set
   */
  public void setNotUsed(Set<String> notUsed) {
    this.notUsed = notUsed;
  }

}
