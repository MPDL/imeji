package de.mpg.imeji.presentation.edit.editItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.presentation.edit.EditMetadataAbstract;
import de.mpg.imeji.presentation.edit.MetadataInputComponent;
import de.mpg.imeji.presentation.edit.SelectStatementWithInputComponent;
import de.mpg.imeji.presentation.license.LicenseEditor;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Edit the {@link Metadata} of a single {@link Item}
 *
 * @author saquet
 *
 */
@ManagedBean(name = "EditMetadataItemBean")
@ViewScoped
public class EditMetadataItemBean extends EditMetadataAbstract {
  private static final long serialVersionUID = 4116466458089234630L;
  private static Logger LOGGER = Logger.getLogger(EditMetadataItemBean.class);
  private List<SelectStatementWithInputComponent> rows = new ArrayList<>();
  private Item item;
  private LicenseEditor licenseEditor;

  public EditMetadataItemBean() {
    super();
  }

  @PostConstruct
  public void init() {
    final String id = UrlHelper.getParameterValue("id");
    try {
      this.item = itemService.retrieve(ObjectHelper.getURI(Item.class, id), getSessionUser());
      this.licenseEditor = new LicenseEditor(getLocale(), item);
      rows = item.getMetadata().stream()
          .map(md -> new SelectStatementWithInputComponent(md, statementMap))
          .collect(Collectors.toList());
    } catch (final ImejiException e) {
      BeanHelper.error("Error retrieving item");
      LOGGER.error("Error retrieving Item with id " + id, e);
    }
  }

  @Override
  public List<Item> toItemList() {
    item.setMetadata(rows.stream().map(SelectStatementWithInputComponent::getInput)
        .map(MetadataInputComponent::getMetadata).collect(Collectors.toList()));
    item.getLicenses().add(licenseEditor.getLicense());
    return Arrays.asList(item);
  }

  @Override
  public List<Statement> getAllStatements() {
    return rows.stream().filter(row -> row.getInput() != null && !row.getInput().isEmpty())
        .map(SelectStatementWithInputComponent::getInput).map(MetadataInputComponent::getStatement)
        .collect(Collectors.toMap(Statement::getIndex, Function.identity(), (a, b) -> a)).values()
        .stream().collect(Collectors.toList());
  }

  /**
   * Add a new empty row
   */
  public void addMetadata() {
    rows.add(new SelectStatementWithInputComponent(statementMap));
  }

  /**
   * Remove a metadata
   *
   * @param index
   */
  public void removeMetadata(int index) {
    rows.remove(index);
  }

  /**
   * @return the rows
   */
  public List<SelectStatementWithInputComponent> getRows() {
    return rows;
  }

  /**
   * @param rows the rows to set
   */
  public void setRows(List<SelectStatementWithInputComponent> rows) {
    this.rows = rows;
  }

  /**
   * @return the licenseEditor
   */
  public LicenseEditor getLicenseEditor() {
    return licenseEditor;
  }

  /**
   * @param licenseEditor the licenseEditor to set
   */
  public void setLicenseEditor(LicenseEditor licenseEditor) {
    this.licenseEditor = licenseEditor;
  }



}
