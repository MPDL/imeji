package de.mpg.imeji.presentation.search.advanced.group;

import java.io.Serializable;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.model.SearchElement;

/**
 * Abstract Class the Group of the form of the advanced search
 * 
 * @author saquet
 *
 */
abstract class AbstractAdvancedSearchFormGroup implements Serializable {
  private static final long serialVersionUID = 8263343036009618543L;

  /**
   * Return the {@link AbstractAdvancedSearchFormGroup} as a {@link SearchElement}
   * 
   * @return
   */
  public abstract SearchElement toSearchElement();

  /**
   * Validate the current {@link AbstractAdvancedSearchFormGroup} and throw ann erro if not valid
   * 
   * @throws UnprocessableError
   */
  public abstract void validate() throws UnprocessableError;

}
