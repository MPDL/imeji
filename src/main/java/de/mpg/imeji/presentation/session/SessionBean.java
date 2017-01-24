/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.session;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.ImejiConfiguration;
import de.mpg.imeji.logic.config.ImejiConfiguration.BROWSE_VIEW;
import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.controller.AlbumController;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.util.MaxPlanckInstitutUtils;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.util.CookieUtils;
import de.mpg.imeji.presentation.util.ServletUtil;

/**
 * The session Bean for imeji.
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "SessionBean")
@SessionScoped
public class SessionBean implements Serializable {
  private static final long serialVersionUID = 3367867290955569762L;
  private static final String STYLE_COOKIE = "IMEJI_STYLE";
  private static final String BROWSE_VIEW_COOKIE = "IMEJI_BROWSE_VIEW";

  private enum Style {
    NONE, DEFAULT, ALTERNATIVE;
  }

  private User user = null;
  private List<String> selected = new ArrayList<String>();;
  private Set<String> clipboard = new HashSet<>();
  private Album activeAlbum;
  private String selectedImagesContext = null;
  private Style selectedCss = Style.NONE;
  private String applicationUrl;
  private String selectedBrowseListView;


  /*
   * Specific variables for the Max Planck Institute
   */
  public String institute;
  public String instituteId;

  /**
   * The session Bean for imeji
   */
  public SessionBean() {
    initCssWithCookie();
    initApplicationUrl();
    initBrowseViewWithCookieOrConfig();
    institute = findInstitute();
    instituteId = findInstituteId();
  }


  /**
   * Init the default browse view. If a cookie is set, use it, otherwise use config value
   */
  private void initBrowseViewWithCookieOrConfig() {
    this.selectedBrowseListView =
        CookieUtils.readNonNull(BROWSE_VIEW_COOKIE, Imeji.CONFIG.getDefaultBrowseView());
  }

  /**
   * Initialize the CSS value with the Cookie value
   */
  private void initCssWithCookie() {
    selectedCss = Style.valueOf(CookieUtils.readNonNull(STYLE_COOKIE, Style.NONE.name()));
  }

  /**
   * Return the version of the software
   *
   * @return
   */
  public String getVersion() {
    return PropertyReader.getVersion();
  }

  /**
   * Return the name of the current application (defined in the property)
   *
   * @return
   * @throws URISyntaxException
   * @throws IOException
   */
  public String getInstanceName() {
    try {
      return Imeji.CONFIG.getInstanceName();
    } catch (final Exception e) {
      return "imeji";
    }
  }

  /**
   * Read application URL from the imeji properties
   */
  private void initApplicationUrl() {
    try {
      applicationUrl = StringHelper.normalizeURI(PropertyReader.getProperty("imeji.instance.url"));
    } catch (final Exception e) {
      applicationUrl = "http://localhost:8080/imeji";
    }
  }

  public String getApplicationUrl() {
    return applicationUrl;
  }

  /**
   * Get the context of the images (item, collection, album)
   *
   * @return
   */
  public String getSelectedImagesContext() {
    return selectedImagesContext;
  }

  /**
   * setter
   *
   * @param selectedImagesContext
   */
  public void setSelectedImagesContext(String selectedImagesContext) {
    this.selectedImagesContext = selectedImagesContext;
  }

  public void reloadUser() throws Exception {
    if (user != null) {
      user = new UserService().retrieve(user.getId(), Imeji.adminUser);
    }
  }

  /**
   * @return the user
   */
  public User getUser() {
    return user;
  }

  /**
   * @param user the user to set
   */
  public void setUser(User user) {
    this.user = user;
  }

  /**
   * getter
   *
   * @return
   */
  public List<String> getSelected() {
    return selected;
  }

  /**
   * setter
   *
   * @param selected
   */
  public void setSelected(List<String> selected) {
    this.selected = selected;
  }

  /**
   * Return the number of item selected
   *
   * @return
   */
  public int getSelectedSize() {
    return selected.size();
  }


  /**
   * setter
   *
   * @param activeAlbum
   */
  public void setActiveAlbum(Album activeAlbum) {
    this.activeAlbum = activeAlbum;
  }

  /**
   * getter
   *
   * @return
   */
  public Album getActiveAlbum() {
    if (activeAlbum != null && (!SecurityUtil.authorization().read(getUser(), activeAlbum.getId())
        || !SecurityUtil.authorization().create(getUser(), activeAlbum.getId()))) {
      setActiveAlbum(null);
    }
    return activeAlbum;
  }

  /**
   * Make the passed album active
   *
   * @param albumId
   * @throws ImejiException
   */
  public String activateAlbum(String albumId) throws ImejiException {
    this.activeAlbum = new AlbumController().retrieve(URI.create(albumId), user);
    return "pretty:";
  }

  /**
   * Deactivate the album
   */
  public String deactivateAlbum() {
    this.activeAlbum = null;
    return "pretty:";
  }

  /**
   * setter
   *
   * @return
   */
  public String getActiveAlbumId() {
    return ObjectHelper.getId(activeAlbum.getId());
  }

  /**
   * getter
   *
   * @return
   */
  public int getActiveAlbumSize() {
    return activeAlbum.getImages().size();
  }



  /**
   * Check if the selected CSS is correct according to the configuration value. If errors are found,
   * then change the selected CSS
   *
   * @param defaultCss - the value of the default css in the config
   * @param alternativeCss - the value of the alternative css in the config
   */
  public void checkCss(String defaultCss, String alternativeCss) {
    if (selectedCss == Style.ALTERNATIVE && (alternativeCss == null || "".equals(alternativeCss))) {
      // alternative css doesn't exist, therefore set to default
      selectedCss = Style.DEFAULT;
    }
    if (selectedCss == Style.DEFAULT && (defaultCss == null || "".equals(defaultCss))) {
      // default css doesn't exist, therefore set to none
      selectedCss = Style.NONE;
    }
    if (selectedCss == Style.NONE && defaultCss != null && !"".equals(defaultCss)) {
      // default css exists, therefore set to default
      selectedCss = Style.DEFAULT;
    }
  }

  /**
   * Get the the selected {@link Style}
   *
   * @return
   * @throws URISyntaxException
   * @throws IOException
   */
  public String getSelectedCss() {
    return selectedCss.name();
  }

  /**
   * Toggle the selected css
   *
   * @return
   */
  public void toggleCss() {
    selectedCss = selectedCss == Style.DEFAULT ? Style.ALTERNATIVE : Style.DEFAULT;
    CookieUtils.updateCookieValue(STYLE_COOKIE, selectedCss.name());
  }

  /**
   * Return the Institute of the current {@link User} according to his IP. IMPORTANT: works only for
   * Max Planck Institutes IPs.
   *
   * @return
   */
  public String getInstituteNameByIP() {
    if (StringUtils.isEmpty(institute)) {
      return "unknown";
    }
    return institute;
  }

  /**
   * Return the Institute of the current {@link User} according to his IP. IMPORTANT: works only for
   * Max Planck Institutes IPs.
   *
   * @return
   */
  public String getInstituteIdByIP() {
    if (StringUtils.isEmpty(institute)) {
      return "unknown";
    }
    return instituteId;
  }

  /**
   * Return the suffix of the email of the user
   *
   * @return
   */
  public String getInstituteByUser() {
    if (user != null) {
      return user.getEmail().split("@")[1];
    }
    return "";
  }

  /**
   * Find the Name of the Institute of the current user
   */
  public String findInstitute() {
    if (institute != null) {
      return institute;
    }
    return MaxPlanckInstitutUtils.getInstituteNameForIP(readUserIp());
  }

  /**
   * Find the Name of the Institute of the current user
   */
  public String findInstituteId() {
    if (instituteId != null) {
      return instituteId;
    }
    return MaxPlanckInstitutUtils.getInstituteIdForIP(readUserIp());
  }

  /**
   * Read the IP of the current User
   *
   * @return
   */
  private String readUserIp() {
    final HttpServletRequest request =
        (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    String ipAddress = request.getHeader("X-FORWARDED-FOR");
    if (ipAddress == null) {
      ipAddress = request.getRemoteAddr();
    }
    if (ipAddress != null && ipAddress.split(",").length > 1) {
      ipAddress = ipAddress.split(",")[0];
    }
    return ipAddress;
  }

  /**
   * Logout and redirect to the home page
   *
   * @throws IOException
   */
  private void logoutFromSpot() {
    if (getUser() != null && !SecurityUtil.authorization().isSysAdmin(user)) {
      setUser(null);
    }
  }

  public String getSelectedBrowseListView() {
    return selectedBrowseListView;
  }

  public void setSelectedBrowseListView(String selectedBrowseListView) {
    this.selectedBrowseListView = selectedBrowseListView;
  }

  public void toggleBrowseView() {
    selectedBrowseListView =
        selectedBrowseListView.equals(ImejiConfiguration.BROWSE_VIEW.LIST.name())
            ? BROWSE_VIEW.THUMBNAIL.name() : BROWSE_VIEW.LIST.name();
    CookieUtils.updateCookieValue(BROWSE_VIEW_COOKIE, selectedBrowseListView);
  }

  /**
   * Return the {@link SessionBean} form the {@link HttpSession}
   *
   * @param req
   * @return
   */
  public static SessionBean getSessionBean(HttpServletRequest req) {
    return (SessionBean) ServletUtil.getSession(req, SessionBean.class.getSimpleName());
  }


  /**
   * @return the clipboard
   */
  public Set<String> getClipboard() {
    return clipboard;
  }


  /**
   * @param clipboard the clipboard to set
   */
  public void setClipboard(Set<String> clipboard) {
    this.clipboard = clipboard;
  }

  /**
   * Add selected items to the clipboard
   */
  public void addToClipBoard() {
    this.clipboard.addAll(selected);
  }

  /**
   * Clear the clipbord
   */
  public void clearClipboard() {
    this.clipboard.clear();
  }
}
