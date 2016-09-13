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
package de.mpg.imeji.presentation.beans;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import org.codehaus.jettison.json.JSONException;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.config.ImejiFileTypes;
import de.mpg.imeji.logic.config.ImejiConfiguration.HtmlSnippet;
import de.mpg.imeji.logic.storage.util.ImageMagickUtils;
import de.mpg.imeji.presentation.lang.InternationalizationBean;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * JavaBean managing the imeji configuration which is made directly by the administrator from the
 * web (i.e. not in the property file)
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "Configuration")
@ApplicationScoped
public class ConfigurationBean {

  /**
   * Save the configuration in the config file
   */
  public void saveConfig() {
    Imeji.CONFIG.saveConfig();
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

  public void setStartPageCarouselEnabled(boolean input) {
    Imeji.CONFIG.setStartPageCarouselEnabled(input);
  }

  public boolean getStartPageCarouselEnabled() {
    return Imeji.CONFIG.getStartPageCarouselEnabled();
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
    de.mpg.imeji.presentation.lang.InternationalizationBean internationalizationBean =
        (InternationalizationBean) BeanHelper.getApplicationBean(InternationalizationBean.class);
    return Imeji.CONFIG.getSnippets(internationalizationBean.getLanguages());
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

  public void setEmailServerPassword(String s) {
    Imeji.CONFIG.setEmailServerPassword(s);
  }

  public String getEmailServerPassword() {
    return Imeji.CONFIG.getEmailServerPassword();
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

  public void setStartPageCarouselQuery(String s) {
    Imeji.CONFIG.setStartPageCarouselQuery(s);
  }

  public String getStartPageCarouselQuery() {
    return Imeji.CONFIG.getStartPageCarouselQuery();
  }

  public void setStartPageCarouselQueryOrder(String s) {
    Imeji.CONFIG.setStartPageCarouselQueryOrder(s);
  }

  public String getStartPageCarouselQueryOrder() {
    return Imeji.CONFIG.getStartPageCarouselQueryOrder();
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

  public String getDoiUser() {
    return Imeji.CONFIG.getDoiUser();
  }

  public void setDoiUser(String s) {
    Imeji.CONFIG.setDoiUser(s);
  }

  public String getDoiPassword() {
    return Imeji.CONFIG.getDoiPassword();
  }

  public void setDoiPassword(String s) {
    Imeji.CONFIG.setDoiPassword(s);
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
    Navigation navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
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
    } catch (Exception e) {
      BeanHelper.error("Wrong format for quota definition! Has to be comma separated list. "
          + "Wrong input " + e.getMessage());
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

  public boolean getAlbumsEnabled() {
    return Imeji.CONFIG.getAlbumsEnabled();
  }

  public void setAlbumsEnabled(boolean enabled) {
    Imeji.CONFIG.setAlbumsEnabled(enabled);
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
}
