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
package de.mpg.imeji.logic.storage.transform.impl;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.im4java.core.IM4JavaException;

import de.mpg.imeji.logic.storage.transform.ImageGenerator;
import de.mpg.imeji.logic.storage.util.MediaUtils;
import de.mpg.imeji.presentation.util.PropertyReader;

/**
 * {@link ImageGenerator} implemented with imagemagick
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class MagickImageGenerator implements ImageGenerator {
  private boolean enabled = false;
  private static final Logger LOGGER = Logger.getLogger(MagickImageGenerator.class);

  /**
   * Default constructor
   */
  public MagickImageGenerator() {
    try {
      enabled = Boolean.parseBoolean(PropertyReader.getProperty("imeji.imagemagick.enable"));
    } catch (Exception e) {
      throw new RuntimeException("Error reading property imeji.imagemagick.enable", e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.mpg.imeji.logic.storage.transform.ImageGenerator#generateJPG(byte[], java.lang.String)
   */
  @Override
  public File generateJPG(File file, String extension)
      throws IOException, URISyntaxException, InterruptedException, IM4JavaException {
    if (enabled) {
      try {
        return MediaUtils.convertToJPEG(file, extension);
      } catch (Exception e) {
        LOGGER.warn("Error with imagemagick: ", e);
        return null;
      }
    }
    return null;
  }
}
