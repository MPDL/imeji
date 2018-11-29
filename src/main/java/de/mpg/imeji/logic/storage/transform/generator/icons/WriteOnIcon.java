package de.mpg.imeji.logic.storage.transform.generator.icons;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;

/**
 * Represents an icon that can be loaded and written on. User can write customized text on an icon
 * and set font size, color, position etc. of the written content.
 * 
 * @author breddin
 *
 */

public class WriteOnIcon extends ImejiFileIcon {

  /**
   * Coordinates: Where to write text on the image
   */
  Point textPosition;

  /**
   * Font of written text
   */
  private Font font;
  private Color textColor;
  private int maxTextLength;

  public WriteOnIcon(String iconName, Point position, Font font, Color textColor, int maxTextLength) {
    super(iconName);
    this.textPosition = position;
    this.font = font; // new Font("Serif", Font.BOLD, fontSize);
    this.textColor = textColor;
    this.maxTextLength = maxTextLength;
  }

  public Color getTextColor() {
    return this.textColor;
  }

  public Font getTextFont() {
    return this.font;
  }

  public int getXPosition() {
    return this.textPosition.x;
  }

  public int getYPosition() {
    return this.textPosition.y;
  }

  public int getMaxTextLength() {
    return this.maxTextLength;
  }
}
