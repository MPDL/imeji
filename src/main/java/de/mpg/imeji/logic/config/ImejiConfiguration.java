package de.mpg.imeji.logic.config;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.util.StringHelper;

public class ImejiConfiguration {

  /**
   * Enumeration of available configuration
   *
   * @author saquet (initial creation)
   * @author $Author$ (last modification)
   * @version $Revision$ $LastChangedDate$
   */
  private enum CONFIGURATION {
    SNIPPET, CSS_DEFAULT, CSS_ALT, MAX_FILE_SIZE, FILE_TYPES, STARTPAGE_HTML, DATA_VIEWER_FORMATS, DATA_VIEWER_URL, AUTOSUGGEST_USERS, AUTOSUGGEST_ORGAS, STARTPAGE_FOOTER_LOGOS, META_DESCRIPTION, INSTANCE_NAME, CONTACT_EMAIL, EMAIL_SERVER, EMAIL_SERVER_USER, EMAIL_SERVER_PASSWORD, EMAIL_SERVER_ENABLE_AUTHENTICATION, EMAIL_SERVER_SENDER, EMAIL_SERVER_PORT, STARTPAGE_CAROUSEL_ENABLED, STARTPAGE_CAROUSEL_QUERY, STARTPAGE_CAROUSEL_QUERY_ORDER, UPLOAD_WHITE_LIST, UPLOAD_BLACK_LIST, LANGUAGES, IMPRESSUM_URL, IMPRESSUM_TEXT, FAVICON_URL, LOGO, REGISTRATION_TOKEN_EXPIRY, REGISTRATION_ENABLED, DEFAULT_QUOTA, RSA_PUBLIC_KEY, RSA_PRIVATE_KEY, BROWSE_DEFAULT_VIEW, DOI_SERVICE_URL, DOI_USER, DOI_PASSWORD, QUOTA_LIMITS, PRIVATE_MODUS, REGISTRATION_WHITE_LIST, REGISTRATION_SNIPPET, ALBUMS_ENABLED, HELP_URL, MAINTENANCE_MESSAGE, TERMS_OF_USE, TERMS_OF_USE_URL;
  }

  private static Properties config;
  private static File configFile;
  private static ImejiFileTypes fileTypes;
  private static String lang = "en";
  private static final Logger LOGGER = Logger.getLogger(ImejiConfiguration.class);
  // A list of predefined file types, which is set when imeji is initialized
  private static final String DEFAULT_SEARCH_FILE_TYPE_LIST =
      "[Image@en,Bilder@de=jpg,jpeg,tiff,tiff,jp2,pbm,gif,png,psd][Video@en,Video@de=wmv,swf,rm,mp4,mpg,m4v,avi,mov.asf,flv,srt,vob][Audio@en,Ton@de=aif,iff,m3u,m4a,mid,mpa,mp3,ra,wav,wma][Document@en,Dokument@de=doc,docx,odt,pages,rtf,tex,rtf,bib,csv,ppt,pps,pptx,key,xls,xlr,xlsx,gsheet,nb,numbers,ods,indd,pdf,dtx]";
  private static final String DEFAULT_FILE_BLACKLIST =
      "386,aru,atm,aut,bat,bin,bkd,blf,bll,bmw,boo,bqf,buk,bxz,cc,ce0,ceo,cfxxe,chm,cih,cla,class,cmd,com,cpl,cxq,cyw,dbd,dev,dlb,dli,dll,dllx,dom,drv,dx,dxz,dyv,dyz,eml,exe,exe1,exe_renamed,ezt,fag,fjl,fnr,fuj,hlp,hlw,hsq,hts,ini,iva,iws,jar,js,kcd,let,lik,lkh,lnk,lok,mfu,mjz,nls,oar,ocx,osa,ozd,pcx,pgm,php2,php3,pid,pif,plc,pr,qit,rhk,rna,rsc_tmp,s7p,scr,scr,shs,ska,smm,smtmp,sop,spam,ssy,swf,sys,tko,tps,tsa,tti,txs,upa,uzy,vb,vba,vbe,vbs,vbx,vexe,vsd,vxd,vzr,wlpginstall,wmf,ws,wsc,wsf,wsh,wss,xdu,xir,xlv,xnt,zix,zvz";
  private static final String DEFAULT_LANGUAGE_LIST = "en,de,ja,es";
  private static final String DEFAULT_REGISTRATION_TOKEN_EXPIRATION_IN_DAYS = "1";
  private static final String DEFAULT_CAROUSEL_SHOW = "true";
  private static final String DEFAULT_USER_QUOTA = "1";
  private static final String DEFAULT_USER_QUOTA_LIST = "1, 10, 20";
  public static final String QUOTA_UNLIMITED = "unlimited";
  public static final String DEFAULT_ALBUMS_ENABLED = "true";
  public static final String DEFAULT_HELP_URL =
      "https://raw.githubusercontent.com/imeji-community/imeji-help/master/imeji-help-default.html";
  private String dataViewerUrl;

  public enum BROWSE_VIEW {
    LIST, THUMBNAIL;
  }

  private static final BROWSE_VIEW predefinedBrowseView = BROWSE_VIEW.THUMBNAIL;


  /**
   * Constructor, create the file if not existing
   *
   * @throws URISyntaxException
   * @throws IOException
   * @throws ImejiException
   */
  public ImejiConfiguration() throws IOException, URISyntaxException, ImejiException {
    getConfigurationFile();
    readConfig();
  }

  /**
   * Get the Configuration File from the filesystem. If not existing, create a new one with default
   * values
   *
   * @throws IOException
   * @throws URISyntaxException
   */
  private synchronized void getConfigurationFile() throws IOException, URISyntaxException {
    configFile = new File(PropertyReader.getProperty("imeji.tdb.path") + "/conf.xml");
    if (!configFile.exists()) {
      configFile.createNewFile();
      setDefaultConfig();
    }
  }


  /**
   * Write the Default Configuration to the Disk. This should be called when the Configuration is
   * initialized for the first time.
   */
  private synchronized void setDefaultConfig() {
    config = new Properties();
    fileTypes = new ImejiFileTypes(DEFAULT_SEARCH_FILE_TYPE_LIST);
    initPropertiesWithDefaultValue();
    saveConfig();
  }

  /**
   * Load the imeji configuration from a {@link File}
   *
   * @param f
   * @throws IOException
   * @throws ImejiException
   */
  private synchronized void readConfig() throws IOException, ImejiException {
    config = new Properties();
    try {
      FileInputStream in = new FileInputStream(configFile);
      config.loadFromXML(in);
    } catch (Exception e) {
      throw new ImejiException(
          "conf.xml could not be read. Please check in tdb directory if exsting and not empty. If Emtpy, remove it.");
    }
    dataViewerUrl = (String) config.get(CONFIGURATION.DATA_VIEWER_URL.name());
    fileTypes = new ImejiFileTypes((String) config.get(CONFIGURATION.FILE_TYPES.name()));
    initPropertiesWithDefaultValue();
  }

  private void initPropertiesWithDefaultValue() {
    initPropertyWithDefaultValue(CONFIGURATION.FILE_TYPES, fileTypes.toString());
    initPropertyWithDefaultValue(CONFIGURATION.UPLOAD_BLACK_LIST, DEFAULT_FILE_BLACKLIST);
    initPropertyWithDefaultValue(CONFIGURATION.LANGUAGES, DEFAULT_LANGUAGE_LIST);
    initPropertyWithDefaultValue(CONFIGURATION.BROWSE_DEFAULT_VIEW, predefinedBrowseView.name());
    initPropertyWithDefaultValue(CONFIGURATION.STARTPAGE_CAROUSEL_ENABLED, DEFAULT_CAROUSEL_SHOW);
    initPropertyWithDefaultValue(CONFIGURATION.DEFAULT_QUOTA, DEFAULT_USER_QUOTA);
    initPropertyWithDefaultValue(CONFIGURATION.QUOTA_LIMITS, DEFAULT_USER_QUOTA_LIST);
    initPropertyWithDefaultValue(CONFIGURATION.ALBUMS_ENABLED, DEFAULT_ALBUMS_ENABLED);
    initPropertyWithDefaultValue(CONFIGURATION.HELP_URL, DEFAULT_HELP_URL);
  }


  /**
   * Init a property with its default value if null or empty
   *
   * @param c
   * @param defaultValue
   */
  private void initPropertyWithDefaultValue(CONFIGURATION c, String defaultValue) {
    String currentValue = (String) config.get(c.name());
    if (currentValue != null && !"".equals(currentValue)) {
      setProperty(c.name(), currentValue);
    } else {
      setProperty(c.name(), defaultValue);
    }

  }

  /**
   * Save the configuration in the config file
   */
  public void saveConfig() {
    try {
      if (fileTypes != null) {
        setProperty(CONFIGURATION.FILE_TYPES.name(), fileTypes.toString());
      }
      if (dataViewerUrl != null) {
        setProperty(CONFIGURATION.DATA_VIEWER_URL.name(), dataViewerUrl);
      }
      config.storeToXML(new FileOutputStream(configFile), "imeji configuration File", "UTF-8");
      LOGGER.info("saving imeji config");
    } catch (Exception e) {
      LOGGER.error("Error saving configuration:", e);
    }
  }

  /**
   * Set the value of a configuration property, and save it on disk
   *
   * @param name
   * @param value
   */
  private void setProperty(String name, String value) {
    config.setProperty(name, value);
  }

  /**
   * Return a property as a non null String to avoid null pointer exception
   *
   * @param name
   * @return
   */
  private String getPropertyAsNonNullString(String name) {
    String v = (String) config.get(name);
    return v == null ? "" : v;
  }

  /**
   * Set the Snippet in the configuration
   *
   * @param str
   */
  public void setSnippet(String str) {
    setProperty(CONFIGURATION.SNIPPET.name(), str);
  }

  /**
   * Read the snippet from the configuration
   *
   * @return
   */
  public String getSnippet() {
    return (String) config.get(CONFIGURATION.SNIPPET.name());
  }

  /**
   * Set the url of the default CSS
   *
   * @param url
   */
  public void setDefaultCss(String url) {
    setProperty(CONFIGURATION.CSS_DEFAULT.name(), url);
  }

  /**
   * Return the url of the default CSS
   *
   * @return
   */
  public String getDefaultCss() {
    return (String) config.get(CONFIGURATION.CSS_DEFAULT.name());
  }

  /**
   * Set the url of the default CSS
   *
   * @param url
   */
  public void setAlternativeCss(String url) {
    setProperty(CONFIGURATION.CSS_ALT.name(), url);
  }

  /**
   * Return the url of the default CSS
   *
   * @return
   */
  public String getAlternativeCss() {
    return (String) config.get(CONFIGURATION.CSS_ALT.name());
  }

  /**
   * Set the url of the default CSS
   *
   * @param md_url
   */
  public void setUploadMaxFileSize(String size) {
    try {
      Integer.parseInt(size);
    } catch (Exception e) {
      setProperty(CONFIGURATION.MAX_FILE_SIZE.name(), "");
    }
    setProperty(CONFIGURATION.MAX_FILE_SIZE.name(), size);
  }

  /**
   * Return the url of the default CSS
   *
   * @return
   */
  public String getUploadMaxFileSize() {
    String size = (String) config.get(CONFIGURATION.MAX_FILE_SIZE.name());
    if (size == null || size.equals("")) {
      return "0";
    }
    return size;
  }

  /**
   * Get the type of Files
   *
   * @return
   */
  public ImejiFileTypes getFileTypes() {
    return fileTypes;
  }

  /**
   * Set the type of Files
   *
   * @param types
   */
  public void setFileTypes(ImejiFileTypes types) {
    setProperty(CONFIGURATION.FILE_TYPES.name(), fileTypes.toString());
  }

  /**
   * Get the html snippet for a specified lang
   *
   * @param lang
   * @return
   */
  public String getStartPageHTML(String lang) {
    String html = (String) config.get(CONFIGURATION.STARTPAGE_HTML.name() + "_" + lang);
    return html != null ? html : "";
  }

  /**
   * Get the html snippet for the footer of the startpage
   *
   * @return
   */
  public String getStartPageFooterLogos() {
    String html = (String) config.get(CONFIGURATION.STARTPAGE_FOOTER_LOGOS.name());
    return html != null ? html : "";
  }

  /**
   *
   * @param html
   */
  public void setStartPageFooterLogos(String html) {
    setProperty(CONFIGURATION.STARTPAGE_FOOTER_LOGOS.name(), html);
  }

  /**
   * Utility class to parse the html snippets
   *
   * @author saquet
   *
   */
  public class HtmlSnippet {
    private String html;
    private String lang;

    public HtmlSnippet(String lang, String html) {
      this.lang = lang;
      this.html = html;
    }

    public void listener(ValueChangeEvent event) {
      html = (String) event.getNewValue();
      setProperty(CONFIGURATION.STARTPAGE_HTML.name() + "_" + lang, html);
    }

    public String getLang() {
      return lang;
    }

    public void setLang(String lang) {
      this.lang = lang;
    }

    public String getHtml() {
      return html;
    }

    public void setHtml(String html) {
      this.html = html;
    }
  }

  /**
   * Read all the html snippets in the config and retunr it as a {@link List} {@link HtmlSnippet}
   *
   * @return
   */
  public List<HtmlSnippet> getSnippets(List<SelectItem> languages) {
    List<HtmlSnippet> snippets = new ArrayList<>();
    for (SelectItem lang : languages) {
      String html =
          (String) config.get(CONFIGURATION.STARTPAGE_HTML.name() + "_" + lang.getValue());
      snippets.add(new HtmlSnippet((String) lang.getValue(), html != null ? html : ""));
    }
    return snippets;
  }

  /**
   * @return the lang
   */
  public String getLang() {
    return lang;
  }

  /**
   * @param lang the lang to set
   */
  public void setLang(String s) {
    lang = s;
  }

  /**
   * @return the list of all formats supported by the data viewer service
   */
  public String getDataViewerFormatListString() {
    return config.getProperty(CONFIGURATION.DATA_VIEWER_FORMATS.name());
  }

  /**
   * @param str
   *
   */
  public void setDataViewerFormatListString(String str) {
    config.setProperty(CONFIGURATION.DATA_VIEWER_FORMATS.name(), str);

  }

  /**
   * true if the format is supported by the data viewer service
   *
   * @param format
   * @return
   */
  public boolean isDataViewerSupportedFormats(String format) {
    String l = getDataViewerFormatListString();
    if (l == null || "".equals(format)) {
      return false;
    }
    return l.contains(format);
  }

  /**
   * @return the url of the data viewer service
   */
  public String getDataViewerUrl() {
    return config.getProperty(CONFIGURATION.DATA_VIEWER_URL.name());
  }

  /**
   * @param str
   *
   */
  public void setDataViewerUrl(String str) {
    dataViewerUrl = str;
  }

  public String fetchDataViewerFormats() throws JSONException {
    String connURL;
    if (dataViewerUrl.endsWith("/")) {
      connURL = dataViewerUrl + "api/explain/formats";
    } else {
      connURL = dataViewerUrl + "/api/explain/formats";
    }
    // String connURL = dataViewerUrl + "/api/explain/formats";
    DefaultHttpClient httpclient = new DefaultHttpClient();
    HttpGet httpget = new HttpGet(connURL);
    HttpResponse resp;
    String str = "";
    try {
      resp = httpclient.execute(httpget);
      if (200 == resp.getStatusLine().getStatusCode()) {
        HttpEntity entity = resp.getEntity();
        if (entity != null) {
          String retSrc = EntityUtils.toString(entity);
          JSONArray array = new JSONArray(retSrc);
          int i = 0;
          while (i < array.length()) {
            str += array.get(i) + ", ";
            i++;
          }
        }
      }
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
    }
    setDataViewerFormatListString(str);
    return "";
  }

  public String getAutosuggestForOrganizations() {
    return config.getProperty(CONFIGURATION.AUTOSUGGEST_ORGAS.name());
  }

  public void setAutosuggestForOrganizations(String s) {
    config.setProperty(CONFIGURATION.AUTOSUGGEST_ORGAS.name(), s);

  }

  public String getAutoSuggestForUsers() {
    return config.getProperty(CONFIGURATION.AUTOSUGGEST_USERS.name());
  }

  public void setAutoSuggestForUsers(String s) {
    config.setProperty(CONFIGURATION.AUTOSUGGEST_USERS.name(), s);

  }

  /**
   * Set the meta description
   *
   * @param md_url
   */
  public void setMetaDescription(String s) {
    setProperty(CONFIGURATION.META_DESCRIPTION.name(), s);
  }

  /**
   * Return the meta description
   *
   * @return
   */
  public String getMetaDescription() {
    return (String) config.get(CONFIGURATION.META_DESCRIPTION.name());
  }

  /**
   * Set the name of the instance
   *
   * @param md_url
   */
  public void setInstanceName(String s) {
    setProperty(CONFIGURATION.INSTANCE_NAME.name(), s);
  }

  /**
   * Return the name of the instance
   *
   * @return
   */
  public String getInstanceName() {
    return getPropertyAsNonNullString(CONFIGURATION.INSTANCE_NAME.name());
  }

  /**
   * Set the contact email
   *
   * @param md_url
   */
  public void setContactEmail(String s) {
    setProperty(CONFIGURATION.CONTACT_EMAIL.name(), s);
  }

  /**
   * Return contact email
   *
   * @return
   */
  public String getContactEmail() {
    if ((String) config.get(CONFIGURATION.CONTACT_EMAIL.name()) != null) {
      return (String) config.get(CONFIGURATION.CONTACT_EMAIL.name());
    }
    return "";
  }

  public void setEmailServer(String s) {
    setProperty(CONFIGURATION.EMAIL_SERVER.name(), s);
  }

  public String getEmailServer() {
    return (String) config.get(CONFIGURATION.EMAIL_SERVER.name());
  }

  public void setEmailServerUser(String s) {
    setProperty(CONFIGURATION.EMAIL_SERVER_USER.name(), s);
  }

  public String getEmailServerUser() {
    return (String) config.get(CONFIGURATION.EMAIL_SERVER_USER.name());
  }

  public void setEmailServerPassword(String s) {
    setProperty(CONFIGURATION.EMAIL_SERVER_PASSWORD.name(), s);
  }

  public String getEmailServerPassword() {
    return (String) config.get(CONFIGURATION.EMAIL_SERVER_PASSWORD.name());
  }

  public void setEmailServerEnableAuthentication(boolean b) {
    setProperty(CONFIGURATION.EMAIL_SERVER_ENABLE_AUTHENTICATION.name(), Boolean.toString(b));
  }

  public boolean getEmailServerEnableAuthentication() {
    return Boolean
        .parseBoolean((String) config.get(CONFIGURATION.EMAIL_SERVER_ENABLE_AUTHENTICATION.name()));
  }

  public void setPrivateModus(boolean b) {
    setProperty(CONFIGURATION.PRIVATE_MODUS.name(), Boolean.toString(b));
  }

  public boolean getPrivateModus() {
    return Boolean.parseBoolean((String) config.get(CONFIGURATION.PRIVATE_MODUS.name()));
  }

  public void setEmailServerSender(String s) {
    setProperty(CONFIGURATION.EMAIL_SERVER_SENDER.name(), s);
  }

  public String getEmailServerSender() {
    return (String) config.get(CONFIGURATION.EMAIL_SERVER_SENDER.name());
  }

  public void setEmailServerPort(String s) {
    setProperty(CONFIGURATION.EMAIL_SERVER_PORT.name(), s);
  }

  public String getEmailServerPort() {
    return (String) config.get(CONFIGURATION.EMAIL_SERVER_PORT.name());
  }


  public void setStartPageCarouselQuery(String s) {
    setProperty(CONFIGURATION.STARTPAGE_CAROUSEL_QUERY.name(), s);
  }

  public String getStartPageCarouselQuery() {
    return (String) config.get(CONFIGURATION.STARTPAGE_CAROUSEL_QUERY.name());
  }

  public void setStartPageCarouselQueryOrder(String s) {
    setProperty(CONFIGURATION.STARTPAGE_CAROUSEL_QUERY_ORDER.name(), s);
  }

  public String getStartPageCarouselQueryOrder() {
    return (String) config.get(CONFIGURATION.STARTPAGE_CAROUSEL_QUERY_ORDER.name());
  }

  public void setUploadBlackList(String s) {
    setProperty(CONFIGURATION.UPLOAD_BLACK_LIST.name(), s);
  }

  public String getUploadBlackList() {
    return (String) config.get(CONFIGURATION.UPLOAD_BLACK_LIST.name());
  }

  public void setUploadWhiteList(String s) {
    setProperty(CONFIGURATION.UPLOAD_WHITE_LIST.name(), s);
  }

  public String getUploadWhiteList() {
    if (config.get(CONFIGURATION.UPLOAD_WHITE_LIST.name()) != null) {
      return (String) config.get(CONFIGURATION.UPLOAD_WHITE_LIST.name());
    }
    return "";
  }

  public String getLanguages() {
    return getPropertyAsNonNullString(CONFIGURATION.LANGUAGES.name());
  }

  public void setLanguages(String value) {
    setProperty(CONFIGURATION.LANGUAGES.name(), value);
  }

  public String getDoiUser() {
    return (String) config.get(CONFIGURATION.DOI_USER.name());
  }

  public void setDoiUser(String s) {
    setProperty(CONFIGURATION.DOI_USER.name(), s);
  }

  public String getDoiPassword() {
    return (String) config.get(CONFIGURATION.DOI_PASSWORD.name());
  }

  public void setDoiPassword(String s) {
    setProperty(CONFIGURATION.DOI_PASSWORD.name(), s);
  }

  public String getDoiServiceUrl() {
    return (String) config.get(CONFIGURATION.DOI_SERVICE_URL.name());
  }

  public void setDoiServiceUrl(String s) {
    setProperty(CONFIGURATION.DOI_SERVICE_URL.name(), s);
  }

  public void setImpressumUrl(String s) {
    setProperty(CONFIGURATION.IMPRESSUM_URL.name(), s);
  }

  public String getImpressumUrl() {
    return (String) config.get(CONFIGURATION.IMPRESSUM_URL.name());
  }

  public void setImpressumText(String s) {
    setProperty(CONFIGURATION.IMPRESSUM_TEXT.name(), s);
  }

  public String getImpressumText() {
    return (String) config.get(CONFIGURATION.IMPRESSUM_TEXT.name());
  }

  public void setFaviconUrl(String s) {
    setProperty(CONFIGURATION.FAVICON_URL.name(), s);
  }

  public void setRegistrationTokenExpiry(String s) {
    try {
      Integer.valueOf(s);
    } catch (NumberFormatException e) {
      LOGGER.info(
          "Could not understand the Registration Token Expiry Setting, setting it to default ("
              + DEFAULT_REGISTRATION_TOKEN_EXPIRATION_IN_DAYS + " day).");
      s = DEFAULT_REGISTRATION_TOKEN_EXPIRATION_IN_DAYS;
    }

    setProperty(CONFIGURATION.REGISTRATION_TOKEN_EXPIRY.name(), s);
  }


  public String getRegistrationTokenExpiry() {
    return registrationTokenCompute();
  }

  public boolean isRegistrationEnabled() {
    return Boolean.parseBoolean((String) config.get(CONFIGURATION.REGISTRATION_ENABLED.name()));
  }

  public void setRegistrationEnabled(boolean enabled) {
    config.setProperty(CONFIGURATION.REGISTRATION_ENABLED.name(), Boolean.toString(enabled));
  }

  /**
   * Return the url of the favicon
   *
   * @return
   */
  public String getFaviconUrl(String applicationUrl) {
    String myFavicon = (String) config.get(CONFIGURATION.FAVICON_URL.name());
    if (myFavicon == null || "".equals(myFavicon)) {
      return applicationUrl + "resources/icon/imeji.ico";
    } else {
      return (String) config.get(CONFIGURATION.FAVICON_URL.name());
    }
  }

  public void setLogoUrl(String s) {
    setProperty(CONFIGURATION.LOGO.name(), s);
  }

  /**
   * Return the url of the favicon
   *
   * @return
   */
  public String getLogoUrl() {
    return (String) config.get(CONFIGURATION.LOGO.name());
  }

  private String registrationTokenCompute() {
    String myToken = (String) config.get(CONFIGURATION.REGISTRATION_TOKEN_EXPIRY.name());
    return StringHelper.isNullOrEmptyTrim(myToken) ? DEFAULT_REGISTRATION_TOKEN_EXPIRATION_IN_DAYS
        : myToken;
  }

  public String getRsaPublicKey() {
    return (String) config.get(CONFIGURATION.RSA_PUBLIC_KEY.name());
  }

  public void setRsaPublicKey(String string) {
    config.put(CONFIGURATION.RSA_PUBLIC_KEY.name(), string);
  }

  public String getRsaPrivateKey() {
    return (String) config.get(CONFIGURATION.RSA_PRIVATE_KEY.name());
  }

  public void setRsaPrivateKey(String string) {
    config.put(CONFIGURATION.RSA_PRIVATE_KEY.name(), string);
  }

  public String getDefaultBrowseView() {
    return getPropertyAsNonNullString(CONFIGURATION.BROWSE_DEFAULT_VIEW.name());
  }

  public void setDefaultBrowseView(String string) {
    setProperty(CONFIGURATION.BROWSE_DEFAULT_VIEW.name(), BROWSE_VIEW.valueOf(string).name());
  }

  public void setQuotaLimits(String limits) {
    String[] limitArray = limits.split(",");
    for (int i = 0; i < limitArray.length; i++) {
      Double.parseDouble(limitArray[i]);
    }
    setProperty(CONFIGURATION.QUOTA_LIMITS.name(), limits);
  }

  public String getDefaultQuota() {
    return getPropertyAsNonNullString(CONFIGURATION.DEFAULT_QUOTA.name());
  }

  public void setdefaultQuota(String defaultQuota) {
    setProperty(CONFIGURATION.DEFAULT_QUOTA.name(), defaultQuota);
  }

  public String getQuotaLimits() {
    return (String) config.get(CONFIGURATION.QUOTA_LIMITS.name());
  }

  public List<String> getQuotaLimitsAsList() {
    String limitString =
        (String) config.get(CONFIGURATION.QUOTA_LIMITS.name()) + "," + QUOTA_UNLIMITED;
    return Arrays.asList(limitString.split(","));
  }

  public void setStartPageCarouselEnabled(boolean input) {
    setProperty(CONFIGURATION.STARTPAGE_CAROUSEL_ENABLED.name(), String.valueOf(input));
  }

  public boolean getStartPageCarouselEnabled() {
    return Boolean.valueOf(config.getProperty(CONFIGURATION.STARTPAGE_CAROUSEL_ENABLED.name()))
        && !getPrivateModus();
  }

  public String getRegistrationWhiteList() {
    return (String) config.get(CONFIGURATION.REGISTRATION_WHITE_LIST.name());
  }

  public void setRegistrationWhiteList(String s) {
    setProperty(CONFIGURATION.REGISTRATION_WHITE_LIST.name(), s);
  }

  public boolean getAlbumsEnabled() {
    return Boolean.parseBoolean((String) config.get(CONFIGURATION.ALBUMS_ENABLED.name()));
  }

  public void setAlbumsEnabled(boolean enabled) {
    setProperty(CONFIGURATION.ALBUMS_ENABLED.name(), Boolean.toString(enabled));
  }

  public void setHelpUrl(String url) {
    setProperty(CONFIGURATION.HELP_URL.name(), url);
  }

  public String getHelpUrl() {
    return (String) config.get(CONFIGURATION.HELP_URL.name());
  }

  public void setRegistrationSnippet(String url) {
    setProperty(CONFIGURATION.REGISTRATION_SNIPPET.name(), url);
  }

  public String getRegistrationSnippet() {
    return (String) config.get(CONFIGURATION.REGISTRATION_SNIPPET.name());
  }

  public void setMaintenanceMessage(String message) {
    setProperty(CONFIGURATION.MAINTENANCE_MESSAGE.name(), message);
  }

  public String getMaintenanceMessage() {
    return (String) config.get(CONFIGURATION.MAINTENANCE_MESSAGE.name());
  }

  public void setTermsOfUse(String s) {
    setProperty(CONFIGURATION.TERMS_OF_USE.name(), s);
  }

  public String getTermsOfUse() {
    return (String) config.get(CONFIGURATION.TERMS_OF_USE.name());
  }

  public void setTermsOfUseUrl(String s) {
    setProperty(CONFIGURATION.TERMS_OF_USE_URL.name(), s);
  }

  public String getTermsOfUseUrl() {
    return (String) config.get(CONFIGURATION.TERMS_OF_USE_URL.name());
  }
}
