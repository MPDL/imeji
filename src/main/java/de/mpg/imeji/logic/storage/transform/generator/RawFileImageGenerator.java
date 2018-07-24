package de.mpg.imeji.logic.storage.transform.generator;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.logic.storage.util.ImageUtils;
import de.mpg.imeji.logic.util.StorageUtils;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * {@link ImageGenerator} for all unknown/unsupported format. It creates a default image
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class RawFileImageGenerator extends AbstractWritableImageGenerator {
  private static final Logger LOGGER = Logger.getLogger(RawFileImageGenerator.class);


  
  public RawFileImageGenerator() {
	  super("file-icon", new Point(630, 700), 150, Color.WHITE, 3);
  }
  
  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.transform.ImageGenerator#generateJPG(byte[], java.lang.String)
   */
  @Override
  public File generatePreview(File file, String extension) {
	  
	 if (StringHelper.isNullOrEmptyTrim(extension)) {
		  extension = FilenameUtils.getExtension(file.getName());
	 }
	 try {    
		 File icon = super.generateAndWriteOnImage(file, extension);
		 return icon;
    } catch (IOException | URISyntaxException e) {
      LOGGER.error("Error creating icon", e);
    }
    return null;
  }

  /**
   * This {@link ImageGenerator} class creates a default icons for all file types
   * that are not supported by the other {@link ImageGenerator} classes 
   */
  @Override
  protected boolean generatorSupportsMimeType(String fileExtension) {
	
	return true;
  }

 
}
