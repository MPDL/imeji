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
 * Copyright 2006-2007 Fachinformationszentrum Karlsruhe Gesellschaft fÃ¼r
 * wissenschaftlich-technische Information mbH and Max-Planck- Gesellschaft zur FÃ¶rderung der
 * Wissenschaft e.V. All rights reserved. Use is subject to license terms.
 */
package de.mpg.imeji.test.logic.storage;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.storage.Storage;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.UploadResult;
import de.mpg.imeji.logic.storage.impl.InternalStorage;
import de.mpg.imeji.logic.storage.internal.InternalStorageManager;
import de.mpg.imeji.logic.storage.util.StorageUtils;

/**
 * Test {@link Storage}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class StorageTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(StorageTest.class);

  private static final String TEST_IMAGE = "./src/test/resources/storage/test.png";
  private static final String FILENAME = "test";
  private static final String INTERNATIONAL_CHARACHTERS =
      "japanese:ãƒ†ã‚¹ãƒˆ  chinese:å¯¦é©— yiddish:×¤Ö¼×¨×�Ö¸×‘×¢ arab:Ø§Ø®ØªØ¨Ø§Ø± bengali: à¦ªà¦°à§€à¦•à§�à¦·à¦¾";
  private static final String LONG_NAME =
      "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"
          + "0123456789012345678901234567890adasd dsdfdj ghdjghfdgh gfhg df gfhdfghdgf hisfgshdfghsdi gfhsdigf sdi gfidsf gsidfhsidf gsdih "
          + "hsgfhidsgfhdsg fh dsfshdgfhidsgfihsdgfiwuzfgisdh fg shdfg sdihfg sdihgfisdgfhsdgf ihsdg fhsdgfizsdgf zidsgfizsd fi fhsdhfgsdhfg"
          + "hgf dhfgdshfgdshfghsdg fhsdf ghsdg fsdhf gsdjgf sdjgfsd fgdszfg sdfzgsdzgf sdfg dgfhisgfigifg i";
  /**
   * Not working: * /
   */
  private static final String SPECIAL_CHARACHTERS = "!\"Â§$%&()=? '#_-.,";

  @Before
  public void cleanFiles() {
    try {
      File f = new File(PropertyReader.getProperty("imeji.storage.path"));
      if (f.exists())
        FileUtils.cleanDirectory(f);
    } catch (Exception e) {
      LOGGER.error("CleanFiles error", e);
    }
  }

  /**
   * Test for {@link InternalStorage}
   * 
   * @throws FileNotFoundException
   */
  @Test
  public void internalStorageBasic() {
    uploadReadDelete(FILENAME + ".png");
  }

  @Test
  public void internalStorageSpecialFileName() {
    uploadReadDelete(SPECIAL_CHARACHTERS + ".png");
  }

  @Test
  public void internalStorageInternationalFileName() {
    uploadReadDelete(INTERNATIONAL_CHARACHTERS + ".png");
  }

  @Test
  public void internalStorageLongFileName() {
    uploadReadDelete(LONG_NAME + ".png");
  }

  @Test
  public void testMimeTypeDetection() {
    String STATIC_CONTEXT_STORAGE = "src/test/resources/storage";
    File file = new File(STATIC_CONTEXT_STORAGE + "/test2.wrongext");
    String mimeType = StorageUtils.getMimeType(file);
    assertThat(mimeType, equalTo("image/jpeg"));
  }

  /**
   * Do upload - read - delete methods in a row
   * 
   * @param filename
   * @throws ImejiException
   */
  private void uploadReadDelete(String filename) {
    StorageController sc = new StorageController("internal");
    InternalStorageManager manager = new InternalStorageManager();
    // UPLOAD
    File file = new File(TEST_IMAGE);
    try {
      UploadResult res = sc.upload(filename, file, "1");
      Assert.assertFalse(res.getOrginal() + " url is same as path",
          res.getOrginal().equals(manager.transformUrlToPath(res.getOrginal())));
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      // READ THE URL
      sc.read(res.getOrginal(), baos, true);
      baos.toByteArray();
      byte[] stored = baos.toByteArray();
      try {
        // Test if the uploaded file is the (i.e has the same hashcode) the
        // one which has been stored
        Assert.assertTrue("Uploaded file has been modified",
            Arrays.hashCode(FileUtils.readFileToByteArray(file)) == Arrays.hashCode(stored));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      // DELETE THE FILE
      sc.delete(res.getId());
      // Test that the file has been correctly deleted (i.e, the number of
      // files in the storage is null)
      Assert.assertEquals(0, manager.getAdministrator().getNumberOfFiles());
      // Assert.assertTrue(Arrays.equals(original, stored));
      // Assert.assertTrue(Arrays.hashCode(original) ==
      // Arrays.hashCode(stored));
    } catch (ImejiException e) {
      LOGGER.info("There has been some upload error in the storage test.");
    }
  }



}
