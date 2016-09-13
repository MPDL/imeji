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
package de.mpg.imeji.logic.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A File Type (image, video, audio...)
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ImejiFileTypes {

  /**
   * A Single File Type (video, image, sound, etc.)
   *
   * @author saquet (initial creation)
   * @author $Author$ (last modification)
   * @version $Revision$ $LastChangedDate$
   */
  public class Type {
    private String names;
    private String extensions;
    private Map<String, String> namesMap;

    /**
     * Default Constructor
     */
    public Type(String names, String extension) {
      this.names = names;
      this.extensions = extension;
      this.namesMap = parseNames(names);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return names + "=" + extensions;
    }

    /**
     * Give a regex to search for this file type
     *
     * @return
     */
    public String getAsRegexQuery() {
      String regex = "";
      for (String extension : extensions.split(",")) {
        if (!regex.equals("")) {
          regex += "|";
        }
        regex += "." + extension + "$";
      }
      return regex;
    }

    public String[] getExtensionArray() {
      return extensions.split(",");
    }

    /**
     * True if the type has the following (in whatever language)
     *
     * @param name
     * @return
     */
    public boolean hasName(String name) {
      return namesMap.containsValue(name);
    }

    /**
     * Return a name for a defined language
     *
     * @param lang
     * @return
     */
    public String getName(String lang) {
      String name = namesMap.get(lang);
      if (name != null) {
        return name;
      }
      return namesMap.get("en");
    }

    /**
     * @return the name
     */
    public String getNames() {
      return names;
    }

    /**
     * @param md_name the name to set
     */
    public void setNames(String names) {
      this.names = names;
      this.namesMap = parseNames(names);
    }

    /**
     * @return the extensions
     */
    public String getExtensions() {
      return extensions;
    }

    /**
     * @param extensions the extensions to set
     */
    public void setExtensions(String extensions) {
      this.extensions = extensions;
    }

    /**
     * Parse the names (Image@en,Bilder@de,Image@fr) into a Map ()
     *
     * @param names
     * @return
     */
    private Map<String, String> parseNames(String names) {
      Map<String, String> map = new HashMap<String, String>();
      for (String nameWithLang : names.split(",")) {
        String[] nl = nameWithLang.split("@");
        String name = nl[0];
        String lang = "en";
        if (nl.length > 1) {
          lang = nl[1];
        }
        map.put(lang, name);
      }
      return map;
    }
  }

  private List<Type> types;
  private Pattern typePattern = Pattern.compile("\\[(.*?)\\]");

  /**
   * Initialize a new FilterTypeBean
   */
  public ImejiFileTypes(String s) {
    parse(s);
  }

  /**
   * Parse a String for the following format: <br/>
   * [image=jpg,png,tiff][video=avi,mp4]
   *
   * @param s
   */
  private void parse(String s) {
    this.types = new ArrayList<>();
    if (s != null) {
      Matcher m = typePattern.matcher(s);
      while (m.find()) {
        String typeString = m.group(1);
        this.types.add(new Type(typeString.split("=")[0], typeString.split("=")[1]));
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    String s = "";
    for (Type type : types) {
      s += "[" + type.toString() + "]";
    }
    return s;
  }

  /**
   * Return the type according to its name. If not found, return null.
   *
   * @param name
   * @return
   */
  public Type getType(String name) {
    for (Type type : types) {
      if (type.hasName(name)) {
        return type;
      }
    }
    return null;
  }

  /**
   * Add an emtpy type
   *
   * @param pos
   */
  public void addType(int pos) {
    types.add(pos, new Type("", ""));
  }

  /**
   * Remove a type
   *
   * @param pos
   */
  public void removeType(int pos) {
    types.remove(pos);
  }

  /**
   * @return the type
   */
  public List<Type> getTypes() {
    return types;
  }

  /**
   * @param metadata_type the type to set
   */
  public void setTypes(List<Type> types) {
    this.types = types;
  }
}
