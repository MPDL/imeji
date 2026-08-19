package de.mpg.imeji.presentation.rewrite;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ocpsoft.common.util.Assert;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestHelper {

  private static final Logger LOGGER = LogManager.getLogger(RequestHelper.class);
  private static final Pattern JSESSIONID_PATTERN = Pattern.compile("(?i)^(.*);jsessionid=[\\w\\.\\-\\+]+(.*)");
  private final String contextPath;
  private final URI prettyRequestURL;
  private final URI originalRequestURL;
  private String requestQueryString;



  private final Map<String, List<String>> requestQueryParameters = new LinkedHashMap();

  RequestHelper(HttpServletRequest request) {
    Assert.notNull(request, "HttpServletRequest argument was null");

    this.contextPath = request.getContextPath();

    String requestUrl = request.getRequestURI();
    requestUrl = this.stripContextPath(requestUrl);
    Matcher sessionIdMatcher = JSESSIONID_PATTERN.matcher(requestUrl);
    if (sessionIdMatcher.matches()) {
      requestUrl = sessionIdMatcher.replaceFirst("$1$2");
    }
    String prettyRequestURL = requestUrl;


    if (request.getAttribute("jakarta.servlet.forward.request_uri") != null) {
      prettyRequestURL = request.getAttribute("jakarta.servlet.forward.request_uri").toString();
      prettyRequestURL = this.stripContextPath(prettyRequestURL);
      Matcher sessionIdMatcher2 = JSESSIONID_PATTERN.matcher(prettyRequestURL);
      if (sessionIdMatcher2.matches()) {
        prettyRequestURL = sessionIdMatcher.replaceFirst("$1$2");
      }
    }

    String encoding = request.getCharacterEncoding() == null ? "UTF-8" : request.getCharacterEncoding();
    try {
      this.originalRequestURL = new URI(requestUrl);
      this.prettyRequestURL = new URI(prettyRequestURL);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    this.requestQueryString = request.getQueryString() != null ? request.getQueryString() : "";
    if (!requestQueryString.isEmpty() && !requestQueryString.startsWith("?")) {
      this.requestQueryString = "?" + requestQueryString;
    }
    addParameters(requestQueryString);
  }

  public static RequestHelper getCurrentInstance() {
    FacesContext context = FacesContext.getCurrentInstance();
    return getCurrentInstance(context);
  }

  public static RequestHelper getCurrentInstance(FacesContext context) {
    Assert.notNull(context, "FacesContext argument was null.");
    return getCurrentInstance((HttpServletRequest) context.getExternalContext().getRequest());
  }

  public static RequestHelper getCurrentInstance(HttpServletRequest request) {
    Assert.notNull(request, "HttpServletRequest argument was null");
    return new RequestHelper(request);
  }

  public String stripContextPath(String uri) {
    if (!this.contextPath.equals("/") && uri.startsWith(this.contextPath)) {
      uri = uri.substring(this.contextPath.length());
    }

    return uri;
  }

  public void addParameters(String url) {
    if (url != null && !"".equals(url)) {
      url = url.trim();
      if (url.length() > 1) {
        if (url.contains("?")) {
          url = url.substring(url.indexOf(63) + 1);
        }

        String[] pairs = url.split("&(amp;)?");
        String[] var3 = pairs;
        int var4 = pairs.length;

        for (int var5 = 0; var5 < var4; ++var5) {
          String pair = var3[var5];
          int pos = pair.indexOf(61);
          String name;
          String value;
          if (pos == -1) {
            name = pair;
            value = null;
          } else {
            try {
              name = URLDecoder.decode(pair.substring(0, pos), "UTF-8");
              value = URLDecoder.decode(pair.substring(pos + 1, pair.length()), "UTF-8");
            } catch (IllegalArgumentException var11) {
              LOGGER.warn("Ignoring invalid query parameter: " + pair);
              continue;
            } catch (UnsupportedEncodingException var12) {
              throw new RuntimeException("UTF-8 encoding not supported. Something is seriously wrong with your environment.");
            }
          }

          List<String> list = (List) this.requestQueryParameters.get(name);
          if (list == null) {
            list = new ArrayList();
            this.requestQueryParameters.put(name, list);
          }

          ((List) list).add(value);
        }
      }
    }

  }

  public URI getPrettyRequestURL() {
    return prettyRequestURL;
  }

  public String getRequestQueryString() {
    return requestQueryString;
  }

  public Map<String, List<String>> getRequestQueryParameters() {
    return requestQueryParameters;
  }

  public URI getOriginalRequestURL() {
    return originalRequestURL;
  }
}
