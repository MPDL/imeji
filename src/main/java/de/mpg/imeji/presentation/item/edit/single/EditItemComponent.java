package de.mpg.imeji.presentation.item.edit.single;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.License;
import de.mpg.imeji.logic.model.Metadata;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.MetadataFactory;
import de.mpg.imeji.presentation.item.edit.EditMetadataAbstract;
import de.mpg.imeji.presentation.item.edit.MetadataInputComponent;
import de.mpg.imeji.presentation.item.license.LicenseEditor;

/**
 * Edit the {@link Metadata} of a single {@link Item}
 *
 * @author saquet
 *
 */
public class EditItemComponent extends EditMetadataAbstract {
  private static final long serialVersionUID = 4116466458089234630L;
  private static final Logger LOGGER = LogManager.getLogger(EditItemComponent.class);
  private List<EditItemEntry> entries = new ArrayList<>();
  private Item item;
  private String filename;
  private LicenseEditor licenseEditor;
  private final User user;
  private final Locale locale;

  public EditItemComponent(Item item, User user, Locale locale) throws ImejiException {
    super();
    this.item = item;
    this.user = user;
    this.locale = locale;
    this.licenseEditor = new LicenseEditor(getLocale(), item);
    this.filename = item.getFilename();
    Map<String, EditItemEntry> entriesMap = getDefaultStatements().values().stream().collect(
        toMap(Statement::getIndex, s -> new EditItemEntry(new MetadataFactory().setStatementId(s.getIndex()).build(), statementMap)));
    entriesMap.keySet().removeAll(item.getMetadata().stream().map(Metadata::getIndex).collect(toList()));
    entries = entriesMap.values().stream().collect(toList());
    entries.addAll(item.getMetadata().stream().map(md -> new EditItemEntry(md, statementMap)).collect(toList()));
    addMetadata();
  }

  /**
   * @return a list with only one item, the one that was edited
   */
  @Override
  public List<Item> toItemList() {
    item.setFilename(filename);
    // append the metadata entries from GUI to the item object
    item.setMetadata(
        entries.stream().map(EditItemEntry::getInput).filter(in -> in != null).map(MetadataInputComponent::getMetadata).collect(toList()));
    List<License> licenses = new ArrayList<>(item.getLicenses());
    licenses.add(licenseEditor.getLicense());
    item.setLicenses(licenses);
    // item.getLicenses().add(licenseEditor.getLicense());
    return Arrays.asList(item);
  }

  @Override
  public List<Statement> getAllStatements() {
    return entries.stream().filter(row -> row.getInput() != null && !row.getInput().isEmpty()).map(EditItemEntry::getInput)
        .map(MetadataInputComponent::getStatement).filter(s -> s != null)
        .collect(toMap(Statement::getIndex, Function.identity(), (a, b) -> a)).values().stream().collect(Collectors.toList());
  }

  /**
   * Add a new empty row
   */
  public void addMetadata() {
    entries.add(new EditItemEntry(statementMap));
  }

  /**
   * Remove a metadata
   *
   * @param index of the metadata in the list
   */
  public void removeMetadata(int index) {
    entries.remove(index);
  }

  /**
   * Enables sorting meta data elements via drag&drop in GUI
   * 
   * @param sourceIndex index in list of the source element that got dragged
   * @param targetIndex index in list of the element on which the source element got dropped source
   *        element gets added behind the element on which it was dropped
   */
  public void changeSortOderOfMetadata(int sourceIndex, int targetIndex) {

    // we add +1 to the target index
    // goal: insert the dragged element behind the element it was dropped on
    targetIndex = targetIndex + 1;

    if (targetIndex > 0 && targetIndex <= entries.size() && sourceIndex >= 0 && sourceIndex < entries.size()) {
      EditItemEntry draggedEntry = entries.remove(sourceIndex);
      entries.add(targetIndex, draggedEntry);
    }
  }

  /**
   * @return the rows
   */
  public List<EditItemEntry> getEntries() {
    if (!entries.stream().filter(e -> e.getInput() == null).findAny().isPresent()) {
      addMetadata();
    }
    return entries;
  }

  /**
   * @param rows the rows to set
   */
  public void setEntries(List<EditItemEntry> entries) {
    this.entries = entries;
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

  @Override
  public User getSessionUser() {
    return this.user;
  }

  @Override
  public Locale getLocale() {
    return this.locale;
  }
}
