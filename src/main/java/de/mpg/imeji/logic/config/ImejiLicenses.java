package de.mpg.imeji.logic.config;

/**
 * List of predefined licenses for imeji
 * 
 * @author saquet
 *
 */
public enum ImejiLicenses {
  CC_BY("Creative Commons: Attribution 4.0 International (CC BY 4.0)", "CC BY 4.0",
      "https://creativecommons.org/licenses/by/4.0/"), CC_BY_ND(
          "Creative Commons: Attribution-NoDerivatives 4.0 International (CC BY-ND 4.0)",
          "CC BY-ND 4.0", "https://creativecommons.org/licenses/by-nd/4.0/"), CC_BY_SA(
              "Creative Commons: Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)",
              "CC BY-SA 4.0", "https://creativecommons.org/licenses/by-sa/4.0/"), CC0(
                  "Creative Commons: Public Domain Dedication (CC0 1.0)", "CC0",
                  "https://creativecommons.org/publicdomain/zero/1.0/"), PDDL(
                      "Open Data Commons: ODC Public Domain Dedication and Licence", "PDDL",
                      "http://opendatacommons.org/licenses/pddl/summary/"), ODC_By(
                          "Open Data Commons: Open Data Commons Attribution License (ODC-By) v1.0",
                          "ODC-By", "http://opendatacommons.org/licenses/by/summary/"), ODC_ODbL(
                              "Open Data Commons: Open Database License (ODbL) v1.0", "ODbL",
                              "http://opendatacommons.org/licenses/odbl/summary/"), DL_DE_BY(
                                  "Data licence Germany – attribution – version 2.0",
                                  "dl-de/by-2-0", "https://www.govdata.de/dl-de/by-2-0"), DL_DE_0(
                                      "Data licence Germany - Zero - Version 2.0", "dl-de/0-2-0",
                                      "https://www.govdata.de/dl-de/by-2-0");

  private final String url;
  private final String label;
  private final String shortLabel;
  public static final String NO_LICENSE = "no_license";

  private ImejiLicenses(String label, String shortLabel, String url) {
    this.url = url;
    this.label = label;
    this.shortLabel = shortLabel;
  }

  public String getLabel() {
    return label;
  }

  public String getUrl() {
    return url;
  }

  public String getShortLabel() {
    return shortLabel;
  }
}
