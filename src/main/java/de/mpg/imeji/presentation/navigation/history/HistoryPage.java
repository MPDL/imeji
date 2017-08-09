package de.mpg.imeji.presentation.navigation.history;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.vo.User;

/**
 * An imeji web page
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class HistoryPage implements Serializable {
  private static final long serialVersionUID = -6620054520723398563L;
  private int pos = 0;
  private String url;
  private String title = "";
  private final ImejiPages imejiPage;
  private final Map<String, String[]> params;


  /**
   * Constructor a an {@link HistoryPage}
   *
   * @param url
   * @param params
   * @param user
   * @throws Exception
   */
  public HistoryPage(String url, Map<String, String[]> params, User user) throws Exception {
    this.params = new HashMap<>(params);
    this.url = url;
    this.imejiPage = HistoryUtil.getImejiPage(getCompleteUrl());
  }

  private HistoryPage(String url, Map<String, String[]> params, String title,
      ImejiPages imejiPage) {
    this.params = params;
    this.url = url;
    this.imejiPage = imejiPage;
    this.title = title;
  }

  /**
   * Return a new instance of the same page
   */
  public HistoryPage copy() {
    return new HistoryPage(new String(url), new HashMap<>(params), new String(title), imejiPage);
  }


  /**
   * Compares 2 {@link HistoryPage}
   *
   * @param page
   * @return
   */
  public boolean isSame(HistoryPage page) {
    if (isNull() || page == null || page.isNull()) {
      return false;
    } else if (isNull() && page.isNull()) {
      return true;
    } else {
      return url.equals(page.url);
    }
  }

  public boolean isNull() {
    // return (type == null && uri == null);
    return url == null;
  }

  public String getInternationalizedName(Locale locale) {
    try {
      final String inter = Imeji.RESOURCE_BUNDLE.getLabel(imejiPage.getLabel(), locale);
      return title != null ? inter + " " + title : inter;
    } catch (final Exception e) {
      return imejiPage.getLabel();
    }
  }

  /**
   * Set a parameter (for instance q) with a new value. To get it as an RUL, call getCompleteUrl()
   *
   * @param param
   * @param value
   */
  public void setParamValue(String param, String value) {
    final String[] valueArray = {value};
    params.put(param, valueArray);
  }

  public int getPos() {
    return pos;
  }

  public void setPos(int pos) {
    this.pos = pos;
  }

  public String getCompleteUrlWithHistory() {
    final String delim = HistoryUtil.paramsMapToString(params).isEmpty() ? "?" : "&";
    return getCompleteUrl() + delim + "h=" + pos;
  }

  public String getCompleteUrl() {
    return url + HistoryUtil.paramsMapToString(params);
  }

  public String getUrl() {
    return url;
  }

  public String getUrlEncoded() throws UnsupportedEncodingException {
    return URLEncoder.encode(getCompleteUrl(), "UTF-8");
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public ImejiPages getImejiPage() {
    return imejiPage;
  }
}
