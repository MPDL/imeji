package de.mpg.imeji.presentation.search.facet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.ImejiExceptionWithUserMessage;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.statement.StatementService;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.StatementType;
import de.mpg.imeji.logic.search.facet.FacetService;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.search.model.SearchCollectionMetadata;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

@ManagedBean(name = "CreateFacetBean")
@ViewScoped
public class CreateFacetBean extends SuperBean {
  private static final long serialVersionUID = 4885254101366390248L;
  private static final Logger LOGGER = LogManager.getLogger(CreateFacetBean.class);
  private String name;
  private String index;
  private String type;
  private String objecttype;
  private final FacetService service = new FacetService();
  private List<Statement> statements = new ArrayList<>();

  public CreateFacetBean() {
    try {
      this.statements = retrieveStatements();
    } catch (ImejiException e) {
      LOGGER.error("Error retrieving statements", e);
    }
  }

  /**
   * Create the facet
   */
  public void save() {
    Facet facet = new Facet();
    facet.setIndex(index);
    facet.setName(name);
    facet.setType(type);
    facet.setObjectType(objecttype);

    try {
      service.create(facet, getSessionUser());
      redirect(getNavigation().getApplicationUrl() + "facets");
    } 
    catch (final ImejiExceptionWithUserMessage exceptionWithMessage) {
        String userMessage = "Error creating facet. " + exceptionWithMessage.getUserMessage(getLocale());
        BeanHelper.error(userMessage);
        if (exceptionWithMessage.getMessage() != null) {
          LOGGER.error(exceptionWithMessage.getMessage(), exceptionWithMessage);
        } else {
          LOGGER.error(userMessage, exceptionWithMessage);
        }
      }
    catch (UnprocessableError e) {
      BeanHelper.error(e, getLocale());
    } catch (ImejiException | IOException e) {
      LOGGER.error("Error creating facet", e);
      BeanHelper.error("Error creating facet: " + e.getMessage());
    }

  }

  /**
   * Init the form facet with an index and a type
   * 
   * @param index
   * @param type
   */
  public void initFacet(String index, String type, String objectType) {
    this.type = type;
    this.index = index;
    this.objecttype = objectType;
  }

  /**
   * Init the form facet with the statement
   * 
   * @param statement
   */
  public void initFacet(Statement statement) {
    this.type = statement.getType().toString();
    this.index = statement.getSearchIndex();
    this.objecttype = Facet.OBJECTTYPE_ITEM;
  }

  /**
   * Init the form facet with the statement
   * 
   * @param statement
   */
  public void initCollectionFacet(String label) {
    this.type = StatementType.TEXT.name();
    this.index = SearchCollectionMetadata.labelToIndex(label);
    this.objecttype = Facet.OBJECTTYPE_COLLECTION;
  }

  /**
   * Get the index name of the given collection-metadata label
   * 
   * @param label of a collection-metadata
   * @return The index name
   */
  public String labelToIndex(String label) {
    return SearchCollectionMetadata.labelToIndex(label);
  }


  /**
   * True if the index is already used by a facet
   * 
   * @param index
   * @return
   */
  public boolean isUsed(String index) {
    return service.exists(index);
  }

  public List<Statement> retrieveStatements() throws ImejiException {
    return new StatementService().retrieveAll().stream().filter(s -> s.getType() != StatementType.GEOLOCATION).collect(Collectors.toList());
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the index
   */
  public String getIndex() {
    return index;
  }

  /**
   * @param index the index to set
   */
  public void setIndex(String index) {
    this.index = index;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(String type) {
    this.type = type;
  }

  public List<String> getTypeList() {
    return Stream.of(StatementType.values()).filter(t -> t != StatementType.GEOLOCATION).map(Enum::name).collect(Collectors.toList());
  }

  public SystemFacets[] getSystemFacets() {
    return SystemFacets.values();
  }

  public List<Statement> getStatements() {
    return statements;
  }

  public String getObjecttype() {
    return objecttype;
  }

  public void setObjectType(String objecttype) {
    this.objecttype = objecttype;
  }

  public enum SystemFacets {
    //Facets for COLLECTIONS
    ORGANIZATIONS("Organizations", SearchFields.author_organization_exact.getIndex(), StatementType.TEXT.name(),
        Facet.OBJECTTYPE_COLLECTION),
    TYPES("Types", SearchFields.collection_type.getIndex(), StatementType.TEXT.name(), Facet.OBJECTTYPE_COLLECTION),

    AUTHORS("Authors", SearchFields.author_completename_exact.getIndex(), StatementType.TEXT.name(), Facet.OBJECTTYPE_COLLECTION),

    //Facets for ITEMS
    COLLECTION("Collection", SearchFields.collection.name(), StatementType.TEXT.name()),
    AUTHORS_OF_COLLECTION("Collection's authors", SearchFields.collection_author.getIndex(), StatementType.TEXT.name()),
    FILETYPE("Filetype", SearchFields.filetype.getIndex(), StatementType.TEXT.name()),
    ORGANIZATION_OF_COLLECTION("Collection's organizations", SearchFields.collection_author_organisation.getIndex(),
        StatementType.TEXT.name()),
    LICENSE("License", SearchFields.license.getIndex(), StatementType.TEXT.name());

    private final String label;
    private final String index;
    private final String type;
    private String objecttype = Facet.OBJECTTYPE_ITEM;

    private SystemFacets(String label, String index, String type) {
      this.label = label;
      this.index = index;
      this.type = type;
    }

    private SystemFacets(String label, String index, String type, String objecttype) {
      this.label = label;
      this.index = index;
      this.type = type;
      this.objecttype = objecttype;
    }

    /**
     * @return the label
     */
    public String getLabel() {
      return label;
    }

    /**
     * @return the index
     */
    public String getIndex() {
      return index;
    }

    /**
     * @return the type
     */
    public String getType() {
      return type;
    }

    public String getObjecttype() {
      return objecttype;
    }

    public void setObjecttype(String objecttype) {
      this.objecttype = objecttype;
    }
  }
}
