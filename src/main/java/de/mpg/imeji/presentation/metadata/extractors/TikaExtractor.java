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
package de.mpg.imeji.presentation.metadata.extractors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;

import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.vo.Item;

/**
 * User {@link Tika} to extract metadata out of the image
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class TikaExtractor {
  private static final Logger LOGGER = Logger.getLogger(TikaExtractor.class);
  private static final int WRITE_LIMIT = 10 * 1024 * 1024;

  public static List<String> extract(Item item) {
    List<String> techMd = new ArrayList<String>();
    try {
      StorageController sc = new StorageController();
      ByteArrayOutputStream bous = new ByteArrayOutputStream();
      sc.read(item.getFullImageUrl().toString(), bous, true);
      ByteArrayInputStream in = new ByteArrayInputStream(bous.toByteArray());
      Metadata metadata = new Metadata();
      AutoDetectParser parser = new AutoDetectParser();
      BodyContentHandler handler = new BodyContentHandler(WRITE_LIMIT);
      parser.parse(in, handler, metadata);
      for (String name : metadata.names()) {
        techMd.add(name + " :  " + metadata.get(name));
      }
    } catch (Exception e) {
      LOGGER.error("There had been some Tika extraction issues.", e);
    }
    return techMd;
  }

  public static List<String> extractFromFile(File file) {
    List<String> techMd = new ArrayList<String>();
    try {
      Metadata metadata = new Metadata();
      AutoDetectParser parser = new AutoDetectParser();
      BodyContentHandler handler = new BodyContentHandler();
      FileInputStream is = new FileInputStream(file);
      parser.parse(is, handler, metadata);
      for (String name : metadata.names()) {
        techMd.add(name + " :  " + metadata.get(name));
      }
    } catch (Exception e) {
      LOGGER.error("There had been some Tika file metadata extraction issues.", e);
    }
    return techMd;
  }
}
