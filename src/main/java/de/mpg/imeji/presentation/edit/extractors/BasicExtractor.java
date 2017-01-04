/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.edit.extractors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.vo.Item;

/**
 * Extract technical metadata with {@link ImageIO}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class BasicExtractor {
  /**
   * Extract Metadata from one {@link Item} with {@link ImageIO}
   *
   * @param item
   * @return
   * @throws Exception
   */
  public static List<String> extractTechMd(Item item) throws Exception {
    final List<String> techMd = new ArrayList<String>();
    final URI uri = item.getFullImageUrl();
    final String imageUrl = uri.toURL().toString();
    final StorageController sc = new StorageController();
    final ByteArrayOutputStream bous = new ByteArrayOutputStream();
    sc.read(imageUrl, bous, true);
    final InputStream input = new ByteArrayInputStream(bous.toByteArray());
    final ImageInputStream iis = ImageIO.createImageInputStream(input);
    final Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
    if (readers.hasNext()) {
      // pick the first available ImageReader
      final ImageReader reader = readers.next();
      // attach source to the reader
      reader.setInput(iis, true);
      // read metadata of first image
      final IIOMetadata metadata = reader.getImageMetadata(0);
      final String[] names = metadata.getMetadataFormatNames();
      final int length = names.length;
      for (int i = 0; i < length; i++) {
        displayMetadata(techMd, metadata.getAsTree(names[i]));
      }
    }
    return techMd;
  }

  /**
   * Format the metadata in a convenient xml format for user
   *
   * @param techMd
   * @param root
   */
  private static void displayMetadata(List<String> techMd, Node root) {
    displayMetadata(techMd, root, 0);
  }

  private static void indent(List<String> techMd, StringBuilder sb, int level) {
    for (int i = 0; i < level; i++) {
      sb.append("    ");
    }
  }

  /**
   * Indent the the technical metadata which are diplayed in xml
   *
   * @param techMd
   * @param node
   * @param level
   */
  static void displayMetadata(List<String> techMd, Node node, int level) {
    final StringBuilder sb = new StringBuilder();
    // print open tag of element
    indent(techMd, sb, level);
    sb.append("<" + node.getNodeName());
    final NamedNodeMap map = node.getAttributes();
    if (map != null) {
      // print attribute values
      final int length = map.getLength();
      for (int i = 0; i < length; i++) {
        final Node attr = map.item(i);
        sb.append(" " + attr.getNodeName() + "=\"" + attr.getNodeValue() + "\"");
      }
    }
    Node child = node.getFirstChild();
    if (child == null) {
      // no children, so close element and return
      sb.append("/>");
      techMd.add(sb.toString());
      sb.delete(0, sb.length());
      return;
    }
    // children, so close current tag
    sb.append(">");
    techMd.add(sb.toString());
    sb.delete(0, sb.length());
    while (child != null) {
      // print children recursively
      displayMetadata(techMd, child, level + 1);
      child = child.getNextSibling();
    }
    // print close tag of element
    indent(techMd, sb, level);
    sb.append("</" + node.getNodeName() + ">");
    techMd.add(sb.toString());
    sb.delete(0, sb.length());
  }
}
