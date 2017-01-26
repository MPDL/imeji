package de.mpg.imeji.presentation.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.search.model.SearchGroup;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchMetadata;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.util.DateFormatter;
import de.mpg.imeji.logic.vo.Statement;

/**
 * An element in the advanced search form
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchMetadataForm implements Serializable {

  private static final long serialVersionUID = 5066545429677597018L;
  private static final Logger LOGGER = Logger.getLogger(SearchMetadataForm.class);
  private String searchValue;
  private String familyName;
  private String givenName;
  private String orgName;
  private String uri;
  private String number;
  private String longitude;
  private String latitude;
  private String distance = "1km";
  private SearchOperators operator;
  private LOGICAL_RELATIONS logicalRelation;
  private boolean not = false;
  private String namespace;
  private List<SelectItem> operatorMenu;
  private List<SelectItem> predefinedValues;
  private Statement statement;

  /**
   * Default constructor, create empty {@link SearchMetadataForm}
   */
  public SearchMetadataForm() {
    this.logicalRelation = LOGICAL_RELATIONS.OR;
    this.operator = SearchOperators.REGEX;
  }

  /**
   * Create a new {@link SearchMetadataForm} from a {@link SearchGroup}
   *
   * @param searchGroup
   * @param profile
   */
  public SearchMetadataForm(SearchGroup searchGroup, Locale locale) {
    this();
    for (final SearchElement se : searchGroup.getElements()) {
      switch (se.getType()) {
        case PAIR:
          // No use case so far with simple pairs
          operator = ((SearchPair) se).getOperator();
          searchValue = ((SearchPair) se).getValue();
          not = ((SearchPair) se).isNot();
          break;
        case METADATA:
          parseMetadata((SearchMetadata) se);
          break;
        case LOGICAL_RELATIONS:
          logicalRelation = ((SearchLogicalRelation) se).getLogicalRelation();
          break;
        default:
          break;
      }
    }
    initStatement(namespace);
    initOperatorMenu(locale);
  }

  private void parseMetadata(SearchMetadata md) {
    namespace = md.getStatement().toString();
    switch (md.getField()) {
      case coordinates:
        final String[] values = md.getValue().split(",");
        this.distance = "1km";
        this.latitude = values[0];
        this.longitude = values[1];
        if (values.length == 3) {
          this.distance = values[2];
        }
        break;
      case person_family:
        this.familyName = md.getValue();
        break;
      case person_given:
        this.givenName = md.getValue();
        break;
      case person_id:
        this.uri = md.getValue();
        break;
      case person_org_name:
        this.orgName = md.getValue();
        break;
      case url:
        this.uri = md.getValue();
        break;
      default:
        searchValue = md.getValue();
        operator = md.getOperator();
        not = md.isNot();
        break;
    }
  }

  /**
   * Validate the search entry
   *
   * @throws UnprocessableError
   */
  public void validate() throws UnprocessableError {
    if (namespace != null) {
      switch (statement.getType()) {
        case DATE:
          try {
            DateFormatter.format(searchValue);
          } catch (final Exception e) {
            throw new UnprocessableError("error_date_format", e);
          }
          break;
        case GEOLOCATION:
          final Set<String> messages = new HashSet<>();
          if (isEmtpyValue(latitude + longitude)) {
            break;
          }
          if (!isEmtpyValue(latitude + longitude)
              && (isEmtpyValue(latitude) || isEmtpyValue(longitude))) {
            messages.add("error_long_latitude_must_be_both_not_null");
          }
          if (!isEmtpyValue(latitude + longitude) && isEmtpyValue(distance)) {
            messages.add("error_search_distance_null");
          }
          try {
            if (!isEmtpyValue(latitude)) {
              final Double la = Double.parseDouble(latitude);
              if (!(la >= -90 && la <= 90)) {
                messages.add("error_latitude_format");
              }
            }
          } catch (final Exception e) {
            messages.add("error_latitude_format");
            LOGGER.error("Search: invalid latitude", e);
          }
          try {
            if (!isEmtpyValue(longitude)) {
              final Double lo = Double.parseDouble(longitude);
              if (!(lo >= -180 && lo <= 180)) {
                messages.add("error_longitude_format");
              }
            }
          } catch (final Exception e) {
            messages.add("error_longitude_format");
            LOGGER.error("Search: invalid longitude", e);
          }
          if (!messages.isEmpty()) {
            throw new UnprocessableError(messages);
          }
          break;
        case NUMBER:
          try {
            Long.parseLong(searchValue);
          } catch (final Exception e) {
            throw new UnprocessableError("error_number_format", e);
          }
          break;
      }
    }
  }

  public SearchMetadataForm(SearchMetadata metadata, Locale locale) {
    this();
    operator = metadata.getOperator();
    searchValue = metadata.getValue();
    not = metadata.isNot();
    namespace = metadata.getStatement().toString();
    initStatement(namespace);
    initOperatorMenu(locale);
  }

  /**
   * Intialize the filtrsMenu
   */
  public void initOperatorMenu(Locale locale) {
    operatorMenu = new ArrayList<SelectItem>();
    switch (statement.getType()) {
      case DATE:
        operatorMenu.add(new SelectItem(SearchOperators.EQUALS, "="));
        operatorMenu.add(new SelectItem(SearchOperators.GREATER, ">="));
        operatorMenu.add(new SelectItem(SearchOperators.LESSER, "<="));
        break;
      case NUMBER:
        operatorMenu.add(new SelectItem(SearchOperators.EQUALS, "="));
        operatorMenu.add(new SelectItem(SearchOperators.GREATER, ">="));
        operatorMenu.add(new SelectItem(SearchOperators.LESSER, "<="));
        break;
      default:
        operatorMenu.add(new SelectItem(SearchOperators.REGEX, "--"));
        operatorMenu.add(new SelectItem(SearchOperators.EQUALS,
            Imeji.RESOURCE_BUNDLE.getLabel("exactly", locale)));
    }
  }

  /**
   * @param p
   * @param namespace
   */
  public void initStatement(String namespace) {
    if (statement == null) {
      throw new RuntimeException("Statement with namespace \"" + namespace + "\" not found ");
    }
    initPredefinedValues();
  }

  /**
   * Initialize the predefined values if there are some defined in the profile
   */
  public void initPredefinedValues() {
    if (statement.getLiteralConstraints() != null && statement.getLiteralConstraints().size() > 0) {
      predefinedValues = new ArrayList<SelectItem>();
      predefinedValues.add(new SelectItem(null, "Select"));
      for (final String s : statement.getLiteralConstraints()) {
        predefinedValues.add(new SelectItem(s, s));
      }
    } else {
      predefinedValues = null;
    }
  }

  /**
   * Return the {@link SearchMetadataForm} as a {@link SearchGroup}
   *
   * @return
   */
  public SearchGroup getAsSearchGroup() {
    try {
      final SearchGroup group = new SearchGroup();
      if (namespace != null) {
        switch (statement.getType()) {
          case DATE:
            if (!isEmtpyValue(searchValue)) {
              group.addPair(new SearchMetadata(SearchFields.time, operator, searchValue,
                  statement.getIndex(), not));
            }
            break;
          case GEOLOCATION:
            if (!isEmtpyValue(searchValue + latitude + longitude)) {
              group.setNot(not);
              if (!isEmtpyValue(searchValue)) {
                group.addPair(new SearchMetadata(SearchFields.location, operator, searchValue,
                    statement.getIndex(), false));
              }
              if (!isEmtpyValue(latitude) && !isEmtpyValue(longitude)) {
                if (!group.isEmpty()) {
                  group.addLogicalRelation(LOGICAL_RELATIONS.AND);
                }
                group.addPair(new SearchMetadata(SearchFields.coordinates,
                    SearchOperators.GEO, Double.parseDouble(latitude) + ","
                        + Double.parseDouble(longitude) + "," + distance,
                    statement.getIndex(), false));
              }
            }
            break;
          case NUMBER:
            if (!isEmtpyValue(searchValue)) {
              group.addPair(new SearchMetadata(SearchFields.number, operator, searchValue,
                  statement.getIndex(), not));
            }
            break;
          case PERSON:
            if (!isEmtpyValue(searchValue + familyName + givenName + uri + orgName)) {
              group.setNot(not);
              if (!isEmtpyValue(searchValue)) {
                group.addPair(new SearchMetadata(SearchFields.person_completename, operator,
                    searchValue, statement.getIndex(), false));
              }
              if (!isEmtpyValue(familyName)) {
                if (!group.isEmpty()) {
                  group.addLogicalRelation(LOGICAL_RELATIONS.AND);
                }
                group.addPair(new SearchMetadata(SearchFields.person_family, operator, familyName,
                    statement.getIndex(), false));
              }
              if (!isEmtpyValue(givenName)) {
                if (!group.isEmpty()) {
                  group.addLogicalRelation(LOGICAL_RELATIONS.AND);
                }
                group.addPair(new SearchMetadata(SearchFields.person_given, operator, givenName,
                    statement.getIndex(), false));
              }
              if (!isEmtpyValue(uri)) {
                if (!group.isEmpty()) {
                  group.addLogicalRelation(LOGICAL_RELATIONS.AND);
                }
                group.addPair(new SearchMetadata(SearchFields.person_id, operator, uri,
                    statement.getIndex(), false));
              }
              if (!isEmtpyValue(orgName)) {
                if (!group.isEmpty()) {
                  group.addLogicalRelation(LOGICAL_RELATIONS.AND);
                }
                group.addPair(new SearchMetadata(SearchFields.person_org_name, operator, orgName,
                    statement.getIndex(), false));
              }
            }
            break;
          case TEXT:
            if (!isEmtpyValue(searchValue)) {
              group.addPair(new SearchMetadata(SearchFields.text, operator, searchValue,
                  statement.getIndex(), not));
            }
            break;
          case URL:
            if (!isEmtpyValue(searchValue + uri)) {
              if (!isEmtpyValue(searchValue)) {
                group.addPair(new SearchMetadata(SearchFields.label, operator, searchValue,
                    statement.getIndex(), not));
              }
              if (!isEmtpyValue(uri)) {
                if (!group.isEmpty()) {
                  group.addLogicalRelation(LOGICAL_RELATIONS.AND);
                }
                group.addPair(
                    new SearchMetadata(SearchFields.url, operator, uri, statement.getIndex(), not));
              }
            }
            break;
        }
      }
      return group;
    } catch (final UnprocessableError e) {
      LOGGER.error("Error transforming search metadata form to search group", e);
      return new SearchGroup();
    }
  }

  /**
   * True if the value is emtpy for the search
   *
   * @param value
   * @return
   */
  private boolean isEmtpyValue(String value) {
    return value == null || "".equals(value.trim());
  }

  /**
   * Return the type of the current statement (text, number, etc.)
   *
   * @return
   */
  public String getType() {
    return statement.getType().name();
  }

  public String getSearchValue() {
    return searchValue;
  }

  public void setSearchValue(String searchValue) {
    this.searchValue = searchValue;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public LOGICAL_RELATIONS getLogicalRelation() {
    return logicalRelation;
  }

  public void setLogicalRelation(LOGICAL_RELATIONS lr) {
    this.logicalRelation = lr;
  }

  public SearchOperators getOperator() {
    return operator;
  }

  public void setOperator(SearchOperators op) {
    this.operator = op;
  }

  public List<SelectItem> getOperatorMenu() {
    return operatorMenu;
  }

  public void setOperatorMenu(List<SelectItem> filtersMenu) {
    this.operatorMenu = filtersMenu;
  }

  public List<SelectItem> getPredefinedValues() {
    return predefinedValues;
  }

  public void setPredefinedValues(List<SelectItem> predefinedValues) {
    this.predefinedValues = predefinedValues;
  }

  public void setInverse(String str) {
    this.not = str.equals("true");
  }

  public String getInverse() {
    return Boolean.toString(not);
  }

  /**
   * @return the familyName
   */
  public String getFamilyName() {
    return familyName;
  }

  /**
   * @param familyName the familyName to set
   */
  public void setFamilyName(String familyName) {
    this.familyName = familyName;
  }

  /**
   * @return the givenName
   */
  public String getGivenName() {
    return givenName;
  }

  /**
   * @param givenName the givenName to set
   */
  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  /**
   * @return the uri
   */
  public String getUri() {
    return uri;
  }

  /**
   * @param uri the uri to set
   */
  public void setUri(String uri) {
    this.uri = uri;
  }

  /**
   * @return the number
   */
  public String getNumber() {
    return number;
  }

  /**
   * @param number the number to set
   */
  public void setNumber(String number) {
    this.number = number;
  }

  /**
   * @return the longitude
   */
  public String getLongitude() {
    return longitude;
  }

  /**
   * @param longitude the longitude to set
   */
  public void setLongitude(String longitude) {
    this.longitude = longitude;
  }

  /**
   * @return the latitude
   */
  public String getLatitude() {
    return latitude;
  }

  /**
   * @param latitude the latitude to set
   */
  public void setLatitude(String latitude) {
    this.latitude = latitude;
  }

  /**
   * @return the orgName
   */
  public String getOrgName() {
    return orgName;
  }

  /**
   * @param orgName the orgName to set
   */
  public void setOrgName(String orgName) {
    this.orgName = orgName;
  }

  /**
   * @return the distance
   */
  public String getDistance() {
    return distance;
  }

  /**
   * @param distance the distance to set
   */
  public void setDistance(String distance) {
    this.distance = distance;
  }

}
