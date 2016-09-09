/*
 *
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the Common Development and Distribution
 * License, Version 1.0 only (the "License"). You may not use this file except in compliance with
 * the License.
 *
 * You can obtain a copy of the license at license/ESCIDOC.LICENSE or http://www.escidoc.de/license.
 * See the License for the specific language governing permissions and limitations under the
 * License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each file and include the License
 * file at license/ESCIDOC.LICENSE. If applicable, add the following below this CDDL HEADER, with
 * the fields enclosed by brackets "[]" replaced with your own identifying information: Portions
 * Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */
/*
 * Copyright 2006-2007 Fachinformationszentrum Karlsruhe Gesellschaft für
 * wissenschaftlich-technische Information mbH and Max-Planck- Gesellschaft zur Förderung der
 * Wissenschaft e.V. All rights reserved. Use is subject to license terms.
 */
package de.mpg.imeji.presentation.auth;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.auth.authentication.impl.HttpAuthentication;
import de.mpg.imeji.logic.controller.resource.UserController;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * {@link Filter} for imeji authentification
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class AuthenticationFilter implements Filter {
  private FilterConfig filterConfig = null;
  private Pattern jsfPattern = Pattern.compile(".*\\/jsf\\/.*\\.xhtml");
  private static final Logger LOGGER = Logger.getLogger(AuthenticationFilter.class);

  /*
   * (non-Javadoc)
   *
   * @see javax.servlet.Filter#destroy()
   */
  @Override
  public void destroy() {
    setFilterConfig(null);
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
   * javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(ServletRequest serv, ServletResponse resp, FilterChain chain)
      throws IOException, ServletException {
    try {
      HttpServletRequest request = (HttpServletRequest) serv;
      SessionBean session = getSession(request);
      if (session != null && session.getUser() == null) {
        HttpAuthentication httpAuthentification = new HttpAuthentication(request);
        if (httpAuthentification.hasLoginInfos()) {
          session.setUser(httpAuthentification.doLogin());
        }
      } else if (session != null && session.getUser() != null) {
        if (isReloadUser(request, session.getUser())) {
          session.reloadUser();
        }
      }
    } catch (Exception e) {
      LOGGER.info("We had some exception in Authentication filter", e);
    } finally {
      chain.doFilter(serv, resp);
    }
  }

  /**
   * True if it is necessary to reload the User. This method tried to reduce as much as possible
   * reload of the user, to avoid too much database queries.
   *
   * @param req
   * @return
   */
  private boolean isReloadUser(HttpServletRequest req, User user) {
    return isXHTMLRequest(req) && !isAjaxRequest(req) && isModifiedUser(user);
  }

  /**
   * True if the {@link User} has been modified in the database (for instance, a user has share
   * something with him)
   *
   * @param user
   * @return
   */
  private boolean isModifiedUser(User user) {
    try {
      return new UserController(user).isModified(user);
    } catch (Exception e) {
      return true;
    }
  }

  /**
   * True if the request is done from an xhtml page
   *
   * @param req
   * @return
   */
  private boolean isXHTMLRequest(HttpServletRequest req) {
    Matcher m = jsfPattern.matcher(req.getRequestURI());
    return m.matches();
  }

  /**
   * True of the request is an Ajax Request
   *
   * @param req
   * @return
   */
  private boolean isAjaxRequest(HttpServletRequest req) {
    return "partial/ajax".equals(req.getHeader("Faces-Request"));
  }


  /*
   * (non-Javadoc)
   *
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  @Override
  public void init(FilterConfig arg0) throws ServletException {
    this.setFilterConfig(arg0);
  }

  public FilterConfig getFilterConfig() {
    return filterConfig;
  }

  public void setFilterConfig(FilterConfig filterConfig) {
    this.filterConfig = filterConfig;
  }

  /**
   * Return the {@link SessionBean} form the {@link HttpSession}
   *
   * @param req
   * @return
   */
  private SessionBean getSession(HttpServletRequest req) {
    return (SessionBean) req.getSession(true).getAttribute(SessionBean.class.getSimpleName());
  }


}
