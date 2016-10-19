package de.mpg.imeji.logic.contentanalysis.impl;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;

import de.mpg.imeji.logic.contentanalysis.ContentAnalyse;
import de.mpg.imeji.logic.contentanalysis.ContentAnalyser;
import de.mpg.imeji.logic.vo.TechnicalMetadata;

/**
 * A content analyser based on apache Tika
 * 
 * @author saquet
 *
 */
public class TikaContentAnalyser implements ContentAnalyser {
  private static final Logger LOGGER = Logger.getLogger(TikaContentAnalyser.class);
  // Avoid too long technical metadata, to reduce performance issues
  private static final int METADATA_MAX_LENGHT = 250;
  // Limit the sire of the body parsed, too avoid heap space out of memory
  private static final int BODY_MAX_LENGHT = 1000000;

  @Override
  public String extractFulltext(File file) {
    Tika tika = new Tika();
    ContentAnalyse contentAnalyse = new ContentAnalyse();
    Metadata metadata = new Metadata();
    try {
      InputStream stream = TikaInputStream.get(file.toPath());
      try {
        stream = TikaInputStream.get(file.toPath());
        contentAnalyse
            .setFulltext(cleanText(tika.parseToString(stream, metadata, BODY_MAX_LENGHT)));
      } catch (Exception e) {
        LOGGER.error("Error extracting fulltext/metadata from file", e);
      } finally {
        stream.close();
      }
    } catch (Exception e) {
      LOGGER.error("Error closing stream", e);
    }
    return contentAnalyse.getFulltext();
  }

  @Override
  public List<TechnicalMetadata> extractTechnicalMetadata(File file) {
    Tika tika = new Tika();
    ContentAnalyse contentAnalyse = new ContentAnalyse();
    Metadata metadata = new Metadata();
    try {
      InputStream stream = TikaInputStream.get(file.toPath());
      Reader reader = null;
      try {
        stream = TikaInputStream.get(file.toPath());
        reader = tika.parse(stream, metadata);
        for (String name : metadata.names()) {
          if (metadata.get(name).length() < METADATA_MAX_LENGHT) {
            contentAnalyse.getTechnicalMetadata()
                .add(new TechnicalMetadata(name, metadata.get(name)));
          }
        }
      } catch (Exception e) {
        LOGGER.error("Error extracting fulltext/metadata from file", e);
      } finally {
        stream.close();
        if (reader != null) {
          reader.close();
        }
      }
    } catch (Exception e) {
      LOGGER.error("Error closing stream", e);
    }
    return contentAnalyse.getTechnicalMetadata();
  }

  @Override
  public ContentAnalyse extractAll(File file) {
    Tika tika = new Tika();
    ContentAnalyse contentAnalyse = new ContentAnalyse();
    Metadata metadata = new Metadata();
    try {
      InputStream stream = TikaInputStream.get(file.toPath());
      try {
        stream = TikaInputStream.get(file.toPath());
        contentAnalyse
            .setFulltext(cleanText(tika.parseToString(stream, metadata, BODY_MAX_LENGHT)));
        for (String name : metadata.names()) {
          if (metadata.get(name).length() < METADATA_MAX_LENGHT) {
            contentAnalyse.getTechnicalMetadata()
                .add(new TechnicalMetadata(name, metadata.get(name)));
          }
        }
      } catch (Exception e) {
        LOGGER.error("Error extracting fulltext/metadata from file", e);
      } finally {
        stream.close();
      }
    } catch (Exception e) {
      LOGGER.error("Error closing stream", e);
    }
    return contentAnalyse;
  }

  /**
   * Clean the text to make it better for search
   * 
   * @param text
   * @return
   * @throws IOException
   */
  private String cleanText(String text) throws IOException {
    BufferedReader reader = new BufferedReader(new StringReader(text));
    ByteArrayOutputStream bous = new ByteArrayOutputStream();
    String line = "";
    while ((line = reader.readLine()) != null) {
      bous.write((line + " ").getBytes(Charset.forName("UTF-8")));
    }
    return bous.toString("UTF-8");
  }
}
