package de.mpg.imeji.logic.contentanalysis;

import org.apache.tika.sax.ContentHandlerDecorator;
import org.xml.sax.SAXException;

public class BodyContentHandlerImejiDecorator extends ContentHandlerDecorator {
  /**
   * The maximum number of characters to write to the character stream. Set to -1 for no limit.
   */
  private final int writeLimit;
  /**
   * Number of characters written so far.
   */
  private int writeCount = 0;

  public BodyContentHandlerImejiDecorator(int writeLimit) {
    this.writeLimit = writeLimit;
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    if (writeLimit == -1 || writeCount + length <= writeLimit) {
      super.characters(ch, start, length);
      writeCount += length;
    } else {
      // super.characters(ch, start, writeLimit - writeCount);
      // writeCount = writeLimit;
      return;
    }
  }

}
