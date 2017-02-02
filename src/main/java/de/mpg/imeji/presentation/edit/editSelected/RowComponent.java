package de.mpg.imeji.presentation.edit.editSelected;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.presentation.edit.SelectStatementWithInputComponent;

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
      List<SelectStatementWithInputComponent> columns) {
    this.item = item;
    this.filename = item.getFilename();
    cells = columns.stream().map(c -> new CellComponent(c.asStatement(), item.getMetadata()))
        .collect(Collectors.toList());
  }

  /**
   * Add a Statement
   *
   * @param statement
   */
  public void addCell(Statement statement) {
    final List<Metadata> l = new ArrayList<>();
    cells.add(new CellComponent(statement, l));
  }

  /**
   * Add a Statement with a metadata
   *
   * @param statement
   */
  public void addCell(Statement statement, Metadata metadata) {
    final List<Metadata> l = new ArrayList<>();
    l.add(metadata);
    cells.add(new CellComponent(statement, l));
  }

  /**
   * Change the Statement of all metadata with the following index
   * 
   * @param index
   * @param newStatement
   */
  public void changeStatement(String index, Statement newStatement) {
    cells.stream().filter(c -> c.getStatement().getIndex().equals(index))
        .forEach(c -> c.changeStatement(newStatement));
  }

  /**
   * Remove all Cells with this index
   *
   * @param index
   */
  public void removeCell(String index) {
    cells =
        cells.stream().filter(cell -> !index.equals(cell.getIndex())).collect(Collectors.toList());
  }

  public Item toItem() {
    item.setFilename(filename);
    final List<Metadata> metadata = new ArrayList<>();
    for (final CellComponent cell : cells) {
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
