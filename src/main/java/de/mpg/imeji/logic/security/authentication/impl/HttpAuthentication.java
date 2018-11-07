package de.mpg.imeji.logic.security.authentication.impl;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.security.authentication.Authentication;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * {@link Authentification} for {@link HttpServletRequest}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class HttpAuthentication implements Authentication {
	/**
	 * The content of the http header
	 */
	private String usernamePassword = null;
	private String apiKey = null;

	/**
	 * Constructor with {@link HttpServletRequest}
	 */
	public HttpAuthentication(HttpServletRequest request) {
		this(getAuthorizationHeader(request));
	}

	/**
	 * Constructor with the authorization header
	 *
	 * @param authorizationHeader
	 */
	public HttpAuthentication(String authorizationHeader) {
		parseAuthorizationHeader(authorizationHeader);
	}

	/**
	 * Get the Authorization header
	 *
	 * @param request
	 * @return
	 */
	private static String getAuthorizationHeader(HttpServletRequest request) {
		if (request.getHeader("Authorization") == null
				&& !StringHelper.isNullOrEmptyTrim(request.getParameter("apiKey"))) {
			return "Bearer " + request.getParameter("apiKey");
		}
		return request.getHeader("Authorization");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.mpg.imeji.logic.auth.Authentification#doLogin()
	 */
	@Override
	public User doLogin() throws AuthenticationError {
		if (apiKey != null) {
			final APIKeyAuthentication keyAuthentication = new APIKeyAuthentication(apiKey);
			return keyAuthentication.doLogin();
		} else if (usernamePassword != null) {
			final int p = usernamePassword.indexOf(":");
			if (p != -1) {
				return new DefaultAuthentication(getUserLogin(), getUserPassword()).doLogin();
			}
		}
		// not logged in
		return null;
	}

	/**
	 * Parse the authprization header and set the variables
	 *
	 * @param authHeader
	 */
	private void parseAuthorizationHeader(String authHeader) {
		if (authHeader != null && authHeader.contains("Basic")) {
			usernamePassword = new String(Base64.decodeBase64(authHeader.replace("Basic ", "").trim().getBytes()));
		}
		if (authHeader != null && authHeader.contains("Bearer ")) {
			apiKey = authHeader.replace("Bearer ", "").trim();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.mpg.imeji.logic.auth.Authentification#getUserLogin()
	 */
	@Override
	public String getUserLogin() {
		if (usernamePassword != null) {
			final int p = usernamePassword.indexOf(":");
			if (p != -1) {
				return usernamePassword.substring(0, p);
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.mpg.imeji.logic.auth.Authentification#getUserPassword()
	 */
	@Override
	public String getUserPassword() {
		if (usernamePassword != null) {
			final int p = usernamePassword.indexOf(":");
			if (p != -1) {
				return usernamePassword.substring(p + 1);
			}
		}
		return null;
	}

	/**
	 * True if the request has informations about the login (user and password)
	 *
	 * @return
	 */
	public boolean hasLoginInfos() {
		return usernamePassword != null;
	}
}
