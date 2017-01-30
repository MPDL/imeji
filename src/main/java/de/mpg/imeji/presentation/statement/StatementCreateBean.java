package de.mpg.imeji.presentation.statement;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.statement.StatementService;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.StatementType;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

@ManagedBean(name = "StatementCreateBean")
@ViewScoped
public class StatementCreateBean extends SuperBean {
  private static final long serialVersionUID = 3080933791853851564L;
  private static final Logger LOGGER = Logger.getLogger(StatementCreateBean.class);
  private final List<SelectItem> statementTypeMenu = new ArrayList<>();
  private String name;
  private String type = StatementType.TEXT.name();

  public StatementCreateBean() {
    for (final StatementType statementType : StatementType.values()) {
      statementTypeMenu.add(new SelectItem(statementType.name()));
    }
  }

  /**
   * Create a new statement
   */
  public void save() {
    final StatementService service = new StatementService();
    final Statement statement =
        ImejiFactory.newStatement().setIndex(name).setType(StatementType.valueOf(type)).build();
    try {
      service.create(statement, getSessionUser());
    } catch (final ImejiException e) {
      BeanHelper.error("Error creating statement");
      LOGGER.error("Error creating statement", e);
    }
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * @return the statementTypeMenu
   */
  public List<SelectItem> getStatementTypeMenu() {
    return statementTypeMenu;
  }
}
