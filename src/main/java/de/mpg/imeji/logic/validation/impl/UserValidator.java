package de.mpg.imeji.logic.validation.impl;

import java.util.HashSet;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.User;

/**
 * {@link Validator} for {@link CollectionImeji}
 *
 * @author saquet
 *
 */
public class UserValidator extends ObjectValidator implements Validator<User> {
  private UnprocessableError exception = new UnprocessableError(new HashSet<String>());

  @Override
  public void validate(User user, Method m) throws UnprocessableError {
    exception = new UnprocessableError();
    setValidateForMethod(m);
    if (isDelete()) {
      return;
    }

    if (user.getEmail() == null || "".equals(user.getEmail().trim())) {
      exception = new UnprocessableError("error_user_email_unfilled", exception);
    } else if (!isValidEmail(user.getEmail())) {
      exception = new UnprocessableError("error_user_email_not_valid", exception);
    }

    if (emailAlreadyUsed(user)) {
      exception = new UnprocessableError("error_user_already_exists", exception);
    }

    if (user.getPerson() == null || "".equals(user.getPerson().getFamilyName())
        || user.getPerson().getFamilyName() == null) {
      exception = new UnprocessableError("error_user_name_unfilled", exception);
    }

    if (user.getPerson() != null && "".equals(user.getPerson().getOrganizationString())) {
      exception = new UnprocessableError("error_user_organization_unfilled", exception);
    }

    if (exception.hasMessages()) {
      throw exception;
    }
  }


  /**
   * True if the {@link User} exists
   *
   * @return
   * @throws Exception
   */
  private boolean emailAlreadyUsed(User user) {
    UserBusinessController uc = new UserBusinessController();
    return uc.existsUserWitheMail(user.getEmail(), user.getId().toString(),
        (Method.CREATE.equals(getValidateForMethod()) ? true : false));
  }

  /**
   * Is true if the Email is valid
   *
   * @return
   */
  public static boolean isValidEmail(String email) {
    String regexEmailMatch = "([^.@]+)(\\.[^.@]+)*@([^.@]+\\.)+([^.@]+)";
    return email.matches(regexEmailMatch);
  }

  @Override
  public void validate(User t, MetadataProfile p, Method m) throws UnprocessableError {
    validate(t, m);
  }

}
