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
      for (final String extension : extensions.split(",")) {
        if (!regex.equals("")) {
          regex += "|";
        }
        regex += "." + extension + "$";
      }
      return regex;
    }

    public List<String> getAsRegexList() {
      List<String> regexList = new ArrayList<>();
      for (final String extension : extensions.split(",")) {
        regexList.add("." + extension);
      }
      return regexList;
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
      final String name = namesMap.get(lang);
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
      final Map<String, String> map = new HashMap<String, String>();
      for (final String nameWithLang : names.split(",")) {
        final String[] nl = nameWithLang.split("@");
        final String name = nl[0];
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
  private final Pattern typePattern = Pattern.compile("\\[(.*?)\\]");

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
      final Matcher m = typePattern.matcher(s);
      while (m.find()) {
        final String typeString = m.group(1);
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
    for (final Type type : types) {
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
    for (final Type type : types) {
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
