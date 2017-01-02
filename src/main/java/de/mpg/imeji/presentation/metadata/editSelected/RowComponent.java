package de.mpg.imeji.presentation.metadata.editSelected;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.logic.vo.util.MetadataUtil;
import de.mpg.imeji.presentation.metadata.StatementComponent;

/**
 * A row of the edit select item page
 * 
 * @author saquet
 *
 */
public class RowComponent implements Serializable {
  private static final long serialVersionUID = -8707140723442597728L;
  private List<CellComponent> cells = new ArrayList<>();
  private String filename;
  private final Item item;

  public RowComponent(Item item, Map<String, Statement> statementMap,
      List<StatementComponent> columns) {
    this.item = item;
    this.filename = item.getFilename();
    for (StatementComponent column : columns) {
      cells.add(new CellComponent(statementMap.get(column.getIndex()),
          getMetadataForStatement(item, statementMap.get(column.getIndex()))));
    }
  }

  private List<Metadata> getMetadataForStatement(Item item, Statement statement) {
    List<Metadata> l = new ArrayList<>();
    for (Metadata metadata : item.getMetadata()) {
      if (metadata.getStatementId().equals(statement.getIndex())
          && !MetadataUtil.isEmpty(metadata)) {
        l.add(metadata);
      }
    }
    return l;
  }

  /**
   * Add a Statement
   * 
   * @param statement
   */
  public void addCell(Statement statement) {
    List<Metadata> l = new ArrayList<>();
    l.add(ImejiFactory.newMetadata(statement).build());
    cells.add(new CellComponent(statement, l));
  }

  public Item toItem() {
    item.setFilename(filename);
    List<Metadata> metadata = new ArrayList<>();
    for (CellComponent cell : cells) {
      metadata.addAll(cell.toMetadataList());
    }
    item.setMetadata(metadata);
    return item;
  }

  /**
   * @return the cells
   */
  public List<CellComponent> getCells() {
    return cells;
  }

  /**
   * @param cells the cells to set
   */
  public void setCells(List<CellComponent> cells) {
    this.cells = cells;
  }

  /**
   * @return the filename
   */
  public String getFilename() {
    return filename;
  }

  /**
   * @param filename the filename to set
   */
  public void setFilename(String filename) {
    this.filename = filename;
  }

}
