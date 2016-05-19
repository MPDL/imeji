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
package de.mpg.imeji.logic.storage.transform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.storage.Storage.FileResolution;
import de.mpg.imeji.logic.storage.transform.generator.ImageGenerator;
import de.mpg.imeji.logic.storage.transform.generator.MagickImageGenerator;
import de.mpg.imeji.logic.storage.transform.generator.PdfImageGenerator;
import de.mpg.imeji.logic.storage.transform.generator.RawFileImageGenerator;
import de.mpg.imeji.logic.storage.transform.generator.SimpleAudioImageGenerator;
import de.mpg.imeji.logic.storage.transform.generator.SimpleImageGenerator;
import de.mpg.imeji.logic.storage.util.ImageMagickUtils;
import de.mpg.imeji.logic.storage.util.ImageUtils;
import de.mpg.imeji.logic.storage.util.StorageUtils;

/**
 * Implements all process to generate the images
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public final class ImageGeneratorManager {
  private final List<ImageGenerator> generators;
  private static final Logger LOGGER = Logger.getLogger(ImageGeneratorManager.class);

  /**
   * Default constructor of {@link ImageGeneratorManager}
   */
  public ImageGeneratorManager() {
    generators = new ArrayList<ImageGenerator>();
    generators.add(new PdfImageGenerator());
    generators.add(new SimpleAudioImageGenerator());
    generators.add(new MagickImageGenerator());
    generators.add(new SimpleImageGenerator());
    generators.add(new RawFileImageGenerator());
  }

  /**
   * Generate a Thumbnail image for imeji
   *
   * @param bytes
   * @param extension
   * @return
   */
  public File generateThumbnail(File file, String extension) {
    return generate(file, extension, FileResolution.THUMBNAIL);
  }

  /**
   * Generate a Web resolution image for imeji
   *
   * @param bytes
   * @param extension
   * @return
   */
  public File generateWebResolution(File file, String extension) {
    return generate(file, extension, FileResolution.WEB);
  }

  /**
   * Generate an image (only jpg and gif supported here) into a smaller image according to the
   * {@link FileResolution}
   *
   * @param bytes
   * @param extension
   * @param resolution
   * @return
   */
  public File generate(File file, String extension, FileResolution resolution) {
    if (StorageUtils.compareExtension("gif", extension)) {
      File gifFile = ImageMagickUtils.resizeAnimatedGif(file, resolution);
      if (gifFile != null) {
        return gifFile;
      }
    }
    return generateJpeg(file, extension, resolution);
  }

  /**
   * Generate an jpeg image in the wished size.
   *
   * @param bytes
   * @param extension
   * @param resolution
   * @return
   */
  private File generateJpeg(File file, String extension, FileResolution resolution) {
    // Make a jpg out of the file
    try {
      return ImageUtils.resizeJPEG(toJpeg(file, extension), resolution);
    } catch (Exception e) {
      LOGGER.error("Error generating JPEG from File: ", e);
    }
    return null;

  }

  /**
   * Uses the {@link ImageGenerator} to transform the bytes into a jpg
   *
   * @param bytes
   * @param extension
   * @return
   * @throws ImejiException
   * @throws IOException
   */
  private File toJpeg(File file, String extension) throws ImejiException {
    if (StorageUtils.compareExtension(extension, "jpg")) {
      return file;
    }
    for (ImageGenerator imageGenerator : generators) {
      try {
        File jpeg = imageGenerator.generateJPG(file, extension);
        if (jpeg != null && jpeg.length() > 0) {
          return jpeg;
        }
      } catch (Exception e) {
        LOGGER.warn("Error generating image (generator: " + imageGenerator.getClass().getName(), e);
      }
    }
    throw new ImejiException("Unsupported file format (requested was " + extension + ")");
  }
}
