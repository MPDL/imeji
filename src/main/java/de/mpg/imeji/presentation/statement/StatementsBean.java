package de.mpg.imeji.presentation.statement;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.statement.StatementService;
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


  @PostConstruct
  public void init() {
    try {
      setStatements(service.retrieveAll());
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


}
