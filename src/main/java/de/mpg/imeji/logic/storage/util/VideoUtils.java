package de.mpg.imeji.logic.storage.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.feature.local.engine.DoGSIFTEngine;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.video.Video;
import org.openimaj.video.xuggle.XuggleVideo;

import de.mpg.imeji.logic.util.TempFileUtil;
import de.mpg.imeji.presentation.util.PropertyReader;

public final class VideoUtils {
  private static final float IMAGE_DETECTION_UPPER_THRESHOLD = 0.1F;
  private static final float IMAGE_DETECTION_LOWER_THRESHOLD = 0.8F;
  private static final String IMAGE_FILE_EXTENTION = "jpg";
  private static final int SNAPSHOT_CREATION_METHOD = 0;
  private static final Logger LOGGER = Logger.getLogger(VideoUtils.class);

  private VideoUtils() {
    // private Constructor
  }

  /**
   * @return the thresholds for finding good snapshot image within the video.
   */
  private static float[] getGoodImageDetectionThreshold() {
    try {
      String threshold =
          PropertyReader.getProperty("imeji.internal.video.imagedetection.threshold");
      String[] thresholds = threshold.split(",");
      if (thresholds.length <= 0) {
        return new float[] {IMAGE_DETECTION_UPPER_THRESHOLD, IMAGE_DETECTION_LOWER_THRESHOLD};
      }
      if (thresholds.length == 1) {
        return new float[] {Float.parseFloat(thresholds[0]), IMAGE_DETECTION_LOWER_THRESHOLD};
      } else {
        return new float[] {Float.parseFloat(thresholds[0]), Float.parseFloat(thresholds[1])};
      }
    } catch (Exception e) {
      LOGGER.info("Some problems with Image detection", e);
      return new float[] {IMAGE_DETECTION_UPPER_THRESHOLD, IMAGE_DETECTION_LOWER_THRESHOLD};
    }
  }

  /**
   * @return the integer number for using method extracting the snapshot image.
   */
  private static int getSnapshotCreationMethod() {
    try {
      return Integer
          .parseInt(PropertyReader.getProperty("imeji.internal.video.imagedetection.method"));
    } catch (Exception e) {
      LOGGER.info("Could not get snapshot creation method", e);
      return VideoUtils.SNAPSHOT_CREATION_METHOD;
    }
  }

  /**
   * Gets byte array of an snapshot image from provided URL video
   * 
   * @param url
   * @return byte array of an image from video file
   * @throws FileNotFoundException
   * @throws IOException
   */
  public static byte[] videoToImageBytes(URL url) throws FileNotFoundException, IOException {
    return VideoUtils.videoFileToByteAray(url, VideoUtils.getGoodImageDetectionThreshold(),
        VideoUtils.IMAGE_FILE_EXTENTION);
  }

  /**
   * Gets byte array of an snapshot image from provided URL video
   * 
   * @param md_url
   * @return byte array of an image from video file
   * @throws FileNotFoundException
   * @throws IOException
   */
  public static byte[] videoToImageBytes(File file) throws FileNotFoundException, IOException {
    return VideoUtils.videoFileToByteAray(file, VideoUtils.getGoodImageDetectionThreshold(),
        VideoUtils.IMAGE_FILE_EXTENTION);
  }

  /**
   * Gets byte array of an snapshot image from provided video as byte array
   * 
   * @param bytes
   * @return byte array of an image from video file
   * @throws FileNotFoundException
   * @throws IOException
   */
  public static byte[] videoToImageBytes(byte[] bytes) throws FileNotFoundException, IOException {
    return VideoUtils.videoFileToByteAray(bytes, VideoUtils.getGoodImageDetectionThreshold(),
        VideoUtils.IMAGE_FILE_EXTENTION);
  }

  /**
   * Gets byte array of an snapshot image from provided url video
   * 
   * @param md_url
   * @param threshold
   * @param fileExtention
   * @return byte array of an image from video file
   * @throws IOException
   */
  public static byte[] videoFileToByteAray(byte[] bytes, float[] threshold, String fileExtention)
      throws IOException {
    File tempFile = TempFileUtil.createTempFile("videoFileToByteAray", "." + fileExtention);
    FileOutputStream fos = new FileOutputStream(tempFile);
    try {
      fos.write(bytes);
    } finally {
      fos.close();
    }
    switch (VideoUtils.getSnapshotCreationMethod()) {
      case 0:
        return VideoUtils.videoFileToByteAray(new XuggleVideo(tempFile), threshold, fileExtention);
      case 1:
        return VideoUtils.videoFileToByteArayUseFeatureExtraction(new XuggleVideo(tempFile),
            threshold, fileExtention);
      default:
        return VideoUtils.videoFileToByteAray(new XuggleVideo(tempFile), threshold, fileExtention);
    }
  }

  /**
   * Gets byte array of an snapshot image from provided url video
   * 
   * @param url
   * @param threshold
   * @param fileExtention
   * @return byte array of an image from video file
   * @throws IOException
   */
  public static byte[] videoFileToByteAray(URL url, float[] threshold, String fileExtention)
      throws IOException {
    switch (VideoUtils.getSnapshotCreationMethod()) {
      case 0:
        return VideoUtils.videoFileToByteAray(new XuggleVideo(url), threshold, fileExtention);
      case 1:
        return VideoUtils.videoFileToByteArayUseFeatureExtraction(new XuggleVideo(url), threshold,
            fileExtention);
      default:
        return VideoUtils.videoFileToByteAray(new XuggleVideo(url), threshold, fileExtention);
    }
  }

  /**
   * Gets byte array of an snapshot image from provided url video
   * 
   * @param md_url
   * @param threshold
   * @param fileExtention
   * @return byte array of an image from video file
   * @throws IOException
   */
  public static byte[] videoFileToByteAray(File file, float[] threshold, String fileExtention)
      throws IOException {
    switch (VideoUtils.getSnapshotCreationMethod()) {
      case 0:
        return VideoUtils.videoFileToByteAray(new XuggleVideo(file), threshold, fileExtention);
      case 1:
        return VideoUtils.videoFileToByteArayUseFeatureExtraction(new XuggleVideo(file), threshold,
            fileExtention);
      default:
        return VideoUtils.videoFileToByteAray(new XuggleVideo(file), threshold, fileExtention);
    }
  }

  /**
   * This method gets a snapshot image as byte array using threshold bounding algorithms to
   * validate, whether a good candidate image is found.
   * 
   * @param video
   * @param threshold, for upper and lower bound
   * @param fileExtention
   * @return a byte array of an image
   * @throws IOException
   */
  private static byte[] videoFileToByteAray(Video<MBFImage> video, float[] threshold,
      String fileExtention) throws IOException {
    for (MBFImage mbfImage : video) {
      // finding good snapshot image candidate simple algorithm
      float d = 0;
      float c = 1;
      float m = 0;
      for (int x = 0; x < mbfImage.getWidth(); x++) {
        for (int y = 0; y < mbfImage.getHeight(); y++) {
          Float[] t = mbfImage.getPixel(x, y);
          d += (t[0] + t[1] + t[2]) / 3.0;
          // normalize m
          m = d / c++;
        }
      }
      float lb = Math.min(threshold[0], threshold[1]);
      float ub = Math.max(threshold[0], threshold[1]);
      if (lb < 0 || lb > 1) {
        lb = VideoUtils.IMAGE_DETECTION_LOWER_THRESHOLD;
      }
      if (ub < 0 || ub > 1) {
        ub = VideoUtils.IMAGE_DETECTION_UPPER_THRESHOLD;
      }
      if (m >= lb && m <= ub) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
          ImageUtilities.write(mbfImage, fileExtention, baos);
          return baos.toByteArray();
        } finally {
          baos.flush();
          baos.close();
        }
      }
    }
    return null;
  }

  /**
   * This method gets a snapshot image as byte array using threshold bounding algorithms to
   * validate.
   * 
   * @param video
   * @param threshold, describe feature threshold
   * @param fileExtention
   * @return a byte array of an image
   * @throws IOException
   */
  private static byte[] videoFileToByteArayUseFeatureExtraction(Video<MBFImage> video,
      float[] threshold, String fileExtention) throws IOException {
    DoGSIFTEngine engine = new DoGSIFTEngine();
    for (MBFImage mbfImage : video) {
      // finding good snapshot image candidate using difference of gaussian algorithm
      LocalFeatureList<Keypoint> queryKeypoints = engine.findFeatures(mbfImage.flatten());
      if (queryKeypoints.size() > (int) threshold[0]) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
          ImageUtilities.write(mbfImage, fileExtention, baos);
          return baos.toByteArray();
        } finally {
          baos.flush();
          baos.close();
        }
      }
    }
    return null;
  }
}
