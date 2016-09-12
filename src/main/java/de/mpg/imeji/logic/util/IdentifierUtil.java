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
package de.mpg.imeji.logic.util;

import java.net.URI;
import java.util.Random;
import java.util.UUID;

import de.mpg.imeji.logic.vo.util.ObjectHelper;

/**
 * Provides A utility Method to create identifers in imeji
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class IdentifierUtil {
  private static Random rand = new Random();
  /**
   * Array of all possible {@link String} characters which are used to generate a random Id
   */
  private static final String[] RANDOM_ID_CHARSET = {"a", "b", "c", "d", "e", "f", "g", "h", "i",
      "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B",
      "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U",
      "V", "W", "X", "Y", "Z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "_", ""};
  /**
   * The size of the random id. Since the RANDOM_ID_CHARSET has 64 elements, to calculate the number
   * of possible id, do 64^RANDOM_ID_SIZE with: - 1: 6 bits - 10: 60 bits - 12: 72 bits - 15: 90bits
   */
  private static final int RANDOM_ID_SIZE = 16;

  /**
   * Private Constructor
   */
  private IdentifierUtil() {}

  /**
   * Return a randon id
   *
   * @return
   */
  public static String newId() {
    return newRandomId();
  }

  /**
   * Return an identifier according to the method passed (universal or random)
   *
   * @return
   */
  public static String newId(String method) {
    return "universal".equals(method) ? newUniversalUniqueId() : newRandomId();
  }

  /**
   * Return an {@link URI} according to the identifier creation method
   *
   * @param c
   * @return
   */
  public static URI newURI(Class<?> c) {
    return ObjectHelper.getURI(c, newId());
  }

  /**
   * Return an {@link URI} according to the identifier creation method. Method can be universal,
   * random
   *
   * @param c
   * @return
   */
  public static URI newURI(Class<?> c, String method) {
    return ObjectHelper.getURI(c, newId(method));
  }

  /**
   * Create a random id. No assurance of uniqueness, even if probability is small. Generated id are
   * smaller...
   *
   * @return
   */
  public static String newRandomId() {
    String id = "";
    for (int i = 0; i < RANDOM_ID_SIZE; i++) {
      id += RANDOM_ID_CHARSET[rand.nextInt(RANDOM_ID_CHARSET.length)];
    }
    return id;
  }

  /**
   * Create a {@link UUID}
   *
   * @return
   */
  public static String newUniversalUniqueId() {
    return UUID.randomUUID().toString();
  }
}
