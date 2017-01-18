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
package de.mpg.imeji.logic.export;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.content.ContentService;
import de.mpg.imeji.logic.item.ItemController;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.vo.ContentVO;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;

/**
 * {@link ExportAbstract} images in zip
 *
 * @author kleinfercher (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ZIPExport extends ExportAbstract {
  private static final Logger LOGGER = Logger.getLogger(ZIPExport.class);
  private final Map<String, Integer> itemsPerCollection = new HashMap<String, Integer>();
  private final List<String> itemIds;

  public ZIPExport(List<String> itemIds, User user) {
    super(user);
    this.itemIds = itemIds;
    this.name = new Date().toString().replace(" ", "_").replace(":", "-").concat(".zip");
  }

  @Override
  public void export(OutputStream out) throws ImejiException {
    exportAllImages(itemIds, out, user);
  }

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
  private void exportAllImages(List<String> ids, OutputStream out, User user)
      throws ImejiException {
    final ZipOutputStream zip = new ZipOutputStream(out);
    final Map<String, Item> itemMap = retrieveItems(ids);
    final Map<String, ContentVO> contentMap = retrieveContents(itemMap.values());
    createItemsPerCollection(itemMap.values());
    // Create the ZIP file
    for (String contentId : contentMap.keySet()) {
      try {
        addZipEntry(zip, itemMap.get(contentId).getFilename(),
            contentMap.get(contentId).getOriginal(), 0);
      } catch (Exception e) {
        LOGGER.error("Error zip export", e);
      }
    }
    try {
      // Complete the ZIP file
      zip.close();
    } catch (final IOException ioe) {
      LOGGER.info("Could not close the ZIP File!", ioe);
    }
  }

  /**
   * Add a {@link ZipEntry} to the {@link ZipOutputStream}
   * 
   * @param zip
   * @param filename
   * @param fileUrl
   * @throws IOException
   * @throws ImejiException
   */
  private void addZipEntry(ZipOutputStream zip, String filename, String fileUrl, int position)
      throws IOException, ImejiException {
    try {
      if (position > 0) {
        filename = FilenameUtils.getBaseName(filename).replace("_" + (position - 1), "") + "_"
            + position + "." + FilenameUtils.getExtension(filename);
      }
      zip.putNextEntry(new ZipEntry(filename));
      new StorageController().read(fileUrl, zip, false);
      zip.closeEntry();
    } catch (final ZipException ze) {
      if (ze.getMessage().contains("duplicate entry")) {
        addZipEntry(zip, filename, fileUrl, position + 1);
      } else {
        throw ze;
      }
    }
  }

  private void createItemsPerCollection(Collection<Item> items) {
    for (Item item : items) {
      if (itemsPerCollection.containsKey(item.getCollection().toString())) {
        final int newVal = itemsPerCollection.get(item.getCollection().toString()).intValue() + 1;
        itemsPerCollection.put(item.getCollection().toString(), Integer.valueOf(newVal));
      } else {
        itemsPerCollection.put(item.getCollection().toString(), new Integer(1));
      }
    }
  }

  /**
   * Retrieve the contents as a map [ContentId, ContentVO]
   * 
   * @param items
   * @return
   * @throws ImejiException
   */
  private Map<String, ContentVO> retrieveContents(Collection<Item> items) throws ImejiException {
    List<String> contentIds = items.stream().map(Item::getContentId).collect(Collectors.toList());
    Map<String, ContentVO> map = new HashMap<>();
    for (ContentVO c : new ContentService().retrieveBatchLazy(contentIds)) {
      map.put(c.getId().toString(), c);
    }
    return map;
  }

  /**
   * Retrieve the items as a Map [contentId,Item]
   * 
   * @param result
   * @return
   * @throws ImejiException
   */
  private Map<String, Item> retrieveItems(List<String> ids) throws ImejiException {
    final List<Item> items = new ItemController().retrieveBatchLazy(ids, user);
    return items.stream().collect(Collectors.toMap(Item::getContentId, Function.identity()));
  }

  @Override
  public Map<String, Integer> getExportedItemsPerCollection() {
    return itemsPerCollection;
  }


}
