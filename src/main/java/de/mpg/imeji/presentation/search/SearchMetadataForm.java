/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.search;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.model.SelectItem;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.search.model.SearchGroup;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchMetadata;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.util.DateFormatter;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.predefinedMetadata.util.MetadataTypesHelper;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * An element in the advanced search form
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchMetadataForm {
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
  public SearchMetadataForm(SearchGroup searchGroup, MetadataProfile profile) {
    this();
    for (SearchElement se : searchGroup.getElements()) {
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
    initStatement(profile, namespace);
    initOperatorMenu();
  }

  private void parseMetadata(SearchMetadata md) {
    namespace = md.getStatement().toString();
    switch (md.getField()) {
      case coordinates:
        String[] values = md.getValue().split(",");
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
      switch (MetadataTypesHelper.getTypesForNamespace(statement.getType().toString())) {
        case DATE:
          try {
            DateFormatter.format(searchValue);
          } catch (Exception e) {
            throw new UnprocessableError("error_date_format");
          }
          break;
        case GEOLOCATION:
          Set<String> messages = new HashSet<>();
          if (isEmtpyValue(latitude + longitude)) {
            break;
          }
          if (!isEmtpyValue(latitude + longitude)
              && (isEmtpyValue(latitude) || isEmtpyValue(longitude))) {
            messages.add("error_search_long_latitude_must_be_both_not_null");
          }
          if (!isEmtpyValue(latitude + longitude) && isEmtpyValue(distance)) {
            messages.add("error_search_distance_null");
          }
          try {
            if (!isEmtpyValue(latitude)) {
              Double la = Double.parseDouble(latitude);
              if (!(la >= -90 && la <= 90)) {
                messages.add("error_latitude_format");
              }
            }
          } catch (Exception e) {
            messages.add("error_latitude_format");
          }
          try {
            if (!isEmtpyValue(longitude)) {
              Double lo = Double.parseDouble(longitude);
              if (!(lo >= -180 && lo <= 180)) {
                messages.add("error_longitude_format");
              }
            }
          } catch (Exception e) {
            messages.add("error_longitude_format");
          }
          if (!messages.isEmpty()) {
            throw new UnprocessableError(messages);
          }
          break;
        case LICENSE:
          break;
        case NUMBER:
          try {
            Long.parseLong(searchValue);
          } catch (Exception e) {
            throw new UnprocessableError("error_number_format");
          }
          break;
        case CONE_PERSON:
          break;
        case PUBLICATION:
          break;
        case TEXT:
          break;
        case LINK:
          break;
      }
    }
  }

  public SearchMetadataForm(SearchMetadata metadata, MetadataProfile profile) {
    this();
    operator = metadata.getOperator();
    searchValue = metadata.getValue();
    not = metadata.isNot();
    namespace = metadata.getStatement().toString();
    initStatement(profile, namespace);
    initOperatorMenu();
  }

  /**
   * Intialize the filtrsMenu
   */
  public void initOperatorMenu() {
    operatorMenu = new ArrayList<SelectItem>();
    SessionBean sessionBean = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    switch (MetadataTypesHelper.getTypesForNamespace(statement.getType().toString())) {
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
        operatorMenu.add(new SelectItem(SearchOperators.EQUALS, sessionBean.getLabel("exactly")));
    }
  }

  /**
   * @param p
   * @param namespace
   */
  public void initStatement(MetadataProfile p, String namespace) {
    for (Statement st : p.getStatements()) {
      if (st.getId().toString().equals(namespace)) {
        statement = st;
      }
    }
    if (statement == null) {
      throw new RuntimeException(
          "Statement with namespace \"" + namespace + "\" not found in profile " + p.getId());
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
      for (String s : statement.getLiteralConstraints()) {
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
    SearchGroup group = new SearchGroup();
    if (namespace != null) {
      URI ns = URI.create(namespace);
      switch (MetadataTypesHelper.getTypesForNamespace(statement.getType().toString())) {
        case DATE:
          if (!isEmtpyValue(searchValue)) {
            group.addPair(new SearchMetadata(SearchFields.time, operator,
                DateFormatter.format(searchValue), ns, not));
          }
          break;
        case GEOLOCATION:
          if (!isEmtpyValue(searchValue + latitude + longitude)) {
            group.setNot(not);
            if (!isEmtpyValue(searchValue)) {
              group.addPair(
                  new SearchMetadata(SearchFields.location, operator, searchValue, ns, false));
            }
            if (!isEmtpyValue(latitude) && !isEmtpyValue(longitude)) {
              if (!group.isEmpty()) {
                group.addLogicalRelation(LOGICAL_RELATIONS.AND);
              }
              group.addPair(new SearchMetadata(SearchFields.coordinates, SearchOperators.EQUALS,
                  Double.parseDouble(latitude) + "," + Double.parseDouble(longitude) + ","
                      + distance,
                  ns, false));
            }
          }
          break;
        case LICENSE:
          if (!isEmtpyValue(searchValue + uri)) {
            if (!isEmtpyValue(searchValue)) {
              group.addPair(
                  new SearchMetadata(SearchFields.license, operator, searchValue, ns, not));
            }
            if (!isEmtpyValue(uri)) {
              if (!group.isEmpty())
                group.addLogicalRelation(LOGICAL_RELATIONS.AND);
              group.addPair(new SearchMetadata(SearchFields.url, operator, uri, ns, not));
            }
          }
          break;
        case NUMBER:
          if (!isEmtpyValue(searchValue)) {
            group.addPair(new SearchMetadata(SearchFields.number, operator, searchValue, ns, not));
          }
          break;
        case CONE_PERSON:
          if (!isEmtpyValue(searchValue + familyName + givenName + uri + orgName)) {
            group.setNot(not);
            if (!isEmtpyValue(searchValue)) {
              group.addPair(new SearchMetadata(SearchFields.person_completename, operator,
                  searchValue, ns, false));
            }
            if (!isEmtpyValue(familyName)) {
              if (!group.isEmpty()) {
                group.addLogicalRelation(LOGICAL_RELATIONS.AND);
              }
              group.addPair(
                  new SearchMetadata(SearchFields.person_family, operator, familyName, ns, false));
            }
            if (!isEmtpyValue(givenName)) {
              if (!group.isEmpty()) {
                group.addLogicalRelation(LOGICAL_RELATIONS.AND);
              }
              group.addPair(
                  new SearchMetadata(SearchFields.person_given, operator, givenName, ns, false));
            }
            if (!isEmtpyValue(uri)) {
              if (!group.isEmpty()) {
                group.addLogicalRelation(LOGICAL_RELATIONS.AND);
              }
              group.addPair(new SearchMetadata(SearchFields.person_id, operator, uri, ns, false));
            }
            if (!isEmtpyValue(orgName)) {
              if (!group.isEmpty()) {
                group.addLogicalRelation(LOGICAL_RELATIONS.AND);
              }
              group.addPair(
                  new SearchMetadata(SearchFields.person_org_name, operator, orgName, ns, false));
            }
          }
          break;
        case PUBLICATION:
          if (!isEmtpyValue(searchValue)) {
            group
                .addPair(new SearchMetadata(SearchFields.citation, operator, searchValue, ns, not));
          }
          break;
        case TEXT:
          if (!isEmtpyValue(searchValue)) {
            group.addPair(new SearchMetadata(SearchFields.text, operator, searchValue, ns, not));
          }
          break;
        case LINK:
          if (!isEmtpyValue(searchValue + uri)) {
            if (!isEmtpyValue(searchValue)) {
              group.addPair(new SearchMetadata(SearchFields.label, operator, searchValue, ns, not));
            }
            if (!isEmtpyValue(uri)) {
              if (!group.isEmpty()) {
                group.addLogicalRelation(LOGICAL_RELATIONS.AND);
              }
              group.addPair(new SearchMetadata(SearchFields.url, operator, uri, ns, not));
            }
          }
          break;
      }
    }
    return group;
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
    return MetadataTypesHelper.getTypesForNamespace(statement.getType().toString()).name();
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
