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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.business.ItemBusinessController;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.storage.StorageController;
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
  protected List<String> filteredResources = new ArrayList<String>();
  protected String modelURI;
  private static final Logger LOGGER = Logger.getLogger(ZIPExport.class);



  private Map<URI, Integer> itemsPerCollection;


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
    } catch (Exception e) {
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

  protected void filterResources(SearchResult sr, User user) {
    // TODO Auto-generated method stub
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
    List<String> source = sr.getResults();
    ZipOutputStream zip = new ZipOutputStream(out);
    try {
      // Create the ZIP file
      for (int i = 0; i < source.size(); i++) {
        ItemBusinessController ic = new ItemBusinessController();
        Item item = null;
        StorageController sc = null;
        try {
          item = ic.retrieve(new URI(source.get(i)), user);
          updateMetrics(item);
          sc = new StorageController();
          zip.putNextEntry(new ZipEntry(item.getFilename()));
          sc.read(item.getFullImageUrl().toString(), zip, false);
          // Complete the entry
          zip.closeEntry();
        } catch (ZipException ze) {
          if (ze.getMessage().contains("duplicate entry")) {
            String name = i + "_" + item.getFilename();
            zip.putNextEntry(new ZipEntry(name));
            sc.read(item.getFullImageUrl().toString(), zip, false);
            // Complete the entry
            zip.closeEntry();
          }
          LOGGER.error("Error zip export", ze);
        } catch (ImejiException e) {
          LOGGER.info("Could not retrieve Item for export!", e);
        } catch (URISyntaxException eui) {
          LOGGER.info("Could not create URI during retrieval and export! ", eui);
        }
      }
    } catch (IOException e) {
      LOGGER.info("Some IO Exception when exporting all images!", e);
    }

    try {
      // Complete the ZIP file
      zip.close();
    } catch (IOException ioe) {
      LOGGER.info("Could not close the ZIP File!", ioe);
    }
  }

  private void updateMetrics(Item item) {
    // only images for the moment!
    if (modelURI.equals(Imeji.imageModel)) {
      if (itemsPerCollection.containsKey(item.getCollection())) {
        int newVal = itemsPerCollection.get(item.getCollection()).intValue() + 1;
        itemsPerCollection.put(item.getCollection(), Integer.valueOf(newVal));
      } else {
        itemsPerCollection.put(item.getCollection(), new Integer(1));
      }
    }
  }

  public Map<URI, Integer> getItemsPerCollection() {
    return itemsPerCollection;
  }


}
