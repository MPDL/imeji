package de.mpg.imeji.presentation.statement;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.statement.StatementService;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * JSF Bean for the page edit statement
 * 
 * @author saquet
 *
 */
@ViewScoped
@ManagedBean(name = "StatementEditBean")
public class StatementEditBean extends StatementCreateBean {
  private static final long serialVersionUID = 5191523522987113715L;
  private static final Logger LOGGER = Logger.getLogger(StatementEditBean.class);
  private StatementService service = new StatementService();

  public StatementEditBean() {

  }

  @PostConstruct
  public void init() {
    try {
      String id = URLDecoder.decode(UrlHelper.getParameterValue("statementId"), "UTF-8");
      Statement s =
          service.retrieve(ObjectHelper.getURI(Statement.class, id).toString(), getSessionUser());
      getStatementForm().setType(s.getType().name());
      getStatementForm().setName(s.getIndex());
      getStatementForm().setNamespace(s.getNamespace());
      if (s.getVocabulary() != null) {
        getStatementForm().setUseGoogleMapsAPI(
            s.getVocabulary().toString().equals(Imeji.CONFIG.getGoogleMapsApi()));
        getStatementForm().setUseMaxPlanckAuthors(
            s.getVocabulary().toString().equals(Imeji.CONFIG.getConeAuthors()));
      }
      if (s.getLiteralConstraints() != null) {
        getStatementForm().getPredefinedValues().addAll(s.getLiteralConstraints());
      }
    } catch (ImejiException | UnsupportedEncodingException e) {
      LOGGER.error("Error retrieving statement: ", e);
    }
  }

  @Override
  public void save() {
    try {
      service.update(getStatementForm().asStatement(), getSessionUser());
      redirect(getHistory().getPreviousPage().getCompleteUrlWithHistory());
    } catch (final ImejiException | IOException e) {
      BeanHelper.error("Error creating statement");
      LOGGER.error("Error creating statement", e);
    }
  }
}
