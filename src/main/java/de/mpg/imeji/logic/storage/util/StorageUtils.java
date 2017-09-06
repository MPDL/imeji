package de.mpg.imeji.logic.storage.util;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.util.TempFileUtil;

/**
 * Util class fore the storage package
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class StorageUtils {
  private static final Logger LOGGER = Logger.getLogger(StorageUtils.class);
  /**
   * The generic mime-type, when no mime-type is known
   */
  public static final String DEFAULT_MIME_TYPE = "application/octet-stream";
  public static final String BAD_FORMAT = "bad-extension/other";
  private static Tika tika = new Tika();
  public static final MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();

  /**
   * Transform an {@link InputStream} to a {@link Byte} array
   *
   * @param stream
   * @return
   */
  public static byte[] toBytes(InputStream stream) {
    try {
      return IOUtils.toByteArray(stream);
    } catch (final IOException e) {
      LOGGER.error("Error writing stream to byte array", e);
      return new byte[0];
    }
  }

  /**
   * Write a byte array into a File
   *
   * @param bytes
   * @return
   */
  public static File toFile(byte[] bytes) {
    try {
      final File f = TempFileUtil.createTempFile("storageUtils_toFile", null);
      IOUtils.write(bytes, new FileOutputStream(f));
      return f;
    } catch (final IOException e) {
      LOGGER.error("Error creating a temp File", e);
    }
    return null;
  }

  /**
   * Write an inputStream into a File
   *
   * @param bytes
   * @return
   */
  public static File toFile(InputStream in) {
    try {
      final File f = TempFileUtil.createTempFile("storageUtils_toFile", null);
      writeInOut(in, new FileOutputStream(f), true);
      return f;
    } catch (final IOException e) {
      LOGGER.error("Error creating a temp File", e);
    }
    return null;
  }

  /**
   * Write an {@link InputStream} to an {@link OutputStream}
   *
   * @param out
   * @param input
   * @throws IOException
   */
  public static void writeInOut(InputStream in, OutputStream out, boolean close) {
    try {
      IOUtils.copyLarge(in, out);
    } catch (final IOException e) {
      throw new RuntimeException("Error writing inputstream in outputstream: ", e);
    } finally {
      IOUtils.closeQuietly(in);
      if (close) {
        IOUtils.closeQuietly(out);
      }
    }
  }

  /**
   * Read only a part of a file
   * 
   * @param in
   * @param out
   * @param close
   * @param offset
   * @param length
   */
  public static void writeInOut(InputStream in, OutputStream out, boolean close, long offset,
      long length) {
    try {
      IOUtils.copyLarge(in, out, offset, length);
    } catch (final IOException e) {
      throw new RuntimeException("Error writing inputstream in outputstream: ", e);
    } finally {
      IOUtils.closeQuietly(in);
      if (close) {
        IOUtils.closeQuietly(out);
      }
    }
  }

  /**
   * Return a {@link HttpClient} to be used in {@link Get}
   *
   * @return
   */
  public static HttpClient getHttpClient() {
    final MultiThreadedHttpConnectionManager conn = new MultiThreadedHttpConnectionManager();
    final HttpConnectionManagerParams connParams = new HttpConnectionManagerParams();
    connParams.setConnectionTimeout(5000);
    connParams.setDefaultMaxConnectionsPerHost(50);
    conn.setParams(connParams);
    return new HttpClient(conn);
  }

  /**
   * Return a {@link GetMethod} ready to use
   *
   * @param client
   * @param url
   * @return
   */
  public static GetMethod newGetMethod(HttpClient client, String url) throws ImejiException {
    final GetMethod method = new GetMethod(url);
    method.addRequestHeader("Cache-Control", "public");
    method.setRequestHeader("Connection", "close");
    return method;
  }

  /**
   * True if the Filename has an extension
   *
   * @param filename
   * @return
   */
  public static boolean hasExtension(String filename) {
    return !FilenameUtils.getExtension(filename).equals("");
  }

  /**
   * Return the extension as String
   *
   * @param mimeType
   * @return
   */
  public static String getExtension(String mimeType) {
    try {
      return allTypes.forName(mimeType).getExtension().substring(1);
    } catch (final MimeTypeException e) {
      return mimeType;
    }
  }

  /**
   * Guess the extension of a {@link File}
   *
   * @param file
   * @return
   */
  public static String guessExtension(File file) {
    try {
      final MimeType type = allTypes.forName(tika.detect(file));
      if (!type.getExtensions().isEmpty()) {
        final String ext = type.getExtensions().get(0).replace(".", "");
        if (FilenameUtils.getExtension(file.getName()).equals("smr") && "bin".equals(ext)) {
          return "smr";
        }
        return ext;
      } else {
        final String calculatedExtension = FilenameUtils.getExtension(file.getName());
        if (!isNullOrEmpty(calculatedExtension)) {
          return calculatedExtension;
        }
      }
    } catch (final Exception e) {
      LOGGER.error("Error guessing file format", e);
    }

    return BAD_FORMAT;
  }

  /**
   * Get the Mimetype of a file
   *
   * @param f
   * @return
   */
  public static String getMimeType(File f) {
    return getMimeType(guessExtension(f));
  }

  /**
   * True if 2 filename extension are the same (jpeg = jpeg = JPG, etc.)
   *
   * @param ext1
   * @param ext2
   * @return
   */
  public static boolean compareExtension(String ext1, String ext2) {
    if ("".equals(ext1.trim()) || "".equals(ext2.trim())) {
      return false;
    }
    final String mimeType1 = getMimeType(ext1.trim());
    final String mimeType2 = getMimeType(ext2.trim());
    if (DEFAULT_MIME_TYPE.equals(mimeType1) && DEFAULT_MIME_TYPE.equals(mimeType2)) {
      return ext1.equalsIgnoreCase(ext2);
    }
    return mimeType1.equals(mimeType2);
  }

  /**
   * Return the Mime Type of a file according to its format (i.e. file extension). <br/>
   * The File extension can be found via {@link FilenameUtils}
   *
   * @param extension
   * @return
   */
  public static String getMimeType(String extension) {
    if (extension != null) {
      extension = extension.toLowerCase();
    }
    if ("tif".equals(extension)) {
      return "image/tiff";
    } else if ("jpg".equals(extension) || "jpeg".equals(extension)) {
      return "image/jpeg";
    } else if ("png".equals(extension)) {
      return "image/png";
    } else if ("bmp".equals(extension)) {
      return "image/bmp";
    } else if ("gif".equals(extension)) {
      return "image/gif";
    } else if ("pdn".equals(extension)) {
      return "image/x-paintnet";
    } else if ("mov".equals(extension)) {
      return "video/quicktime";
    } else if ("avi".equals(extension)) {
      return "video/x-msvideo";
    } else if ("3gp".equals(extension)) {
      return "video/3gpp";
    } else if ("eps".equals(extension)) {
      return "application/eps";
    } else if ("ts".equals(extension)) {
      return "video/MP2T";
    } else if ("svg".equals(extension)) {
      return "image/svg+xml";
    } else if ("jp2".equals(extension) || "j2k".equals(extension) || "jpf".equals(extension)) {
      return "image/jp2";
    } else if ("mj2".equals(extension)) {
      return "image/mj2";
    } else if ("jpf".equals(extension)) {
      return "image/jpf";
    } else if ("jpx".equals(extension)) {
      return "image/jpx";
    } else if ("mpg".equals(extension)) {
      return "video/mpeg";
    } else if ("mpga".equals(extension)) {
      return "audio/mpeg";
    } else if ("nef".equals(extension)) {
      return "image/x-nikon-nef";
    } else if ("mp4".equals(extension)) {
      return "video/mp4";
    } else if ("wmv".equals(extension)) {
      return "video/x-ms-wmv";
    } else if ("webm".equals(extension)) {
      return "video/webm";
    } else if ("ogg".equals(extension)) {
      return "video/ogg";
    } else if ("flv".equals(extension)) {
      // still not support directly played in browser
      return "video/x-flv";
    } else if ("pdf".equals(extension)) {
      return "application/pdf";
    } else if ("fit".equals(extension) || "fits".equals(extension)) {
      return "application/fits";
    } else if ("mp3".equals(extension) || "mpeg".equals(extension)) {
      return "audio/mpeg";
    } else if ("wav".equals(extension)) {
      return "audio/x-wav";
    } else if ("wma".equals(extension)) {
      return "audio/x-ms-wma";
    } else if ("cmd".equals(extension)) {
      return "application/cmd";
    }
    final String calculatedMimeType = tika.detect("name." + extension);

    if ("".equals(calculatedMimeType)) {
      return "application/octet-stream";
    } else {
      return calculatedMimeType;
    }

  }

  /**
   * Remove extension if exists and update with new one
   *
   * @return update url
   */
  public static String replaceExtension(String url, String newExt) throws IOException {
    return FilenameUtils.removeExtension(url) + "." + newExt;
  }

  /**
   * Calculate the Checksum of a byte array with MD5 algorithm displayed in Hexadecimal
   *
   * @param bytes
   * @return
   * @throws IOException
   */
  public static String calculateChecksum(File file) throws ImejiException {
    try {
      FileInputStream in = new FileInputStream(file);
      String checksum = DigestUtils.md5Hex(in);
      in.close();
      return checksum;
    } catch (final Exception e) {
      throw new UnprocessableError("Error calculating the cheksum of the file: ", e);
    }
  }


  /**
   * Return the bytes from an url
   *
   * @param url
   * @return
   * @throws FileNotFoundException
   */
  public static byte[] getBytes(URL url) throws FileNotFoundException {
    return StorageUtils.toBytes(new FileInputStream(new File(url.getFile())));
  }
}
