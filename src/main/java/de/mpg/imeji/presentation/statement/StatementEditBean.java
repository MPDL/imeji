package de.mpg.imeji.presentation.statement;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.statement.StatementService;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.StatementType;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
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

  @PostConstruct
  public void init() {
    String id = UrlHelper.getParameterValue("id");
    try {
      Statement s =
          service.retrieve(ObjectHelper.getURI(Statement.class, id).toString(), getSessionUser());
      setType(s.getType().name());
      setName(s.getIndex());
    } catch (ImejiException e) {
      LOGGER.error("Error retrieving statement: ", e);
    }
  }

  @Override
  public void save() {
    final Statement statement = ImejiFactory.newStatement().setIndex(getName())
        .setType(StatementType.valueOf(getType())).build();
    try {
      service.update(statement, getSessionUser());
    } catch (final ImejiException e) {
      BeanHelper.error("Error creating statement");
      LOGGER.error("Error creating statement", e);
    }
  }
}
