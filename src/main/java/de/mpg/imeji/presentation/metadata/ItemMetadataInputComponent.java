package de.mpg.imeji.presentation.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;

/**
 * Component to edit all {@link Metadata} of an {@link Item}
 * 
 * @author saquet
 *
 */
public class ItemMetadataInputComponent implements Serializable {
  private static final long serialVersionUID = 3993542405858564526L;
  private List<MetadataInputComponent> metadata = new ArrayList<>();
  private Item item;

  public ItemMetadataInputComponent(Item item, Map<String, Statement> statementMap) {
    this.item = item;
    for (Metadata md : item.getMetadata()) {
      metadata.add(new MetadataInputComponent(md, statementMap.get(md.getStatementId())));
    }
    addEmtpyMetadata(statementMap);
  }

  /**
   * Add an empty MetadataInputComponent to the list
   * 
   * @param statementMap
   */
  private void addEmtpyMetadata(Map<String, Statement> statementMap) {
    Statement defaultStatement = statementMap.values().iterator().next();
    Metadata emtpyMd = ImejiFactory.newMetadata(defaultStatement).build();
    metadata.add(new MetadataInputComponent(emtpyMd, defaultStatement));
  }

  /**
   * Convert the {@link ItemMetadataInputComponent} to an {@link Item}
   * 
   * @return
   */
  public Item toItem() {
    List<Metadata> metadataList = new ArrayList<>();
    for (MetadataInputComponent component : metadata) {
      metadataList.add(component.getMetadata());
    }
    item.setMetadata(metadataList);
    return item;
  }

  /**
   * @return the metadata
   */
  public List<MetadataInputComponent> getMetadata() {
    return metadata;
  }

  /**
   * @param metadata the metadata to set
   */
  public void setMetadata(List<MetadataInputComponent> metadata) {
    this.metadata = metadata;
  }



}
