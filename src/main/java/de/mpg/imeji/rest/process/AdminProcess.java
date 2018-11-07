package de.mpg.imeji.rest.process;

import javax.ws.rs.core.Response.Status;

import org.jose4j.lang.JoseException;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.rest.api.UserAPIService;
import de.mpg.imeji.rest.to.JSONResponse;
import de.mpg.imeji.rest.to.UserTO;

public class AdminProcess {

	/**
	 * Login to imeji. If success, return the {@link UserTO}
	 *
	 * @param req
	 * @return
	 */
	public static JSONResponse login(String authorizationHeader) {
		try {
			// Authenticate user and update the key if successfull authentication
			final User userVO = BasicAuthentication.auth(authorizationHeader);
			final UserAPIService service = new UserAPIService();
			final UserTO userTO = service.updateUserKey(userVO, true);
			return RestProcessUtils.buildResponse(Status.OK.getStatusCode(), userTO);
		} catch (ImejiException | JoseException e) {
			return RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
		}
	}

	/**
	 * Invalidate the current key
	 *
	 * @param authorizationHeader
	 * @return
	 */
	public static JSONResponse logout(String authorizationHeader) {
		try {
			// Authenticate user and update the key if successfull authentication
			// key will only be updated if the key already existed
			final User userVO = BasicAuthentication.auth(authorizationHeader);
			final UserAPIService service = new UserAPIService();
			service.updateUserKey(userVO, false);
			return RestProcessUtils.buildResponse(Status.OK.getStatusCode(), null);
		} catch (ImejiException | JoseException e) {
			return RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
		}
	}

	/**
	 *
	 */
	public static JSONResponse invalidMethod() {
		return RestProcessUtils.buildResponse(Status.METHOD_NOT_ALLOWED.getStatusCode(), null);
	}

	/**
	*
	*/
	public static JSONResponse invalidResource() {
		return RestProcessUtils.buildResponse(Status.NOT_FOUND.getStatusCode(), null);
	}

}
