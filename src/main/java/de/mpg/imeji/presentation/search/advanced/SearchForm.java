package de.mpg.imeji.presentation.search.advanced;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.search.model.SearchElement.SEARCH_ELEMENTS;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.search.advanced.group.FileTypeSearchGroup;
import de.mpg.imeji.presentation.search.advanced.group.LicenseSearchGroup;
import de.mpg.imeji.presentation.search.advanced.group.MetadataSearchGroup;
import de.mpg.imeji.presentation.search.advanced.group.TechnicalMetadataSearchGroup;
import de.mpg.imeji.presentation.search.advanced.group.TextSearchGroup;

/**
 * The form for the Advanced search. Is composed of {@link SearchGroupForm}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchForm implements Serializable {
  private static final long serialVersionUID = 9203984025130411565L;
  private static final Logger LOGGER = Logger.getLogger(SearchForm.class);
  private MetadataSearchGroup metadataSearchGroup;
  private LicenseSearchGroup licenseSearchGroup;
  private FileTypeSearchGroup fileTypeSearchGroup;
  private TechnicalMetadataSearchGroup technicalMetadataSearchGroup;
  private TextSearchGroup textSearchGroup;


  /**
   * Constructor for a {@link SearchQuery}: initialize the form from a query
   *
   * @param searchQuery
   * @param collectionsMap
   * @param profilesMap
   * @throws ImejiException
   */
  public SearchForm(SearchQuery searchQuery, Locale locale, User user) throws ImejiException {
    this.textSearchGroup = new TextSearchGroup();
    this.licenseSearchGroup = new LicenseSearchGroup(locale);
    this.fileTypeSearchGroup = new FileTypeSearchGroup(locale);
    this.metadataSearchGroup = new MetadataSearchGroup(locale);
    this.setTechnicalMetadataSearchGroup(new TechnicalMetadataSearchGroup());
    for (final SearchElement se : searchQuery.getElements()) {
      if (se.getType().equals(SEARCH_ELEMENTS.PAIR)) {
        if (((SearchPair) se).getField() == SearchFields.filetype) {
          fileTypeSearchGroup = new FileTypeSearchGroup(((SearchPair) se).getValue(), locale);
        }
        if (((SearchPair) se).getField() == SearchFields.license) {
          licenseSearchGroup = new LicenseSearchGroup(((SearchPair) se).getValue(), locale);
        }
      }
    }
  }


  /**
   * Validate the Search form according the user input
   *
   * @throws UnprocessableError
   */
  public void validate() throws UnprocessableError {
    final Set<String> messages = new HashSet<>();

    if (!messages.isEmpty()) {
      throw new UnprocessableError(messages);
    }
    if ("".equals(SearchQueryParser.transform2UTF8URL(getFormularAsSearchQuery()))) {
      throw new UnprocessableError("error_search_query_emtpy");
    }
  }

  /**
   * Transform the {@link SearchForm} in a {@link SearchQuery}
   *
   * @return
   */
  public SearchQuery getFormularAsSearchQuery() {
    try {
      SearchFactory factory = new SearchFactory();
      factory.addElement(textSearchGroup.toSearchElement(), LOGICAL_RELATIONS.AND);
      factory.addElement(metadataSearchGroup.toSearchElement(), LOGICAL_RELATIONS.AND);
      factory.addElement(fileTypeSearchGroup.toSearchElement(), LOGICAL_RELATIONS.AND);
      factory.addElement(licenseSearchGroup.toSearchElement(), LOGICAL_RELATIONS.AND);
      factory.addElement(technicalMetadataSearchGroup.toSearchElement(), LOGICAL_RELATIONS.AND);
      return factory.build();
    } catch (final UnprocessableError e) {
      LOGGER.error("Error transforming search form to searchquery", e);
      return new SearchQuery();
    }
  }

  /**
   * @return the licenseSearchGroup
   */
  public LicenseSearchGroup getLicenseSearchGroup() {
    return licenseSearchGroup;
  }

  /**
   * @param licenseSearchGroup the licenseSearchGroup to set
   */
  public void setLicenseSearchGroup(LicenseSearchGroup licenseSearchGroup) {
    this.licenseSearchGroup = licenseSearchGroup;
  }

  /**
   * @return the fileTypeSearchGroup
   */
  public FileTypeSearchGroup getFileTypeSearchGroup() {
    return fileTypeSearchGroup;
  }

  /**
   * @param fileTypeSearchGroup the fileTypeSearchGroup to set
   */
  public void setFileTypeSearchGroup(FileTypeSearchGroup fileTypeSearchGroup) {
    this.fileTypeSearchGroup = fileTypeSearchGroup;
  }

  /**
   * @return the technicalMetadataSearchGroup
   */
  public TechnicalMetadataSearchGroup getTechnicalMetadataSearchGroup() {
    return technicalMetadataSearchGroup;
  }

  /**
   * @param technicalMetadataSearchGroup the technicalMetadataSearchGroup to set
   */
  public void setTechnicalMetadataSearchGroup(
      TechnicalMetadataSearchGroup technicalMetadataSearchGroup) {
    this.technicalMetadataSearchGroup = technicalMetadataSearchGroup;
  }

  /**
   * @return the metadataSearchGroup
   */
  public MetadataSearchGroup getMetadataSearchGroup() {
    return metadataSearchGroup;
  }


  /**
   * @return the textSearchGroup
   */
  public TextSearchGroup getTextSearchGroup() {
    return textSearchGroup;
  }


  /**
   * @param textSearchGroup the textSearchGroup to set
   */
  public void setTextSearchGroup(TextSearchGroup textSearchGroup) {
    this.textSearchGroup = textSearchGroup;
  }


  /**
   * @param metadataSearchGroup the metadataSearchGroup to set
   */
  public void setMetadataSearchGroup(MetadataSearchGroup metadataSearchGroup) {
    this.metadataSearchGroup = metadataSearchGroup;
  }


}
