package de.mpg.imeji.logic.util;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

import org.apache.commons.codec.digest.DigestUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

/**
 * Static functions to manipulate {@link String}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class StringHelper {
  /**
   * Character that separates components of an url
   */
  public static final String urlSeparator = "/";
  /**
   * Character that separates components of a file path. This is "/" on UNIX and "\" on Windows.
   */
  public static final String fileSeparator = System.getProperty("file.separator");
  /**
   * Tha maximum size of a file: Theorically, the max length could be 255. For security, imeji uses
   * a lower count.
   */
  public static final int FILENAME_MAX_LENGTH = 200;

  /**
   * Private Constructor
   */
  private StringHelper() {}

  /**
   * Encode a {@link String} to MD5
   * 
   * @param pass
   * @return
   * @throws Exception
   */
  public static String convertToMD5(String pass) throws Exception {
    MessageDigest dig = MessageDigest.getInstance("MD5");
    dig.update(pass.getBytes("UTF-8"));
    byte[] messageDigest = dig.digest();
    StringBuilder sBuilder = new StringBuilder();
    for (int i = 0; i < messageDigest.length; i++) {
      sBuilder.append(Integer.toHexString(0xFF & messageDigest[i]));
    }
    return sBuilder.toString();
  }

  /**
   * Format a uri (URL): add a / if the uri doesn't end with it
   * 
   * @return
   */
  public static String normalizeURI(String uri) {
    if (!uri.endsWith(urlSeparator)) {
      uri += urlSeparator;
    }
    return uri;
  }

  /**
   * Format a system path
   * 
   * @param path
   * @return
   */
  public static String normalizePath(String path) {
    if (!path.endsWith(fileSeparator)) {
      path += fileSeparator;
    }
    return path;
  }

  /**
   * Transform a filename to a correct filename, which can be used by the internal storage to store
   * a file
   * 
   * @param filename
   * @return
   * @throws UnsupportedEncodingException
   */
  public static String normalizeFilename(String filename) {
    try {
      String filextension = getFileExtension(filename);
      filename = DigestUtils.md5Hex(filename) + "." + filextension;
      return filename;
    } catch (Exception e) {
      throw new RuntimeException("Error with filename: " + filename, e);
    }
  }

  /**
   * Parse the file extension from its name
   * 
   * @param filename
   * @return
   */
  public static String getFileExtension(String filename) {
    int i = filename.lastIndexOf('.');
    if (i > 0) {
      return filename.substring(i + 1);
    }
    return null;
  }

  /**
   * Trimmed {@link com.google.common.base.Strings#isNullOrEmpty(String) isNullOrEmpty}
   *
   * @param str
   * @return
   */
  public static boolean isNullOrEmptyTrim(String str) {
    return isNullOrEmpty(str) || "".equals(str.trim());
  }

  /**
   * 
   * @param s
   * @return
   */
  public static boolean hasInvalidTags(String s) {
    if (isNullOrEmpty(s)) {
      return false;
    }
    if (!Jsoup.isValid(s, Whitelist.relaxed())) {
      return true;
    }
    return false;
  }

}
