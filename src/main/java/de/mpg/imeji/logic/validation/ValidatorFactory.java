package de.mpg.imeji.logic.validation;

import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Metadata;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.validation.impl.CollectionValidator;
import de.mpg.imeji.logic.validation.impl.FacetValidator;
import de.mpg.imeji.logic.validation.impl.ItemValidator;
import de.mpg.imeji.logic.validation.impl.MetadataValidator;
import de.mpg.imeji.logic.validation.impl.PseudoValidator;
import de.mpg.imeji.logic.validation.impl.UserGroupValidator;
import de.mpg.imeji.logic.validation.impl.UserValidator;
import de.mpg.imeji.logic.validation.impl.Validator;

/**
 * Factory for {@link Validator}
 *
 * @author saquet
 *
 */
public class ValidatorFactory {

  private ValidatorFactory() {
    // avoid constructor
  }

  /**
   * Return a new {@link Validator} according to the object class
   *
   * @param <T>
   *
   * @param t
   * @return
   */
  public static Validator<?> newValidator(Object obj, Validator.Method method) {
    Validator<?> validator = new PseudoValidator();
    // For now, do not do anything with Delete, just a possibility
    if (Validator.Method.DELETE.equals(method)) {
      return validator;
    }
    if (obj instanceof Item) {
      validator = new ItemValidator();
    } else if (obj instanceof Metadata) {
      validator = new MetadataValidator();
    } else if (obj instanceof CollectionImeji) {
      validator = new CollectionValidator();
    } else if (obj instanceof User) {
      validator = new UserValidator();
    } else if (obj instanceof UserGroup) {
      validator = new UserGroupValidator();
    } else if (obj instanceof Facet) {
      validator = new FacetValidator();
    }
    return validator;
  }
}
