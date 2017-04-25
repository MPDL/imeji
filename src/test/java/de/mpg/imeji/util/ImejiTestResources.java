package de.mpg.imeji.util;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

/**
 * Resources used for the imeji tests
 * 
 * @author saquet
 *
 */
public class ImejiTestResources {
  private static final File TEST = new File("src/test/resources/storage/test");
  private static final File TEST_JPG = new File("src/test/resources/storage/test.jpg");
  private static final File TEST1_JPG = new File("src/test/resources/storage/test1.jpg");
  private static final File TEST2_JPG = new File("src/test/resources/storage/test2.jpg");
  private static final File TEST3_JPG = new File("src/test/resources/storage/test3.jpg");
  private static final File TEST4_JPG = new File("src/test/resources/storage/test4.jpg");
  private static final File TEST5_JPG = new File("src/test/resources/storage/test5.jpg");
  private static final File TEST6_JPG = new File("src/test/resources/storage/test6.jpg");
  private static final File TEST7_JPG = new File("src/test/resources/storage/test7.jpg");
  private static final File TEST_PNG = new File("src/test/resources/storage/test.png");
  private static final File TEST2_PNG = new File("src/test/resources/storage/test2.png");
  private static final File TEST2_WRONG_EXT = new File("src/test/resources/storage/test2.wrongext");
  private static final File TEST_EXE = new File("src/test/resources/storage/test.exe");

  public static File getTestPng() {
    return copyFile(TEST_PNG);
  }

  public static File getTestJpg() {
    return copyFile(TEST_JPG);
  }

  /**
   * @return the test
   */
  public static File getTest() {
    return copyFile(TEST);
  }


  /**
   * @return the test1Jpg
   */
  public static File getTest1Jpg() {
    return copyFile(TEST1_JPG);
  }

  /**
   * @return the test2Jpg
   */
  public static File getTest2Jpg() {
    return copyFile(TEST2_JPG);
  }

  /**
   * @return the test3Jpg
   */
  public static File getTest3Jpg() {
    return copyFile(TEST3_JPG);
  }

  /**
   * @return the test4Jpg
   */
  public static File getTest4Jpg() {
    return copyFile(TEST4_JPG);
  }

  /**
   * @return the test5Jpg
   */
  public static File getTest5Jpg() {
    return copyFile(TEST5_JPG);
  }

  /**
   * @return the test6Jpg
   */
  public static File getTest6Jpg() {
    return copyFile(TEST6_JPG);
  }

  /**
   * @return the test7Jpg
   */
  public static File getTest7Jpg() {
    return copyFile(TEST7_JPG);
  }


  /**
   * @return the test2Png
   */
  public static File getTest2Png() {
    return copyFile(TEST2_PNG);
  }

  /**
   * @return the test2WrongExt
   */
  public static File getTest2WrongExt() {
    return copyFile(TEST2_WRONG_EXT);
  }

  /**
   * @return the testExe
   */
  public static File getTestExe() {
    return copyFile(TEST_EXE);
  }

  private static synchronized File copyFile(File f) {
    try {
      File tmp = File.createTempFile(f.getName(), "." + FilenameUtils.getExtension(f.getName()));
      // File copyWithSameName = new File(tmp.getAbsolutePath().replace(tmp.getName(),
      // f.getName()));
      // FileUtils.copyFile(f, copyWithSameName);
      FileUtils.copyFile(f, tmp);
      return tmp;
    } catch (Exception e) {
      throw new RuntimeException("Error copying file", e);
    }
  }
}
