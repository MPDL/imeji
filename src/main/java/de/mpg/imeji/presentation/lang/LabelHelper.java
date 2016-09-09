/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.lang;

import java.util.Iterator;

import de.mpg.imeji.util.LocalizedString;

public class LabelHelper {
  public static String getDefaultLabel(Iterator<LocalizedString> labels) {
    String l = "";
    if (labels.hasNext()) {
      l = labels.next().toString();
    }
    while (labels.hasNext()) {
      LocalizedString ls = labels.next();
      if (ls.getLang().equals("eng")) {
        l = ls.toString();
      }
    }
    return l;
  }
}
