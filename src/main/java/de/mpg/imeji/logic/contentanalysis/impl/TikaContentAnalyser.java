package de.mpg.imeji.logic.contentanalysis.impl;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;

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

  @Override
  public String extractFulltext(File file) {
    try {
      BodyContentHandler handler = new BodyContentHandler(-1);
      FileInputStream stream = new FileInputStream(file);
      AutoDetectParser parser = new AutoDetectParser();
      Metadata metadata = new Metadata();
      parser.parse(stream, handler, metadata);
      return handler.toString();
    } catch (Exception e) {
      LOGGER.error("Error extracting fulltext", e);
    }
    return "";
  }

  @Override
  public List<TechnicalMetadata> extractTechnicalMetadata(File file) {
    List<TechnicalMetadata> techMd = new ArrayList<>();
    try {
      Metadata metadata = new Metadata();
      AutoDetectParser parser = new AutoDetectParser();
      BodyContentHandler handler = new BodyContentHandler(-1);
      FileInputStream is = new FileInputStream(file);
      parser.parse(is, handler, metadata);
      for (String name : metadata.names()) {
        techMd.add(new TechnicalMetadata(name, metadata.get(name)));
      }
    } catch (Exception e) {
      LOGGER.error("Error extracting technical metadata from file", e);
    }
    return techMd;
  }

  @Override
  public ContentAnalyse extractAll(File file) {
    ContentAnalyse contentAnalyse = new ContentAnalyse();
    try {
      Metadata metadata = new Metadata();
      AutoDetectParser parser = new AutoDetectParser();
      BodyContentHandler handler = new BodyContentHandler(-1);
      FileInputStream is = new FileInputStream(file);
      parser.parse(is, handler, metadata);
      for (String name : metadata.names()) {
        contentAnalyse.getTechnicalMetadata().add(new TechnicalMetadata(name, metadata.get(name)));
      }
      contentAnalyse.setFulltext(handler.toString());
    } catch (Exception e) {
      LOGGER.error("Error extracting fulltext/metadata from file", e);
    }
    return contentAnalyse;
  }

}
