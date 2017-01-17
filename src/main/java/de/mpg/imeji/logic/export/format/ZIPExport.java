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
package de.mpg.imeji.logic.export.format;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.content.ContentService;
import de.mpg.imeji.logic.item.ItemController;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.vo.ContentVO;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;

/**
 * {@link Export} images in zip
 *
 * @author kleinfercher (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ZIPExport extends Export {
  protected String modelURI;
  private static final Logger LOGGER = Logger.getLogger(ZIPExport.class);
  private final Map<URI, Integer> itemsPerCollection;

  /**
   * @param type
   * @return
   * @throws HttpResponseException
   */
  public ZIPExport(String type) throws HttpResponseException {
    itemsPerCollection = new HashMap<URI, Integer>();
    boolean supported = false;
    if ("image".equalsIgnoreCase(type)) {
      modelURI = Imeji.imageModel;
      supported = true;
    }
    if (!supported) {
      throw new HttpResponseException(400, "Type " + type + " is not supported.");
    }
  }

  @Override
  public void init() {}

  @Override
  public void export(OutputStream out, SearchResult sr, User user) {
    try {
      exportAllImages(sr, out, user);
    } catch (final Exception e) {
      LOGGER.info("Some problems with ZIP Export", e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.export.Export#getContentType()
   */
  @Override
  public String getContentType() {
    return "application/zip";
  }

  /**
   * This method exports all images of the current browse page as a zip file
   *
   * @throws ImejiException
   *
   * @throws Exception
   * @throws URISyntaxException
   */
  public void exportAllImages(SearchResult sr, OutputStream out, User user) throws ImejiException {
    final ZipOutputStream zip = new ZipOutputStream(out);
    try {
      final List<Item> items = new ItemController().retrieveBatchLazy(sr.getResults(), user);
      // TODO to the map by filename
      final Map<String, ContentVO> contentMap = retrieveContents(items);
      updateMetrics(items);
      // Create the ZIP file
      for (String filename : contentMap.keySet()) {
        try {
          zip.putNextEntry(new ZipEntry(filename));
          new StorageController().read(contentMap.get(filename).getOriginal(), zip, false);
          zip.closeEntry();
        } catch (final ZipException ze) {
          if (ze.getMessage().contains("duplicate entry")) {
            final String name = System.currentTimeMillis() + "_" + filename;
            zip.putNextEntry(new ZipEntry(name));
            new StorageController().read(contentMap.get(filename).getOriginal(), zip, false);
            // Complete the entry
            zip.closeEntry();
          } else {
            LOGGER.error("Error zip export", ze);
          }
        }
      }
    } catch (final IOException e) {
      LOGGER.info("Some IO Exception when exporting all images!", e);
    }
    try {
      // Complete the ZIP file
      zip.close();
    } catch (final IOException ioe) {
      LOGGER.info("Could not close the ZIP File!", ioe);
    }
  }

  private void updateMetrics(List<Item> items) {
    // only images for the moment!
    // if (modelURI.equals(Imeji.imageModel)) {
    // if (itemsPerCollection.containsKey(item.getCollection())) {
    // final int newVal = itemsPerCollection.get(item.getCollection()).intValue() + 1;
    // itemsPerCollection.put(item.getCollection(), Integer.valueOf(newVal));
    // } else {
    // itemsPerCollection.put(item.getCollection(), new Integer(1));
    // }
    // }
  }

  public Map<URI, Integer> getItemsPerCollection() {
    return itemsPerCollection;
  }

  private Map<String, ContentVO> retrieveContents(List<Item> items) throws ImejiException {
    List<String> contentIds = items.stream().map(Item::getContentId).collect(Collectors.toList());
    return new ContentService().retrieveBatchLazy(contentIds).stream()
        .collect(Collectors.toMap(ContentVO::getItemId, Function.identity()));
  }


}
