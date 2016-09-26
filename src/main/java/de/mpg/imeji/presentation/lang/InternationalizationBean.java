/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.lang;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.presentation.history.HistorySession;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.util.CookieUtils;

/**
 * Java Bean managing language features
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "InternationalizationBean")
@SessionScoped
public class InternationalizationBean implements Serializable {
  private static final long serialVersionUID = -6750472078884009668L;
  private static final Logger LOGGER = Logger.getLogger(InternationalizationBean.class);
  private static final String lANGUAGE_COOKIE = "IMEJI_LANG";
  private static String[] SUPPORTED_LANGUAGES;
  private List<SelectItem> languages;
  private List<SelectItem> isolanguages;
  private List<SelectItem> internationalizedLanguages;
  // The languages supported in imeji (defined in the properties)

  private Locale locale = Locale.ENGLISH;
  private String languagesAsString;

  /**
   * Constructor
   */
  public InternationalizationBean() {
    SUPPORTED_LANGUAGES = Imeji.CONFIG.getLanguages().split(",");
    isolanguages = new Iso639_1Helper().getList();
    readLocaleFromCookie();
    initLanguagesMenu();
    init();
    internationalizeLanguages();
  }

  /**
   * Initialize the bean
   */
  public void init() {
    try {
      changeLanguage(locale.getLanguage());
      languagesAsString = "";
      for (SelectItem s : languages) {
        languagesAsString += s.getValue() + "," + s.getLabel() + "|";
      }
    } catch (Exception e) {
      LOGGER.error("Error Intializing InternationalitationBean:", e);
    }
  }

  /**
   * Menu with first, the supported languages out of the properties, second all the iso languages
   *
   * @param SUPPORTED_LANGUAGES
   */
  public void initLanguagesMenu() {
    // Add first languages out of properties
    languages = new ArrayList<SelectItem>();
    languages.addAll(getsupportedLanguages(true));
  }

  /**
   * If the user already set a lang cookie, return its value, else check the lang in the request
   * (browser dependant)
   *
   * @return
   */
  private void readLocaleFromCookie() {
    this.locale = Locale.forLanguageTag(
        CookieUtils.readNonNull(lANGUAGE_COOKIE, getRequestedLocale().getLanguage()));
  }

  /**
   * Get the Locale according the user request and to the supported languages in the Configuration.
   * If no valid local could be found, return English
   *
   * @return
   */
  public static Locale getRequestedLocale() {
    Locale requestedLocale =
        FacesContext.getCurrentInstance().getExternalContext().getRequestLocale();
    if (isSupported(requestedLocale.getLanguage())) {
      return requestedLocale;
    } else if (isSupported(Locale.ENGLISH.getLanguage())) {
      return Locale.ENGLISH;
    } else if (SUPPORTED_LANGUAGES.length > 0) {
      return new Locale(SUPPORTED_LANGUAGES[0]);
    }
    return Locale.ENGLISH;
  }

  /**
   * Languages for imeji internationalization
   */
  private void internationalizeLanguages() {
    internationalizedLanguages = getsupportedLanguages(true);
  }

  /**
   * True if a language (defined in iso639_1) is supported in imeji (according to the properties)
   *
   * @param langString
   * @return
   */
  public static boolean isSupported(String langString) {

    for (int i = 0; i < SUPPORTED_LANGUAGES.length; i++) {
      if (SUPPORTED_LANGUAGES[i].equals(langString)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return: <br/>
   * - the supported languages (i.e languages with a translation for labels and messages) if the
   * parameter is set to true <br/>
   * - the non supported languages if the parameter is set to false
   *
   * @param supported
   * @return
   */
  private List<SelectItem> getsupportedLanguages(boolean supported) {
    List<SelectItem> l = new ArrayList<SelectItem>();
    for (SelectItem iso : isolanguages) {
      if (supported && isSupported(iso.getValue().toString())
          || (!supported && !isSupported(iso.getValue().toString()))) {
        l.add(iso);
      }
    }
    return l;
  }

  /**
   * Return the label of the language
   *
   * @param lang
   * @return
   */
  public String getLanguageLabel(String lang) {
    for (SelectItem iso : isolanguages) {
      if (((String) iso.getValue()).equals(lang)) {
        return iso.getLabel();
      }
    }
    return lang;
  }

  /**
   * Change the language of imeji
   *
   * @param languageString
   */
  private void changeLanguage(String languageString) {
    if (isSupported(languageString)) {
      locale = new Locale(languageString);
    } else {
      locale = getRequestedLocale();
    }
    CookieUtils.updateCookieValue(lANGUAGE_COOKIE, getLocale().getLanguage());
    internationalizeLanguages();
  }

  /**
   * Listener when the language for imeji is changed
   *
   * @param event
   */
  public void currentlanguageListener(ValueChangeEvent event) {
    if (event != null && !event.getNewValue().toString().equals(event.getOldValue().toString())) {
      changeLanguage(event.getNewValue().toString());
    }
  }

  /**
   * Method called when the user changed the language. The new language is setted via the listener.
   * The method reload the current page
   *
   * @return
   * @throws IOException
   */
  public void changeLanguage() throws IOException {
    HistorySession history = (HistorySession) BeanHelper.getSessionBean(HistorySession.class);
    FacesContext.getCurrentInstance().getExternalContext()
        .redirect(history.getCurrentPage().getCompleteUrl());
    // return "pretty:";
  }

  /**
   * setter
   *
   * @param currentLanguage
   */
  public void setCurrentLanguage(String currentLanguage) {
    this.locale = new Locale(currentLanguage);
  }

  /**
   * getter
   *
   * @return
   */
  public String getCurrentLanguage() {
    return this.locale.getLanguage();
  }

  /**
   * setter
   *
   * @return
   */
  public List<SelectItem> getLanguages() {
    return languages;
  }

  /**
   * setter
   *
   * @param languages
   */
  public void setLanguages(List<SelectItem> languages) {
    this.languages = languages;
  }

  /**
   * getter
   *
   * @return
   */
  public List<SelectItem> getInternationalizedLanguages() {
    return internationalizedLanguages;
  }

  /**
   * setter
   *
   * @param internationalizedLanguages
   */
  public void setInternationalizedLanguages(List<SelectItem> internationalizedLanguages) {
    this.internationalizedLanguages = internationalizedLanguages;
  }

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public String getLanguagesAsString() {
    return languagesAsString;
  }
}
