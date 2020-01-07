package de.mpg.imeji.presentation.statement;

import java.io.IOException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.ImejiExceptionWithUserMessage;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.statement.StatementService;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

@ManagedBean(name = "StatementCreateBean")
@ViewScoped
public class StatementCreateBean extends SuperBean {
  private static final long serialVersionUID = 3080933791853851564L;
  private StatementForm statementForm = new StatementForm();
  private static final Logger LOGGER = LogManager.getLogger(StatementCreateBean.class);

  /**
   * Create a new statement
   */
  public void save() {
    final StatementService service = new StatementService();
    try {
      service.create(statementForm.asStatement(), getSessionUser());
      redirect(getNavigation().getApplicationUrl() + "statements");
    } catch (final ImejiExceptionWithUserMessage exceptionWithMessage) {
        String userMessage = Imeji.RESOURCE_BUNDLE.getMessage(exceptionWithMessage.getMessageLabel(), getLocale());
        BeanHelper.error(userMessage);
        if (exceptionWithMessage.getMessage() != null) {
          LOGGER.error(exceptionWithMessage.getMessage(), exceptionWithMessage);
        } else {
          LOGGER.error(userMessage, exceptionWithMessage);
        }
      }     
    catch (final ImejiException | IOException e) {
      BeanHelper.error("Error creating statement: " + e.getMessage());
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
