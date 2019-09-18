package de.mpg.imeji.presentation.admin;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.codehaus.jettison.json.JSONException;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.ImejiConfiguration.HtmlSnippet;
import de.mpg.imeji.logic.config.ImejiConfiguration.ProtectedPassword;
import de.mpg.imeji.logic.config.ImejiFileTypes;
import de.mpg.imeji.logic.config.emailcontent.contentxml.EmailContentXML;
import de.mpg.imeji.logic.model.ImejiLicenses;
import de.mpg.imeji.logic.storage.util.ImageMagickUtils;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.navigation.Navigation;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * JavaBean managing the imeji configuration which is made directly by the administrator from the
 * web (i.e. not in the property file)
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "Configuration")
@ViewScoped
public class ConfigurationBean extends SuperBean {
  private static final long serialVersionUID = -5694991319458172548L;

  /**
   * Save the configuration
   */
  public void saveConfig() {
    // save standard configuration to configuration file
    Imeji.CONFIG.saveConfig();
    // save email content to email messages files
    Imeji.EMAIL_CONFIG.saveChangesAndUpdate(Imeji.CONFIG);
  }

  /**
   * Set the Snippet in the configuration
   *
   * @param str
   */
  public void setSnippet(String str) {
    Imeji.CONFIG.setSnippet(str);
  }

  /**
   * Read the snippet from the configuration
   *
   * @return
   */
  public String getSnippet() {
    return Imeji.CONFIG.getSnippet();
  }

  public boolean isImageMagickInstalled() throws IOException, URISyntaxException {
    return ImageMagickUtils.imageMagickEnabled;
  }

  /**
   * Set the url of the default CSS
   *
   * @param url
   */
  public void setDefaultCss(String url) {
    Imeji.CONFIG.setDefaultCss(url);
  }

  /**
   * Return the url of the default CSS
   *
   * @return
   */
  public String getDefaultCss() {
    return Imeji.CONFIG.getDefaultCss();
  }

  /**
   * Set the url of the default CSS
   *
   * @param url
   */
  public void setAlternativeCss(String url) {
    Imeji.CONFIG.setAlternativeCss(url);
  }

  /**
   * Return the url of the default CSS
   *
   * @return
   */
  public String getAlternativeCss() {
    return Imeji.CONFIG.getAlternativeCss();
  }

  /**
   * Set the url of the default CSS
   *
   * @param md_url
   */
  public void setUploadMaxFileSize(String size) {
    Imeji.CONFIG.setUploadMaxFileSize(size);
  }

  /**
   * Return the url of the default CSS
   *
   * @return
   */
  public String getUploadMaxFileSize() {
    return Imeji.CONFIG.getUploadMaxFileSize();
  }

  /**
   * Get the type of Files
   *
   * @return
   */
  public ImejiFileTypes getFileTypes() {
    return Imeji.CONFIG.getFileTypes();
  }

  /**
   * Set the type of Files
   *
   * @param types
   */
  public void setFileTypes(ImejiFileTypes types) {
    Imeji.CONFIG.setFileTypes(types);
  }

  /**
   * Get the html snippet for a specified lang
   *
   * @param lang
   * @return
   */
  public String getStartPageHTML(String lang) {
    return Imeji.CONFIG.getStartPageHTML(lang);
  }

  /**
   * Get the html snippet for the footer of the startpage
   *
   * @return
   */
  public String getStartPageFooterLogos() {
    return Imeji.CONFIG.getStartPageFooterLogos();
  }

  /**
   *
   * @param html
   */
  public void setStartPageFooterLogos(String html) {
    Imeji.CONFIG.setStartPageFooterLogos(html);
  }

  /**
   * Read all the html snippets in the config and retunr it as a {@link List} {@link HtmlSnippet}
   *
   * @return
   */
  public List<HtmlSnippet> getSnippets() {
    return Imeji.CONFIG.getSnippets(Arrays.asList(Imeji.CONFIG.getLanguages().split(",")));
  }

  /**
   * @return the lang
   */
  public String getLang() {
    return Imeji.CONFIG.getLang();
  }

  /**
   * @param lang the lang to set
   */
  public void setLang(String s) {
    Imeji.CONFIG.setLang(s);
  }

  /**
   * @return the list of all formats supported by the data viewer service
   */
  public String getDataViewerFormatListString() {
    return Imeji.CONFIG.getDataViewerFormatListString();
  }

  /**
   * @param str
   *
   */
  public void setDataViewerFormatListString(String str) {
    Imeji.CONFIG.setDataViewerFormatListString(str);
  }

  /**
   * true if the format is supported by the data viewer service
   *
   * @param format
   * @return
   */
  public boolean isDataViewerSupportedFormats(String format) {
    return Imeji.CONFIG.isDataViewerSupportedFormats(format);
  }

  /**
   * @return the url of the data viewer service
   */
  public String getDataViewerUrl() {
    return Imeji.CONFIG.getDataViewerUrl();
  }

  /**
   * @param str
   *
   */
  public void setDataViewerUrl(String str) {
    Imeji.CONFIG.setDataViewerUrl(str);
  }

  public String fetchDataViewerFormats() throws JSONException {
    Imeji.CONFIG.fetchDataViewerFormats();
    return "";
  }

  public String getAutosuggestForOrganizations() {
    return Imeji.CONFIG.getAutosuggestForOrganizations();
  }

  public void setAutosuggestForOrganizations(String s) {
    Imeji.CONFIG.setAutosuggestForOrganizations(s);
  }

  public String getAutoSuggestForUsers() {
    return Imeji.CONFIG.getAutoSuggestForUsers();
  }

  public void setAutoSuggestForUsers(String s) {
    Imeji.CONFIG.setAutoSuggestForUsers(s);
  }

  /**
   * Set the meta description
   *
   * @param md_url
   */
  public void setMetaDescription(String s) {
    Imeji.CONFIG.setMetaDescription(s);
  }

  /**
   * Return the meta description
   *
   * @return
   */
  public String getMetaDescription() {
    return Imeji.CONFIG.getMetaDescription();
  }

  public String getMetaAuthor() {
    return Imeji.CONFIG.getMetaAuthor();
  }

  public void setMetaAuthor(String s) {
    Imeji.CONFIG.setMetaAuthor(s);
  }

  /**
   * Set the name of the instance
   *
   * @param md_url
   */
  public void setInstanceName(String s) {
    Imeji.CONFIG.setInstanceName(s);
  }

  /**
   * Return the name of the instance
   *
   * @return
   */
  public String getInstanceName() {
    return Imeji.CONFIG.getInstanceName();
  }

  /**
   * Set the contact email
   *
   * @param md_url
   */
  public void setContactEmail(String s) {
    Imeji.CONFIG.setContactEmail(s);
  }

  /**
   * Return contact email
   *
   * @return
   */
  public String getContactEmail() {
    return Imeji.CONFIG.getContactEmail();
  }

  public void setEmailServer(String s) {
    Imeji.CONFIG.setEmailServer(s);
  }

  public String getEmailServer() {
    return Imeji.CONFIG.getEmailServer();
  }

  public void setEmailServerUser(String s) {
    Imeji.CONFIG.setEmailServerUser(s);
  }

  public String getEmailServerUser() {
    return Imeji.CONFIG.getEmailServerUser();
  }

  public void setEmailServerPassword(ProtectedPassword emailServerPassword) {
    Imeji.CONFIG.setProtectedEmailServerPassword(emailServerPassword);
  }

  public ProtectedPassword getEmailServerPassword() {
    return Imeji.CONFIG.getProtectedEmailServerPassword();
  }

  public void setEmailServerEnableAuthentication(boolean b) {
    Imeji.CONFIG.setEmailServerEnableAuthentication(b);
  }

  public boolean getEmailServerEnableAuthentication() {
    return Imeji.CONFIG.getEmailServerEnableAuthentication();
  }

  public void setPrivateModus(boolean b) {
    Imeji.CONFIG.setPrivateModus(b);
  }

  public boolean getPrivateModus() {
    return Imeji.CONFIG.getPrivateModus();
  }

  public void setEmailServerSender(String s) {
    Imeji.CONFIG.setEmailServerSender(s);
  }

  public String getEmailServerSender() {
    return Imeji.CONFIG.getEmailServerSender();
  }

  public void setEmailServerPort(String s) {
    Imeji.CONFIG.setEmailServerPort(s);
  }

  public String getEmailServerPort() {
    return Imeji.CONFIG.getEmailServerPort();
  }

  public void setUploadBlackList(String s) {
    Imeji.CONFIG.setUploadBlackList(s);
  }

  public String getUploadBlackList() {
    return Imeji.CONFIG.getUploadBlackList();
  }

  public void setUploadWhiteList(String s) {
    Imeji.CONFIG.setUploadWhiteList(s);
  }

  public String getUploadWhiteList() {
    return Imeji.CONFIG.getUploadWhiteList();
  }

  public String getLanguages() {
    return Imeji.CONFIG.getLanguages();
  }

  public void setLanguages(String value) {
    Imeji.CONFIG.setLanguages(value);
  }

  public String getDoiPublisher() {
    return Imeji.CONFIG.getDoiPublisher();
  }

  public void setDoiPublisher(String s) {
    Imeji.CONFIG.setDoiPublisher(s);
  }

  public String getDoiUser() {
    return Imeji.CONFIG.getDoiUser();
  }

  public void setDoiUser(String s) {
    Imeji.CONFIG.setDoiUser(s);
  }

  public ProtectedPassword getDoiPassword() {
    return Imeji.CONFIG.getProtectedDoiPassword();
  }

  public void setDoiPassword(ProtectedPassword protectedDoiPassword) {
    Imeji.CONFIG.setProtectedDoiPassword(protectedDoiPassword);
  }

  public String getDoiServiceUrl() {
    return Imeji.CONFIG.getDoiServiceUrl();
  }

  public void setDoiServiceUrl(String s) {
    Imeji.CONFIG.setDoiServiceUrl(s);
  }

  public void setImpressumUrl(String s) {
    Imeji.CONFIG.setImpressumUrl(s);
  }

  public String getImpressumUrl() {
    return Imeji.CONFIG.getImpressumUrl() != null ? Imeji.CONFIG.getImpressumUrl() : "";
  }

  public void setImpressumText(String s) {
    Imeji.CONFIG.setImpressumText(s);
  }

  public String getImpressumText() {
    return Imeji.CONFIG.getImpressumText() != null ? Imeji.CONFIG.getImpressumText() : "";
  }

  public void setFaviconUrl(String s) {
    Imeji.CONFIG.setFaviconUrl(s);
  }

  public String getFaviconUrl() {
    final Navigation navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
    return Imeji.CONFIG.getFaviconUrl(navigation.getApplicationUri());
  }

  public void setRegistrationTokenExpiry(String s) {
    Imeji.CONFIG.setRegistrationTokenExpiry(s);
  }

  public String getRegistrationTokenExpiry() {
    return Imeji.CONFIG.getRegistrationTokenExpiry();
  }

  public boolean isRegistrationEnabled() {
    return Imeji.CONFIG.isRegistrationEnabled();
  }

  public void setRegistrationEnabled(boolean enabled) {
    Imeji.CONFIG.setRegistrationEnabled(enabled);
  }

  public void setLogoUrl(String s) {
    Imeji.CONFIG.setLogoUrl(s);
  }

  public String getLogoUrl() {
    return Imeji.CONFIG.getLogoUrl();
  }

  public String getDefaultBrowseView() {
    return Imeji.CONFIG.getDefaultBrowseView();
  }

  public void setDefaultBrowseView(String string) {
    Imeji.CONFIG.setDefaultBrowseView(string);
  }

  public void setQuotaLimits(String limits) {
    try {
      Imeji.CONFIG.setQuotaLimits(limits);
    } catch (final Exception e) {
      BeanHelper.error("Wrong format for quota definition! Has to be comma separated list. " + "Wrong input " + e.getMessage());
    }
  }

  public String getQuotaLimits() {
    return Imeji.CONFIG.getQuotaLimits();
  }

  public String getDefaultQuota() {
    return Imeji.CONFIG.getDefaultQuota();
  }

  public void setdefaultQuota(String defaultQuota) {
    Imeji.CONFIG.setdefaultQuota(defaultQuota);
  }

  public String getRegistrationWhiteList() {
    return Imeji.CONFIG.getRegistrationWhiteList();
  }

  public void setRegistrationWhiteList(String s) {
    Imeji.CONFIG.setRegistrationWhiteList(s);
  }

  public void setHelpUrl(String url) {
    Imeji.CONFIG.setHelpUrl(url);
  }

  public String getHelpUrl() {
    return Imeji.CONFIG.getHelpUrl();
  }

  public void setRegistrationSnippet(String url) {
    Imeji.CONFIG.setRegistrationSnippet(url);
  }

  public String getRegistrationSnippet() {
    return Imeji.CONFIG.getRegistrationSnippet();
  }

  public void setMaintenanceMessage(String message) {
    Imeji.CONFIG.setMaintenanceMessage(message);
  }

  public String getMaintenanceMessage() {
    return Imeji.CONFIG.getMaintenanceMessage() != null ? Imeji.CONFIG.getMaintenanceMessage() : "";
  }

  public void setTermsOfUse(String s) {
    Imeji.CONFIG.setTermsOfUse(s);
  }

  public String getTermsOfUse() {
    return Imeji.CONFIG.getTermsOfUse() != null ? Imeji.CONFIG.getTermsOfUse() : "";
  }

  public void setTermsOfUseUrl(String s) {
    Imeji.CONFIG.setTermsOfUseUrl(s);
  }

  public String getTermsOfUseUrl() {
    return Imeji.CONFIG.getTermsOfUseUrl() != null ? Imeji.CONFIG.getTermsOfUseUrl() : "";
  }

  public void setPrivacyPolicy(String s) {
    Imeji.CONFIG.setPrivacyPolicy(s);
  }

  public String getPrivacyPolicy() {
    return Imeji.CONFIG.getPrivacyPolicy() != null ? Imeji.CONFIG.getPrivacyPolicy() : "";
  }

  public void setPrivacyPolicyUrl(String s) {
    Imeji.CONFIG.setPrivacyPolicyUrl(s);
  }

  public String getPrivacyPolicyUrl() {
    return Imeji.CONFIG.getPrivacyPolicyUrl() != null ? Imeji.CONFIG.getPrivacyPolicyUrl() : "";
  }

  public String getDefaultLicense() {
    return Imeji.CONFIG.getDefaultLicense();
  }

  public void setDefaultLicense(String licenseName) {
    Imeji.CONFIG.setDefaultLicense(licenseName);
  }

  public List<SelectItem> getLicenseMenu() {
    final List<SelectItem> licenseMenu = new ArrayList<>();
    for (final ImejiLicenses lic : ImejiLicenses.values()) {
      licenseMenu.add(new SelectItem(lic.name(), lic.getLabel()));
    }
    return licenseMenu;
  }

  public String getTechnicalMetadata() {
    return Imeji.CONFIG.getTechnicalMetadata();
  }

  public void setTechnicalMetadata(String technicalMetadata) {
    Imeji.CONFIG.setTechnicalMetadata(technicalMetadata);
  }

  public String getThumbnailWidth() {
    return Imeji.CONFIG.getThumbnailWidth();
  }

  public void setThumbnailWidth(String thumbnailWidth) {
    Imeji.CONFIG.setThumbnailWidth(thumbnailWidth);
  }

  public String getWebResolutionWidth() {
    return Imeji.CONFIG.getWebResolutionWidth();
  }

  public void setWebResolutionWidth(String webResolutionWidth) {
    Imeji.CONFIG.setWebResolutionWidth(webResolutionWidth);
  }

  public String getNumberOfLinesInThumbnailList() {
    return Imeji.CONFIG.getNumberOfLinesInThumbnailList();
  }

  public void setNumberOfLinesInThumbnailList(String lines) {
    Imeji.CONFIG.setNumberOFLinesInThumbnailList(lines);
  }

  public String getGoogleMapsApi() {
    return Imeji.CONFIG.getGoogleMapsApi();
  }

  public void setGoogleMapsApi(String url) {
    Imeji.CONFIG.setGoogleMapsApi(url);
  }

  public String getConeAuthors() {
    return Imeji.CONFIG.getConeAuthors();
  }

  public void setConeAuthors(String url) {
    Imeji.CONFIG.setConeAuthors(url);
  }

  public String getFacetDisplayed() {
    return Imeji.CONFIG.getFacetDisplayed();
  }

  public void setFacetDisplayed(String str) {
    Imeji.CONFIG.setFacetDisplayed(str);
  }

  // ------------------------------------------------------------------------------
  // SECTION edit/delete/change of message texts
  // -----------------------------------------------------------------------------

  public List<List<EmailContentXML>> getAllMessages() {
    return Imeji.EMAIL_CONFIG.getAllEmailMessagesInAllLanguages();
  }

  public List<EmailContentXML> getEmailRegistrationRequestBody() {
    return Imeji.EMAIL_CONFIG.getAllEmailMessagesInAllLanguagesAsList();
  }

  public String getGUILabelForEMailMessageIdentifier(String identifier) {
    return Imeji.EMAIL_CONFIG.getGUILabelForEMailMessageIdentifier(identifier, getLocale());
  }

  /**
   * Get a label from resource bundle given an identifier
   * 
   * @param identifier
   * @param locale
   * @return
   */
  public String getGUILabel(String identifier) {
    return Imeji.RESOURCE_BUNDLE.getLabel(identifier, getLocale());
  }


  public String getCollectionTypes() {
    return Imeji.CONFIG.getCollectionTypes();
  }

  public void setCollectionTypes(String str) {
    Imeji.CONFIG.setCollectionTypes(str);
  }

  public String getCollectionMetadataSuggestions() {
    return Imeji.CONFIG.getCollectionMetadataSuggestions();
  }

  public List<String> getCollectionMetadataSuggestionsAsList() {
    return Imeji.CONFIG.getCollectionMetadataSuggestionsAsList();
  }

  public void setCollectionMetadataSuggestions(String str) {
    Imeji.CONFIG.setCollectionMetadataSuggestions(str);
  }

  public String getCollectionMetadataSuggestionsPreselect() {
    return Imeji.CONFIG.getCollectionMetadataSuggestionsPreselect();
  }

  public List<String> getCollectionMetadataSuggestionsPreselectAsList() {
    return Imeji.CONFIG.getCollectionMetadataSuggestionsPreselectAsList();
  }

  public void setCollectionMetadataSuggestionsPreselect(String str) {
    Imeji.CONFIG.setCollectionMetadataSuggestionsPreselect(str);
  }

  public List<String> getCollectionMetadataSuggestionsComplete() {
    return Stream.concat(getCollectionMetadataSuggestionsPreselectAsList().stream(), getCollectionMetadataSuggestionsAsList().stream())
        .collect(Collectors.toList());
  }

  public String getOrganizationSuggestions() {
    return Imeji.CONFIG.getOrganizationNames();
  }

  public List<String> getOrganizationSuggestionsAsList() {
    return Imeji.CONFIG.getOrganizationNamesAsList();
  }

  public void setOrganizationSuggestions(String str) {
    Imeji.CONFIG.setOrganizationNames(str);
  }

  public String getMaxNumberCitationAuthors() {
    return Imeji.CONFIG.getMaxNumberCitationAuthors();
  }

  public void setMaxNumberCitationAuthors(String str) {
    Imeji.CONFIG.setMaxNumberCitationAuthors(str);
  }

  public void setDisplayCookieNotice(boolean b) {
    Imeji.CONFIG.setDisplayCookieNotice(b);
  }

  public boolean getDisplayCookieNotice() {
    return Imeji.CONFIG.getDisplayCookieNotice();
  }

}
