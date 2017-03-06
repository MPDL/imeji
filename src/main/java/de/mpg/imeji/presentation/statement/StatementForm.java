package de.mpg.imeji.presentation.statement;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.StatementType;
import de.mpg.imeji.logic.vo.factory.StatementFactory;


public class StatementForm implements Serializable {
  private static final long serialVersionUID = -5136099857875389077L;
  private final List<SelectItem> statementTypeMenu = new ArrayList<>();
  private String name;
  private String namespace;
  private List<String> predefinedValues = new ArrayList<>();
  private String type = StatementType.TEXT.name();
  private boolean useGoogleMapsAPI = false;
  private boolean useMaxPlanckAuthors = false;

  public StatementForm() {
    for (final StatementType statementType : StatementType.values()) {
      statementTypeMenu.add(new SelectItem(statementType.name()));
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

  /**
   * Return the Form as a Statement
   * 
   * @return
   */
  public Statement asStatement() {
    return new StatementFactory().setIndex(name).setType(StatementType.valueOf(type))
        .setNamespace(namespace).setVocabulary(getVocabulary())
        .setLiteralsConstraints(predefinedValues).build();
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
