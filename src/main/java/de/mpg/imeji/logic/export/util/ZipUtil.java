package de.mpg.imeji.logic.export.util;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.storage.StorageController;

/**
 * Utility class for ZIP operations
 * 
 * @author saquet
 *
 */
public class ZipUtil {

	private static final Logger LOGGER = LogManager.getLogger(ZipUtil.class);

	/**
	 * Add a File to the ZipOutputStream
	 * 
	 * @param zip
	 * @param filename
	 * @param fileUrl
	 * @param position
	 * @throws IOException
	 * @throws ImejiException
	 */
	public static void addFile(ZipOutputStream zip, String filename, String fileUrl, int position)
			throws IOException, ImejiException {
		try {
			if (position > 0) {
				filename = FilenameUtils.removeExtension(filename).replace("_" + (position - 1), "") + "_" + position
						+ "." + FilenameUtils.getExtension(filename);
			}
			zip.putNextEntry(new ZipEntry(filename));
			new StorageController().read(fileUrl, zip, false);
			zip.closeEntry();
		} catch (final ZipException ze) {
			if (ze.getMessage().contains("duplicate entry")) {
				addFile(zip, filename, fileUrl, position + 1);
			} else {
				throw ze;
			}
		}
	}

	/**
	 * Add a Folder to the ZipOutputStream
	 * 
	 * @param zip
	 * @param path
	 * @param position
	 * @throws IOException
	 */
	public static String addFolder(ZipOutputStream zip, String path, int position) throws IOException {
		try {
			String newPath = path + (position > 0 ? "_" + position : "");
			zip.putNextEntry(new ZipEntry(newPath + "/"));
			zip.closeEntry();
			return newPath;
		} catch (final ZipException ze) {
			if (ze.getMessage().contains("duplicate entry")) {
				return addFolder(zip, path, position + 1);
			} else {
				throw ze;
			}
		}
	}

	public static void closeZip(ZipOutputStream zip) {
		try {
			zip.close();
		} catch (final IOException ioe) {
			LOGGER.info("Could not close the ZIP File!", ioe);
		}
	}

}
