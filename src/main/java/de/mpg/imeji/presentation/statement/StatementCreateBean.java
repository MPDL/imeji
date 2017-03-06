package de.mpg.imeji.presentation.statement;

import java.io.IOException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.statement.StatementService;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

@ManagedBean(name = "StatementCreateBean")
@ViewScoped
public class StatementCreateBean extends SuperBean {
  private static final long serialVersionUID = 3080933791853851564L;
  private StatementForm statementForm = new StatementForm();
  private static Logger LOGGER = Logger.getLogger(StatementCreateBean.class);


  /**
   * Create a new statement
   */
  public void save() {
    final StatementService service = new StatementService();
    try {
      service.create(statementForm.asStatement(), getSessionUser());
      redirect(getHistory().getPreviousPage().getCompleteUrlWithHistory());
    } catch (final ImejiException | IOException e) {
      BeanHelper.error("Error creating statement");
      LOGGER.error("Error creating statement", e);
    }
  }

  /**
   * @return the statementForm
   */
  public StatementForm getStatementForm() {
    return statementForm;
  }

  /**
   * @param statementForm the statementForm to set
   */
  public void setStatementForm(StatementForm statementForm) {
    this.statementForm = statementForm;
  }

}
