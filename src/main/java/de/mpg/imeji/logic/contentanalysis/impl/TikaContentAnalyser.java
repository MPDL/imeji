package de.mpg.imeji.logic.contentanalysis.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

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

  @Override
  public String extractFulltext(File file) {
    try {
      BodyContentHandler handler = new BodyContentHandler(-1);
      FileInputStream stream = new FileInputStream(file);
      Metadata metadata = new Metadata();
      AutoDetectParser parser = new AutoDetectParser();
      parser.parse(stream, handler, metadata);
      return Jsoup.clean(handler.toString(), Whitelist.simpleText());
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
      BodyContentHandler handler = new BodyContentHandler(-1);
      FileInputStream is = new FileInputStream(file);
      AutoDetectParser parser = new AutoDetectParser();
      parser.parse(is, handler, metadata);
      for (String name : metadata.names()) {
        if (metadata.get(name).length() < METADATA_MAX_LENGHT) {
          techMd.add(new TechnicalMetadata(name, metadata.get(name)));
        }
      }
    } catch (Exception e) {
      LOGGER.error("Error extracting technical metadata from file", e);
    }
    return techMd;
  }

  @Override
  public ContentAnalyse extractAll(File file) {
    ContentAnalyse contentAnalyse = new ContentAnalyse();
    BodyContentHandler handler = new BodyContentHandler(-1);
    try {
      InputStream stream = TikaInputStream.get(file.toPath());
      try {

        stream = TikaInputStream.get(file.toPath());
        Metadata metadata = new Metadata();
        AutoDetectParser parser = new AutoDetectParser();
        parser.parse(stream, handler, metadata, new ParseContext());
        for (String name : metadata.names()) {
          if (metadata.get(name).length() < METADATA_MAX_LENGHT) {
            contentAnalyse.getTechnicalMetadata()
                .add(new TechnicalMetadata(name, metadata.get(name)));
          }
        }
        contentAnalyse.setFulltext(Jsoup.clean(handler.toString(), Whitelist.simpleText()));
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
}
