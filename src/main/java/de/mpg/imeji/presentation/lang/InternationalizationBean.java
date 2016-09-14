/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.lang;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import com.ocpsoft.pretty.PrettyContext;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.presentation.history.HistorySession;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.CookieUtils;

/**
 * Java Bean managing language features
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class InternationalizationBean {
  private static final Logger LOGGER = Logger.getLogger(InternationalizationBean.class);
  private List<SelectItem> languages = null;
  private List<SelectItem> isolanguages = null;
  private String languagesAsString = "";
  private String currentLanguage = "en";
  private SessionBean session = null;
  private List<SelectItem> internationalizedLanguages;
  // The languages supported in imeji (defined in the properties)
  private static String[] SUPPORTED_LANGUAGES;
  public static final String LABEL_BUNDLE = "labels";
  public static final String MESSAGES_BUNDLE = "messages";


  /**
   * Constructor
   */
  public InternationalizationBean() {
    session = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    init();
    internationalizeLanguages();
  }

  /**
   * Initialize the bean
   */
  public void init() {
    try {
      Iso639_1Helper iso639_1Helper = new Iso639_1Helper();
      isolanguages = iso639_1Helper.getList();
      initLanguagesMenu();
      changeLanguage(session.getLocale().getLanguage());
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
    // add a separator
    // languages.add(new SelectItem(null, "--"));
    // Add the other languages (non supported)
    // languages.addAll(getsupportedLanguages(false));
    // init the string of all languages
    languagesAsString = "";
    for (SelectItem s : languages) {
      languagesAsString += s.getValue() + "," + s.getLabel() + "|";
    }
  }

  /**
   * If the user already set a lang cookie, return its value, else check the lang in the request
   * (browser dependant)
   *
   * @return
   */
  public static Locale getUserLocale() {
    return Locale.forLanguageTag(
        CookieUtils.readNonNull(SessionBean.langCookieName, getRequestedLocale().getLanguage()));
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
    SUPPORTED_LANGUAGES = Imeji.CONFIG.getLanguages().split(",");
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
      currentLanguage = languageString;
    } else {
      currentLanguage = getRequestedLocale().getLanguage();
    }
    session.setLocale(new Locale(currentLanguage));
    CookieUtils.updateCookieValue(SessionBean.langCookieName, session.getLocale().getLanguage());
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
      PrettyContext.getCurrentInstance().getRequestURL().toString();
    }
  }

  /**
   * Method called when the user changed the language. The new language is setted via the listener.
   * The method reload the current page
   *
   * @return
   * @throws IOException
   */
  public String changeLanguage() throws IOException {
    HistorySession history = (HistorySession) BeanHelper.getSessionBean(HistorySession.class);
    FacesContext.getCurrentInstance().getExternalContext()
        .redirect(history.getCurrentPage().getCompleteUrl());
    return "pretty:";
  }

  /**
   * setter
   *
   * @param currentLanguage
   */
  public void setCurrentLanguage(String currentLanguage) {
    this.currentLanguage = currentLanguage;
  }

  /**
   * getter
   *
   * @return
   */
  public String getCurrentLanguage() {
    return currentLanguage;
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

  /**
   * @return the languagesAsString
   */
  public String getLanguagesAsString() {
    return languagesAsString;
  }
}
