package de.mpg.imeji.presentation.statement;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.statement.StatementService;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.StatementType;
import de.mpg.imeji.logic.vo.factory.StatementFactory;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

@ManagedBean(name = "StatementCreateBean")
@ViewScoped
public class StatementCreateBean extends SuperBean {
  private static final long serialVersionUID = 3080933791853851564L;
  private static final Logger LOGGER = Logger.getLogger(StatementCreateBean.class);
  private final List<SelectItem> statementTypeMenu = new ArrayList<>();
  private String name;
  private String namespace;
  private List<String> predefinedValues = new ArrayList<>();
  private String type = StatementType.TEXT.name();
  private boolean useGoogleMapsAPI = false;
  private boolean useMaxPlanckAuthors = false;

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
    StatementFactory factory = new StatementFactory().setIndex(name)
        .setType(StatementType.valueOf(type)).setNamespace(namespace).setVocabulary(getVocabulary())
        .setLiteralsConstraints(predefinedValues);
    final Statement statement = factory.build();
    try {
      service.create(statement, getSessionUser());
      redirect(getHistory().getPreviousPage().getCompleteUrlWithHistory());
    } catch (final ImejiException | IOException e) {
      BeanHelper.error("Error creating statement");
      LOGGER.error("Error creating statement", e);
    }
  }

  /**
   * Return the vocabulary chosen for this statement
   * 
   * @return
   */
  protected URI getVocabulary() {
    if (useGoogleMapsAPI && StatementType.GEOLOCATION.name().equals(type)) {
      return URI.create(Imeji.CONFIG.getGoogleMapsApi());
    }
    if (useMaxPlanckAuthors && StatementType.PERSON.name().equals(type)) {
      return URI.create(Imeji.CONFIG.getConeAuthors());
    }
    return null;
  }

  public void addPredefinedValue() {
    predefinedValues.add("");
  }

  public void removePredefinedValue(int index) {
    predefinedValues.remove(index);
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

  /**
   * @return the namespace
   */
  public String getNamespace() {
    return namespace;
  }

  /**
   * @param namespace the namespace to set
   */
  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public void predefinedValuesListener(ValueChangeEvent event) {
    int pos = Integer.parseInt(event.getComponent().getAttributes().get("position").toString());
    this.predefinedValues.set(pos, event.getNewValue().toString());
  }

  /**
   * @return the predefinedValues
   */
  public List<String> getPredefinedValues() {
    return predefinedValues;
  }

  /**
   * @param predefinedValues the predefinedValues to set
   */
  public void setPredefinedValues(List<String> predefinedValues) {
    this.predefinedValues = predefinedValues;
  }

  /**
   * @return the useGoogleMapsAPI
   */
  public boolean isUseGoogleMapsAPI() {
    return useGoogleMapsAPI;
  }

  /**
   * @param useGoogleMapsAPI the useGoogleMapsAPI to set
   */
  public void setUseGoogleMapsAPI(boolean useGoogleMapsAPI) {
    this.useGoogleMapsAPI = useGoogleMapsAPI;
  }

  /**
   * @return the useMaxPlanckAuthors
   */
  public boolean isUseMaxPlanckAuthors() {
    return useMaxPlanckAuthors;
  }

  /**
   * @param useMaxPlanckAuthors the useMaxPlanckAuthors to set
   */
  public void setUseMaxPlanckAuthors(boolean useMaxPlanckAuthors) {
    this.useMaxPlanckAuthors = useMaxPlanckAuthors;
  }
}
