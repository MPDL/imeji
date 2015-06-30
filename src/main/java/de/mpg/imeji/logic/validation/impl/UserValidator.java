package de.mpg.imeji.logic.validation.impl;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.UserController;
import de.mpg.imeji.logic.validation.Validator;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.User;

/**
 * {@link Validator} for {@link CollectionImeji}
 * 
 * @author saquet
 *
 */
public class UserValidator extends ObjectValidator implements Validator<User>  {

	
	public UserValidator(Validator.Method method) {
		super(method);
	}

	
	@Override
	public void validate(User user) throws UnprocessableError {
		if (isDelete())
			return;
		if (!isValidEmail(user.getEmail())) {
				throw new UnprocessableError("error_user_email_not_valid");
		} 

		if (userAlreadyExists(user)) {
			throw new UnprocessableError("error_user_already_exists");
		} 

		if (user.getPerson() == null
				|| "".equals(user.getPerson().getFamilyName())
				|| user.getPerson().getFamilyName() == null) {
			throw new UnprocessableError("error_user_name_unfilled");
		}
	
	}
	

	/**
	 * True if the {@link User} exists
	 * 
	 * @return
	 * @throws Exception
	 */
	private boolean userAlreadyExists(User user)  {
			UserController uc = new UserController(Imeji.adminUser);
			return uc.existsUserWitheMail(user.getEmail(), user.getId().toString(), (Method.CREATE.equals(getValidateForMethod())?true:false));
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
	public void validate(User t, MetadataProfile p)
			throws UnprocessableError {
		validate(t);
	}
	
}