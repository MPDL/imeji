/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.mdProfile.wrapper;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import de.mpg.imeji.logic.ImejiNamespaces;
import de.mpg.imeji.logic.controller.util.ImejiFactory;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;
import de.mpg.imeji.logic.vo.util.ObjectHelper;
import de.mpg.imeji.presentation.mdProfile.MdProfileBean;
import de.mpg.imeji.presentation.util.VocabularyHelper;
import de.mpg.imeji.util.LocalizedString;

/**
 * Wrapper for {@link Statement}, used in java bean {@link MdProfileBean}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class StatementWrapper implements Comparable<StatementWrapper>, Serializable {
  private static final long serialVersionUID = 7304068244275901609L;
  private Statement statement;
  private boolean multiple = false;
  private String vocabularyString;
  private transient VocabularyHelper vocabularyHelper;
  private boolean showRemoveWarning = false;
  private int level = 0;
  /**
   * True if this {@link Statement} is used by at least on {@link Metadata} in imeji
   */
  private boolean used;

  /**
   * Create a new {@link StatementWrapper}
   *
   * @param st
   * @param profile
   */
  public StatementWrapper(Statement st, URI profile, int level) {
    init(st);
    this.level = level;
  }

  /**
   * Reset this {@link StatementWrapper} with emtpy {@link Statement}
   */
  public void reset() {
    Statement newSt = ImejiFactory.newStatement();
    newSt.setType(statement.getType());
    init(newSt);
  }

  /**
   * Initialize the {@link StatementWrapper} fields with {@link Statement} fields
   *
   * @param st
   */
  public void init(Statement st) {
    statement = st;
    initMultiple();
    initVocabulary();
    used = ImejiSPARQL
        .exec("PREFIX fn: <http://www.w3.org/2005/xpath-functions#> SELECT DISTINCT ?s WHERE {?s <"
            + ImejiNamespaces.METADATA + "> ?md . ?md <http://imeji.org/terms/statement> <"
            + statement.getId() + ">} LIMIT 1 ", null)
        .size() > 0;
    for (LocalizedString s : statement.getLabels()) {
      if (s.getLang() == null) {
        s.setLang("en");
      }
    }
  }

  /**
   * Initialize the multiple value according to the {@link Statement}
   */
  private void initMultiple() {
    if ("unbounded".equals(statement.getMaxOccurs())
        || Integer.parseInt(statement.getMaxOccurs()) > 1) {
      multiple = true;
    } else {
      multiple = false;
    }
  }

  /**
   * Initialize the {@link VocabularyHelper} according to the {@link Statement}
   */
  private void initVocabulary() {
    vocabularyHelper = new VocabularyHelper();
    if (statement.getVocabulary() != null) {
      vocabularyString = statement.getVocabulary().toString();
      if ("unknown".equals(vocabularyHelper.getVocabularyName(statement.getVocabulary()))) {
        vocabularyHelper.getVocabularies()
            .add(new SelectItem(statement.getVocabulary().toString(), vocabularyString));
      }
    } else {
      vocabularyString = null;
    }
  }

  /**
   * Return the {@link StatementWrapper} as a {@link Statement}
   *
   * @return
   */
  public Statement getAsStatement() {
    if (vocabularyString != null) {
      statement.setVocabulary(URI.create(vocabularyString));
    } else {
      statement.setVocabulary(null);
    }
    return statement;
  }

  /**
   * Return the id of the {@link Statement} (i.e. the last part of the {@link URI})
   *
   * @return
   */
  public String getStatementId() {
    return ObjectHelper.getId(statement.getId());
  }

  /**
   * Return the id of the parent of the current {@link Statement}
   *
   * @return
   */
  public String getParentId() {
    if (statement.getParent() != null) {
      return ObjectHelper.getId(statement.getParent());
    }
    return null;
  }

  /**
   * Return count of defined labels
   *
   * @return
   */
  public int getLabelsCount() {
    return statement.getLabels().size();
  }

  /**
   * Listener for metadata type menu
   *
   * @param event
   */
  public void typeListener() {
    vocabularyString = null;
    statement.getLiteralConstraints().clear();
  }

  /**
   * Is called when the user select a statement or a language in the drop down list
   */
  public void select() {
    // do nothing
  }

  /**
   * Listener for the preview select box
   *
   * @param event
   */
  public void previewListener(ValueChangeEvent event) {
    if (event.getNewValue() != null && event.getNewValue() != event.getOldValue()) {
      statement.setPreview((Boolean) event.getNewValue());
    }
  }

  /**
   * Listener for the multiple select box
   *
   * @param event
   */
  public void multipleListener(ValueChangeEvent event) {
    if (event.getNewValue() != null && event.getNewValue() != event.getOldValue()) {
      if ((Boolean) event.getNewValue()) {
        statement.setMaxOccurs("unbounded");
      } else {
        statement.setMaxOccurs("1");
      }
    }
  }

  /**
   * Listener for the description select box
   *
   * @param event
   */
  public void descriptionListener(ValueChangeEvent event) {
    if (event.getNewValue() != null && event.getNewValue() != event.getOldValue()) {
      statement.setDescription((Boolean) event.getNewValue());
    }
  }

  /**
   * Listener for the constraints fields
   *
   * @param event
   */
  public void constraintListener(ValueChangeEvent event) {
    if (event.getNewValue() != null && event.getNewValue() != event.getOldValue()) {
      int pos = Integer.parseInt(event.getComponent().getAttributes().get("position").toString());
      ((List<String>) statement.getLiteralConstraints()).set(pos, event.getNewValue().toString());
    }
  }

  /**
   * getter
   *
   * @return
   */
  public Statement getStatement() {
    return statement;
  }

  /**
   * setter
   *
   * @param statement
   */
  public void setStatement(Statement statement) {
    this.statement = statement;
  }

  /**
   * Return the count of the constraints
   *
   * @return
   */
  public int getConstraintsSize() {
    return statement.getLiteralConstraints().size();
  }

  /**
   * getter
   *
   * @return
   */
  public boolean isMultiple() {
    return multiple;
  }

  /**
   * setter
   *
   * @param statement
   */
  public void setMultiple(boolean multiple) {
    this.multiple = multiple;
  }

  /**
   * getter
   *
   * @return
   */
  public String getVocabularyString() {
    return vocabularyString;
  }

  /**
   * setter
   *
   * @param statement
   */
  public void setVocabularyString(String vocabularyString) {
    this.vocabularyString = vocabularyString;
  }

  /**
   * getter
   *
   * @return
   */
  public VocabularyHelper getVocabularyHelper() {
    return vocabularyHelper;
  }

  /**
   * setter
   *
   * @param statement
   */
  public void setVocabularyHelper(VocabularyHelper vocabularyHelper) {
    this.vocabularyHelper = vocabularyHelper;
  }

  /**
   * setter
   *
   * @param statement
   */
  public void setShowRemoveWarning(boolean showRemoveWarning) {
    this.showRemoveWarning = showRemoveWarning;
  }

  /**
   * getter
   *
   * @return
   */
  public boolean isShowRemoveWarning() {
    return showRemoveWarning;
  }

  /**
   * setter
   *
   * @param level the level to set
   */
  public void setLevel(int level) {
    this.level = level;
  }

  /**
   * getter
   *
   * @return the level
   */
  public int getLevel() {
    return level;
  }

  /**
   * getter
   *
   * @return the used
   */
  public boolean isUsed() {
    return used;
  }

  /**
   * setter
   *
   * @param used the used to set
   */
  public void setUsed(boolean used) {
    this.used = used;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(StatementWrapper o) {
    if (getStatement().getPos() > o.getStatement().getPos()) {
      return 1;
    } else if (getStatement().getPos() < o.getStatement().getPos()) {
      return -1;
    }
    return 0;
  }
}
