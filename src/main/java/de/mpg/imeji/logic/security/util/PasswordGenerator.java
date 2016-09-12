/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.security.util;

import java.util.Random;

/**
 * Utility class to generate a random password
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class PasswordGenerator {
  /**
   * The lenght of the generated password
   */
  public static final int PASSWORD_LENGTH = 10;
  /**
   * The Charset used to generate the password
   */
  public static final String[] CHARSET = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k",
      "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D",
      "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W",
      "X", "Y", "Z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "!", "$", "-", "_", "~", "%",
      "[", "]", "(", ")", "+", "=", "<", ">", ";", ":"};
  public static final Random random = new Random();

  /**
   * Generate a random Password of leng
   *
   * @return
   */
  public String generatePassword() {
    String password = "";
    for (int i = 0; i < PASSWORD_LENGTH; i++) {
      password += CHARSET[random.nextInt(CHARSET.length)];
    }
    return password;
  }
}
