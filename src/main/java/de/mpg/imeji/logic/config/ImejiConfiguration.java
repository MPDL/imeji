package de.mpg.imeji.logic.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.faces.event.ValueChangeEvent;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
		SNIPPET, CSS_DEFAULT, CSS_ALT, MAX_FILE_SIZE, FILE_TYPES, STARTPAGE_HTML, DATA_VIEWER_FORMATS, DATA_VIEWER_URL, AUTOSUGGEST_USERS, AUTOSUGGEST_ORGAS, STARTPAGE_FOOTER_LOGOS, META_DESCRIPTION, META_AUTHOR, INSTANCE_NAME, CONTACT_EMAIL, EMAIL_SERVER, EMAIL_SERVER_USER, EMAIL_SERVER_PASSWORD, EMAIL_SERVER_ENABLE_AUTHENTICATION, EMAIL_SERVER_SENDER, EMAIL_SERVER_PORT, UPLOAD_WHITE_LIST, UPLOAD_BLACK_LIST, LANGUAGES, IMPRESSUM_URL, IMPRESSUM_TEXT, FAVICON_URL, LOGO, REGISTRATION_TOKEN_EXPIRY, REGISTRATION_ENABLED, DEFAULT_QUOTA, RSA_PUBLIC_KEY, RSA_PRIVATE_KEY, BROWSE_DEFAULT_VIEW, DOI_SERVICE_URL, DOI_USER, DOI_PASSWORD, QUOTA_LIMITS, PRIVATE_MODUS, REGISTRATION_WHITE_LIST, REGISTRATION_SNIPPET, HELP_URL, MAINTENANCE_MESSAGE, TERMS_OF_USE, TERMS_OF_USE_URL, DEFAULT_LICENSE, TECHNICAL_METADATA, THUMBNAIL_WIDTH, WEB_RESOLUTION_WIDTH, STATEMENTS, NUMBER_OF_LINES_IN_THUMBNAIL_LIST, GOOGLE_MAPS_API, CONE_AUTHORS, DOI_PUBLISHER, FACET_DISPLAYED, PRIVACY_POLICY, PRIVACY_POLICY_URL;
	}

	private static Properties config;
	private static File configFile;
	private static ImejiFileTypes fileTypes;
	private static String lang = "en";
	private static final Logger LOGGER = LogManager.getLogger(ImejiConfiguration.class);

	// Default configuration values that are set when there are no values in
	// config.xml
	// A list of predefined file types, which is set when imeji is initialized
	private static final String DEFAULT_SEARCH_FILE_TYPE_LIST = "[Image@en,Bilder@de=jpg,jpeg,tiff,tiff,jp2,pbm,gif,png,psd][Video@en,Video@de=wmv,swf,rm,mp4,mpg,m4v,avi,mov.asf,flv,srt,vob][Audio@en,Ton@de=aif,iff,m3u,m4a,mid,mpa,mp3,ra,wav,wma][Document@en,Dokument@de=doc,docx,odt,pages,rtf,tex,rtf,bib,csv,ppt,pps,pptx,key,xls,xlr,xlsx,gsheet,nb,numbers,ods,indd,pdf,dtx]";
	private static final String DEFAULT_FILE_BLACKLIST = "386,aru,atm,aut,bat,bin,bkd,blf,bll,bmw,boo,bqf,buk,bxz,cc,ce0,ceo,cfxxe,chm,cih,cla,class,cmd,com,cpl,cxq,cyw,dbd,dev,dlb,dli,dll,dllx,dom,drv,dx,dxz,dyv,dyz,eml,exe,exe1,exe_renamed,ezt,fag,fjl,fnr,fuj,hlp,hlw,hsq,hts,ini,iva,iws,jar,js,kcd,let,lik,lkh,lnk,lok,mfu,mjz,nls,oar,ocx,osa,ozd,pcx,pgm,php2,php3,pid,pif,plc,pr,qit,rhk,rna,rsc_tmp,s7p,scr,scr,shs,ska,smm,smtmp,sop,spam,ssy,swf,sys,tko,tps,tsa,tti,txs,upa,uzy,vb,vba,vbe,vbs,vbx,vexe,vsd,vxd,vzr,wlpginstall,wmf,ws,wsc,wsf,wsh,wss,xdu,xir,xlv,xnt,zix,zvz";
	private static final String DEFAULT_LANGUAGE_LIST = "en,de,ja,es";
	private static final String DEFAULT_REGISTRATION_TOKEN_EXPIRATION_IN_DAYS = "1";
	private static final String DEFAULT_USER_QUOTA = "1";
	private static final String DEFAULT_USER_QUOTA_LIST = "1, 10, 20";
	public static final String QUOTA_UNLIMITED = "unlimited";
	public static final String DEFAULT_DOI_PUBLISHER = "Max Planck Society";
	public static final String DEFAULT_HELP_URL = "https://raw.githubusercontent.com/imeji-community/imeji-help/master/imeji-help-default.html";
	public static final String DEFAULT_TECHNICAL_METADATA = "Make,Model,Exposure Time,Date/Time Digitized,Color Space,Artist,Model";
	public static final String DEFAULT_THUMBNAIL_WIDTH = "103";
	public static final String DEFAULT_PREVIEW_WIDTH = "357";
	public static final String DEFAULT_NUMBER_OF_LINES_IN_THUMBNAIL_LIST = "1,2,3,4,10,15,40";
	public static final String DEFAULT_GOOGLE_MAPS_API = "https://maps.googleapis.com/maps/api/geocode/json?sensor=false&address=";
	public static final String DEFAULT_CONE_AUTHORS = "http://pubman.mpdl.mpg.de/cone/persons/query?format=json&n=10&m=full&q=";

	private String dataViewerUrl;

	private ProtectedPassword emailServerPassword;
	private ProtectedPassword doiPassword;

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
	 * Get the Configuration File from the filesystem. If not existing, create a new
	 * one with default values
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
	 * Write the Default Configuration to the Disk. This should be called when the
	 * Configuration is initialized for the first time.
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
			final FileInputStream in = new FileInputStream(configFile);
			config.loadFromXML(in);
		} catch (final Exception e) {
			throw new ImejiException(
					"conf.xml could not be read. Please check in tdb directory if exsting and not empty. If Emtpy, remove it.");
		}
		dataViewerUrl = (String) config.get(CONFIGURATION.DATA_VIEWER_URL.name());
		fileTypes = new ImejiFileTypes((String) config.get(CONFIGURATION.FILE_TYPES.name()));
		initPropertiesWithDefaultValue();
		initProtectedPasswords();
	}

	private void initProtectedPasswords() {
		this.emailServerPassword = new EmailServerPassword(
				(String) config.get(CONFIGURATION.EMAIL_SERVER_PASSWORD.name()));
		this.doiPassword = new DoiPassword((String) config.get(CONFIGURATION.DOI_PASSWORD.name()));
	}

	private void initPropertiesWithDefaultValue() {
		initPropertyWithDefaultValue(CONFIGURATION.FILE_TYPES, fileTypes.toString());
		initPropertyWithDefaultValue(CONFIGURATION.UPLOAD_BLACK_LIST, DEFAULT_FILE_BLACKLIST);
		initPropertyWithDefaultValue(CONFIGURATION.LANGUAGES, DEFAULT_LANGUAGE_LIST);
		initPropertyWithDefaultValue(CONFIGURATION.BROWSE_DEFAULT_VIEW, predefinedBrowseView.name());
		initPropertyWithDefaultValue(CONFIGURATION.DEFAULT_QUOTA, DEFAULT_USER_QUOTA);
		initPropertyWithDefaultValue(CONFIGURATION.QUOTA_LIMITS, DEFAULT_USER_QUOTA_LIST);
		initPropertyWithDefaultValue(CONFIGURATION.HELP_URL, DEFAULT_HELP_URL);
		initPropertyWithDefaultValue(CONFIGURATION.TECHNICAL_METADATA, DEFAULT_TECHNICAL_METADATA);
		initPropertyWithDefaultValue(CONFIGURATION.THUMBNAIL_WIDTH, DEFAULT_THUMBNAIL_WIDTH);
		initPropertyWithDefaultValue(CONFIGURATION.WEB_RESOLUTION_WIDTH, DEFAULT_PREVIEW_WIDTH);
		initPropertyWithDefaultValue(CONFIGURATION.NUMBER_OF_LINES_IN_THUMBNAIL_LIST,
				DEFAULT_NUMBER_OF_LINES_IN_THUMBNAIL_LIST);
		initPropertyWithDefaultValue(CONFIGURATION.GOOGLE_MAPS_API, DEFAULT_GOOGLE_MAPS_API);
		initPropertyWithDefaultValue(CONFIGURATION.CONE_AUTHORS, DEFAULT_CONE_AUTHORS);
	}

	/**
	 * Init a property with its default value if null or empty
	 *
	 * @param c
	 * @param defaultValue
	 */
	private void initPropertyWithDefaultValue(CONFIGURATION c, String defaultValue) {
		final String currentValue = (String) config.get(c.name());
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
		} catch (final Exception e) {
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
		final String v = (String) config.get(name);
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
		} catch (final Exception e) {
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
		final String size = (String) config.get(CONFIGURATION.MAX_FILE_SIZE.name());
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
		final String html = (String) config.get(CONFIGURATION.STARTPAGE_HTML.name() + "_" + lang);
		return html != null ? html : "";
	}

	/**
	 * Get the html snippet for the footer of the startpage
	 *
	 * @return
	 */
	public String getStartPageFooterLogos() {
		final String html = (String) config.get(CONFIGURATION.STARTPAGE_FOOTER_LOGOS.name());
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
	 * Read all the html snippets in the config and retunr it as a {@link List}
	 * {@link HtmlSnippet}
	 *
	 * @return
	 */
	public List<HtmlSnippet> getSnippets(List<String> languages) {
		final List<HtmlSnippet> snippets = new ArrayList<>();
		for (final String lang : languages) {
			final String html = (String) config.get(CONFIGURATION.STARTPAGE_HTML.name() + "_" + lang);
			snippets.add(new HtmlSnippet((String) lang, html != null ? html : ""));
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
	 * @param lang
	 *            the lang to set
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
		final String l = getDataViewerFormatListString();
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
		final DefaultHttpClient httpclient = new DefaultHttpClient();
		final HttpGet httpget = new HttpGet(connURL);
		HttpResponse resp;
		String str = "";
		try {
			resp = httpclient.execute(httpget);
			if (200 == resp.getStatusLine().getStatusCode()) {
				final HttpEntity entity = resp.getEntity();
				if (entity != null) {
					final String retSrc = EntityUtils.toString(entity);
					final JSONArray array = new JSONArray(retSrc);
					int i = 0;
					while (i < array.length()) {
						str += array.get(i) + ", ";
						i++;
					}
				}
			}
		} catch (final Exception e) {
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
	 * 
	 * @param s
	 */
	public void setMetaAuthor(String s) {
		setProperty(CONFIGURATION.META_AUTHOR.name(), s);
	}

	/**
	 * Return the meta description
	 *
	 * @return
	 */
	public String getMetaAuthor() {
		return (String) config.get(CONFIGURATION.META_AUTHOR.name());
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

	public void setProtectedEmailServerPassword(ProtectedPassword emailServerPassword) {
		setProperty(CONFIGURATION.EMAIL_SERVER_PASSWORD.name(), this.emailServerPassword.getPassword());
	}

	public ProtectedPassword getProtectedEmailServerPassword() {
		return this.emailServerPassword;
	}

	public void setEmailServerEnableAuthentication(boolean b) {
		setProperty(CONFIGURATION.EMAIL_SERVER_ENABLE_AUTHENTICATION.name(), Boolean.toString(b));
	}

	public boolean getEmailServerEnableAuthentication() {
		return Boolean.parseBoolean((String) config.get(CONFIGURATION.EMAIL_SERVER_ENABLE_AUTHENTICATION.name()));
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

	/**
	 * Get imeji instance languages as list of Strings
	 * 
	 * @return
	 */
	public LinkedList<String> getLanguagesAsList() {

		LinkedList<String> systemLanguages = new LinkedList<String>();
		String allSystemLanguages = getLanguages();
		if (!allSystemLanguages.isEmpty()) {
			String[] languages = allSystemLanguages.split(",");
			for (int i = 0; i < languages.length; i++) {
				systemLanguages.add(languages[i]);
			}
		}
		return systemLanguages;
	}

	/**
	 * Get imeji instance languages ','-separated in a String
	 * 
	 * @return
	 */
	public String getLanguages() {
		return getPropertyAsNonNullString(CONFIGURATION.LANGUAGES.name());
	}

	public void setLanguages(String value) {
		setProperty(CONFIGURATION.LANGUAGES.name(), value);
	}

	public String getDoiPublisher() {
		if (StringHelper.isNullOrEmptyTrim(config.get(CONFIGURATION.DOI_PUBLISHER.name()))) {
			return DEFAULT_DOI_PUBLISHER;
		}
		return (String) config.get(CONFIGURATION.DOI_PUBLISHER.name());
	}

	public void setDoiPublisher(String s) {
		setProperty(CONFIGURATION.DOI_PUBLISHER.name(), s);
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

	public void setProtectedDoiPassword(ProtectedPassword emailServerPassword) {
		setProperty(CONFIGURATION.DOI_PASSWORD.name(), this.doiPassword.getPassword());
	}

	public ProtectedPassword getProtectedDoiPassword() {
		return this.doiPassword;
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
		} catch (final NumberFormatException e) {
			LOGGER.info("Could not understand the Registration Token Expiry Setting, setting it to default ("
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
		final String myFavicon = (String) config.get(CONFIGURATION.FAVICON_URL.name());
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
		final String myToken = (String) config.get(CONFIGURATION.REGISTRATION_TOKEN_EXPIRY.name());
		return StringHelper.isNullOrEmptyTrim(myToken) ? DEFAULT_REGISTRATION_TOKEN_EXPIRATION_IN_DAYS : myToken;
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
		final String[] limitArray = limits.split(",");
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
		final String limitString = (String) config.get(CONFIGURATION.QUOTA_LIMITS.name()) + "," + QUOTA_UNLIMITED;
		return Arrays.asList(limitString.split(","));
	}

	public String getRegistrationWhiteList() {
		return (String) config.get(CONFIGURATION.REGISTRATION_WHITE_LIST.name());
	}

	public void setRegistrationWhiteList(String s) {
		setProperty(CONFIGURATION.REGISTRATION_WHITE_LIST.name(), s);
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

	public void setPrivacyPolicy(String s) {
		setProperty(CONFIGURATION.PRIVACY_POLICY.name(), s);
	}

	public String getPrivacyPolicy() {
		return (String) config.get(CONFIGURATION.PRIVACY_POLICY.name());
	}

	public void setPrivacyPolicyUrl(String s) {
		setProperty(CONFIGURATION.PRIVACY_POLICY_URL.name(), s);
	}

	public String getPrivacyPolicyUrl() {
		return (String) config.get(CONFIGURATION.PRIVACY_POLICY_URL.name());
	}

	public String getDefaultLicense() {
		return (String) config.get(CONFIGURATION.DEFAULT_LICENSE.name());
	}

	public void setDefaultLicense(String licenseName) {
		setProperty(CONFIGURATION.DEFAULT_LICENSE.name(), licenseName);
	}

	public String getTechnicalMetadata() {
		return (String) config.get(CONFIGURATION.TECHNICAL_METADATA.name());
	}

	public void setTechnicalMetadata(String technicalMetadata) {
		setProperty(CONFIGURATION.TECHNICAL_METADATA.name(), technicalMetadata);
	}

	public String getThumbnailWidth() {
		return getPropertyAsNonNullString(CONFIGURATION.THUMBNAIL_WIDTH.name());
	}

	public void setThumbnailWidth(String thumbnailWidth) {
		setProperty(CONFIGURATION.THUMBNAIL_WIDTH.name(), thumbnailWidth);
	}

	public String getWebResolutionWidth() {
		return getPropertyAsNonNullString(CONFIGURATION.WEB_RESOLUTION_WIDTH.name());
	}

	public void setWebResolutionWidth(String webResolutionWidth) {
		setProperty(CONFIGURATION.WEB_RESOLUTION_WIDTH.name(), webResolutionWidth);
	}

	public String getStatements() {
		return getPropertyAsNonNullString(CONFIGURATION.STATEMENTS.name());
	}

	public void setStatements(String s) {
		setProperty(CONFIGURATION.STATEMENTS.name(), s);
	}

	public String getNumberOfLinesInThumbnailList() {
		return getPropertyAsNonNullString(CONFIGURATION.NUMBER_OF_LINES_IN_THUMBNAIL_LIST.name());
	}

	public void setNumberOFLinesInThumbnailList(String s) {
		setProperty(CONFIGURATION.NUMBER_OF_LINES_IN_THUMBNAIL_LIST.name(), s);
	}

	public String getGoogleMapsApi() {
		return getPropertyAsNonNullString(CONFIGURATION.GOOGLE_MAPS_API.name());
	}

	public void setGoogleMapsApi(String url) {
		setProperty(CONFIGURATION.GOOGLE_MAPS_API.name(), url);
	}

	public String getConeAuthors() {
		return getPropertyAsNonNullString(CONFIGURATION.CONE_AUTHORS.name());
	}

	public void setConeAuthors(String url) {
		setProperty(CONFIGURATION.CONE_AUTHORS.name(), url);
	}

	public String getFacetDisplayed() {
		String str = getPropertyAsNonNullString(CONFIGURATION.FACET_DISPLAYED.name());
		if (NumberUtils.isNumber(str)) {
			return str;
		}
		return "-1";
	}

	public void setFacetDisplayed(String str) {
		setProperty(CONFIGURATION.FACET_DISPLAYED.name(), str);
	}

	// ------------------------------------------------------------------------
	// Utility classes
	// ------------------------------------------------------------------------

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
	 * Utility class for a GUI password field that - does not show a password unless
	 * a password is entered by the user - the password field is only active and the
	 * user can only set a password if an "set password" check box in GUI is checked
	 */

	public abstract class ProtectedPassword {

		/**
		 * password
		 */
		protected String password;

		/**
		 * check box state: checked (active) or not
		 */
		private boolean active;

		public ProtectedPassword(String password) {
			this.password = password;
			this.active = false;
		}

		public abstract void setInternalPassword();

		// in GUI: password field is always empty
		public String getPassword() {
			return "";
		}

		public void setPassword(String password) {
			if (this.active) {
				this.password = password;
				this.setInternalPassword();
			}
		}

		public void setPasswordActive(boolean active) {
			this.active = active;
		}

		public boolean getPasswordActive() {
			return false;
		}

		public void activeChangedListener(ValueChangeEvent event) {
			boolean changedActive = (boolean) event.getNewValue();
			this.active = changedActive;
		}

	}

	public class EmailServerPassword extends ProtectedPassword {

		public EmailServerPassword(String password) {
			super(password);
		}

		@Override
		public void setInternalPassword() {
			setEmailServerPassword(this.password);
		}

	}

	public class DoiPassword extends ProtectedPassword {

		public DoiPassword(String password) {
			super(password);
		}

		@Override
		public void setInternalPassword() {
			setDoiPassword(this.password);
		}
	}
}
