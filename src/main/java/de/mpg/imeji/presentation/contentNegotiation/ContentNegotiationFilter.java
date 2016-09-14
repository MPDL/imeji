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
package de.mpg.imeji.presentation.contentNegotiation;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.search.model.SearchIndex;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.presentation.servlet.ExportServlet;

/**
 * Filter for the content negociation
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ContentNegotiationFilter implements Filter {
  private FilterConfig filterConfig = null;
  private static final Logger LOGGER = Logger.getLogger(ContentNegotiationFilter.class);

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
      // Limit the case to filter: dispachertype only forward
      if (DispatcherType.REQUEST.compareTo(serv.getDispatcherType()) == 0) {
        HttpServletRequest request = (HttpServletRequest) serv;
        if (rdfNegotiated(request)) {
          String url = "/export?format=rdf&n=10000";
          String type = getType(request);
          if (type != null) {
            url += "&type=" + type + "&" + getQuery(request);
            forwardToExport(url, request, resp);
          }
        }
      }
    } catch (Exception e) {
      LOGGER.error("Filtering error in content negotiation filter", e);
    } finally {
      chain.doFilter(serv, resp);
    }
  }

  /**
   * Return the type as needed by the {@link ExportServlet}
   *
   * @param request
   * @return
   */
  private String getType(HttpServletRequest request) {
    String path = request.getServletPath();
    if ("/browse".equals(path) || path.startsWith("/item/")) {
      return "image";
    }
    if ("/collections".equals(path) || path.startsWith("/collection/")) {
      return "collection";
    }
    if ("/albums".equals(path) || path.startsWith("/album/")) {
      return "album";
    }
    if ("/profile".equals(path) || path.startsWith("/profile/")) {
      return "profile";
    }
    return null;
  }

  /**
   * Return the query
   *
   * @param request
   * @return
   * @throws UnsupportedEncodingException
   */
  private String getQuery(HttpServletRequest request) throws UnsupportedEncodingException {
    String path = request.getServletPath();
    if (path.startsWith("/item/")) {
      return "q=" + SearchIndex.SearchFields.member.name() + "==\""
          + URLEncoder.encode(ObjectHelper.getURI(Item.class, getID(path)).toString(), "UTF-8")
          + "\"";
    }
    if (path.startsWith("/collection/")) {
      return "q=" + SearchIndex.SearchFields.col.name() + "==\""
          + ObjectHelper.getURI(CollectionImeji.class, getID(path)) + "\"";
    }
    if (path.startsWith("/album/")) {
      return "q=" + SearchIndex.SearchFields.alb.name() + "==\""
          + URLEncoder.encode(ObjectHelper.getURI(Album.class, getID(path)).toString(), "UTF-8")
          + "\"";
    }
    if (path.startsWith("/profile/")) {
      return "q=" + SearchIndex.SearchFields.prof.name() + "==\"" + URLEncoder.encode(
          ObjectHelper.getURI(MetadataProfile.class, getID(path)).toString(), "UTF-8") + "\"";
    }
    return request.getQueryString();
  }

  private String getID(String path) {
    return path.split("/")[2];
  }

  /**
   * True if the request asked for rdf
   *
   * @param request
   * @return
   */
  private boolean rdfNegotiated(HttpServletRequest request) {
    if (request != null && request.getHeader("Accept") != null) {
      return request.getHeader("Accept").startsWith("application/rdf+xml");
    }
    return false;
  }

  /**
   * Forward to the exporturl
   *
   * @param exportUrl
   * @param aRequest
   * @param aResponse
   * @throws ServletException
   * @throws IOException
   */
  private void forwardToExport(String exportUrl, HttpServletRequest aRequest,
      ServletResponse aResponse) throws ServletException, IOException {
    RequestDispatcher dispatcher = aRequest.getRequestDispatcher(exportUrl);
    dispatcher.forward(aRequest, aResponse);
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  @Override
  public void init(FilterConfig arg0) throws ServletException {

  }

  public FilterConfig getFilterConfig() {
    return filterConfig;
  }

  public void setFilterConfig(FilterConfig filterConfig) {
    this.filterConfig = filterConfig;
  }
}
