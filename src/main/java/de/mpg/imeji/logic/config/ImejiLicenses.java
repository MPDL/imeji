package de.mpg.imeji.logic.config;

/**
 * List of predefined licenses for imeji
 * 
 * @author saquet
 *
 */
public enum ImejiLicenses {
  CC_BY("Attribution 4.0 International (CC BY 4.0)",
      "https://creativecommons.org/licenses/by/4.0/"), CC_BY_SA(
          "Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)",
          "https://creativecommons.org/licenses/by-sa/4.0/"), CC0(
              "Public Domain Dedication (CC0 1.0)",
              "https://creativecommons.org/publicdomain/zero/1.0/"), PDDL(
                  "ODC Public Domain Dedication and Licence",
                  "http://opendatacommons.org/licenses/pddl/summary/"), ODC_By(
                      "Open Data Commons Attribution License (ODC-By) v1.0",
                      "http://opendatacommons.org/licenses/by/summary/"), ODC_ODbL(
                          "Open Database License (ODbL) v1.0",
                          "http://opendatacommons.org/licenses/odbl/summary/");

  private final String url;
  private final String label;

  private ImejiLicenses(String label, String url) {
    this.url = url;
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public String getUrl() {
    return url;
  }
}
