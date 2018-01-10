package de.mpg.imeji.presentation.util;

import java.io.IOException;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;

import org.apache.http.HttpRequest;

/**
 * Set encoding for all {@link HttpRequest} to UTF-8
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@WebFilter(urlPatterns = "/*", dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD},
    asyncSupported = true, filterName = "EncoderFilter")
public class EncoderFilter implements Filter {
  private FilterConfig filterConfig = null;

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
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    request.setCharacterEncoding("UTF-8");
    chain.doFilter(request, response);
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

  /**
   * getter
   *
   * @return
   */
  public FilterConfig getFilterConfig() {
    return filterConfig;
  }

  /**
   * setter
   *
   * @param filterConfig
   */
  public void setFilterConfig(FilterConfig filterConfig) {
    this.filterConfig = filterConfig;
  }
}
