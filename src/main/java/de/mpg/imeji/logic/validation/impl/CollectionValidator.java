package de.mpg.imeji.logic.validation.impl;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.ImejiConfiguration;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContainerAdditionalInfo;
import de.mpg.imeji.logic.model.LinkedCollection;
import de.mpg.imeji.logic.model.Organization;
import de.mpg.imeji.logic.model.Person;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.validation.Validation;

/**
 * {@link Validator} for {@link CollectionImeji}
 *
 * @author saquet
 *
 */
public class CollectionValidator extends ObjectValidator implements Validator<CollectionImeji> {

  private UnprocessableError exception = new UnprocessableError();

  @Override
  public void validate(CollectionImeji collection, Method m) throws UnprocessableError {
    exception = new UnprocessableError();
    setValidateForMethod(m);
    validateContainerMetadata(collection);
    validateCollectionPersons(collection);
    validateDOI(collection.getDoi());
    if (exception.hasMessages()) {
      throw exception;
    }

    cleanUp(collection);
  }

  protected void validateContainerMetadata(CollectionImeji collection) {
    if (isDelete()) {
      return;
    }
    if (StringHelper.hasInvalidTags(collection.getDescription())) {
      setException(new UnprocessableError("error_bad_format_description", getException()));
    }
    if (isNullOrEmpty(collection.getTitle())) {
      setException(new UnprocessableError("error_collection_need_title", getException()));
    }
    if (!validateCollectionsTitle(collection.getTitle())) {
      setException(new UnprocessableError("error_collection_title_semicolon_not_allowed", getException()));
    }
    if (!collection.isSubCollection() && Imeji.CONFIG.getCollectionTypes() != null && !Imeji.CONFIG.getCollectionTypes().isBlank()
        && (collection.getTypes() == null || collection.getTypes().isEmpty())) {
      setException(new UnprocessableError("error_collection_need_types", getException()));
    }
    if (!validateLinkedCollections(collection)) {
      setException(new UnprocessableError("error_full_web_url_linked_collections", getException()));
    }
    validateAdditionalInfos(collection);
  }

  private void validateAdditionalInfos(CollectionImeji c) {

    for (final ContainerAdditionalInfo info : c.getAdditionalInformations()) {
      if (isNullOrEmpty(info.getLabel()) && !isNullOrEmpty(info.getText())) {
        setException(new UnprocessableError("error_additionalinfo_need_label", getException()));
      }

      if (info.getLabel().equals(ImejiConfiguration.COLLECTION_METADATA_ARTICLE_DOI_LABEL) && !isNullOrEmpty(info.getText())) {
        validateDOI(info.getText());
      }

      if (info.getLabel().equals(ImejiConfiguration.COLLECTION_METADATA_GEO_COORDINATES_LABEL) && !isNullOrEmpty(info.getText())
          && !validateGeoCoordinates(info.getText())) {
        this.exception = new UnprocessableError("error_geo_coordinates_format", exception);
      }
    }
  }

  /**
   * Validate the Persons of a {@link CollectionImeji}
   *
   * @param c
   */
  private void validateCollectionPersons(CollectionImeji c) {
    final List<Person> validPersons = new ArrayList<Person>();
    for (final Person p : c.getPersons()) {
      if (validatePerson(p)) {
        validPersons.add(p);
      }
    }
    if (validPersons.isEmpty()) {
      exception = new UnprocessableError("error_collection_need_one_author", exception);
    }
  }

  /**
   * Validate a Person
   *
   * @param p
   * @return
   */
  private boolean validatePerson(Person p) {
    validateOrgsName(p.getOrganizations());
    if (!isNullOrEmpty(p.getFamilyName().trim())) {
      if (!p.getOrganizations().isEmpty()) {
        if (p.getOrcid() != null && !p.getOrcid().isBlank() && !validateORCIDString(p.getOrcid())) {
          exception = new UnprocessableError("error_orcid_format", exception);
        }
        return true;
      } else {
        exception = new UnprocessableError("error_author_need_one_organization", exception);
      }
    } else {
      exception = new UnprocessableError("error_author_need_one_family_name", exception);
    }
    return false;
  }

  /**
   * If at least 1 organization doesn't have a name, add an exception
   *
   * @param organizations
   * @return the valid organizations
   */
  private void validateOrgsName(Collection<Organization> organizations) {
    for (final Organization o : organizations) {
      if (isNullOrEmpty(o.getName().trim())) {
        exception = new UnprocessableError("error_organization_need_name", exception);
      }
    }
  }

  /**
   * Collection's title must not contain a ';' as this is used as line separator in passing a list
   * of collection titles and uris from JSF to javascript (autocomplete for linked collections)
   * 
   * @param collectionTitle
   * @return
   */
  private boolean validateCollectionsTitle(String collectionTitle) {
    if (collectionTitle.contains(";")) {
      return false;
    }
    return true;
  }

  /**
   * Validate a DOI number
   * 
   * If DOI is not valid create an exception
   *
   * @param doi
   */
  private void validateDOI(String doi) {

    if (!isNullOrEmpty(doi) && !validateDOIComplyWithHandbook(doi)) {
      this.exception = new UnprocessableError("error_doi_creation_error_doi_format", exception);
    }

  }

  /**
   * Validate a DOI number Validation is based on the DOI Handbook, Chapter 2: Numbering
   * 
   * @see https://www.doi.org/doi_handbook/2_Numbering.html
   * 
   * @param doi
   * @return number is valid or not
   */
  private boolean validateDOIComplyWithHandbook(String doi) {

    // Check:
    // (0) Are all characters UTF-8 coded?
    // (1) Do we have a prefix and a suffix, separated by '/'
    // Separation character is the first encounter of '/'
    // (2) Does the prefix start with '10.x'
    // (3) If the prefix contains further '.'s do we have some other character
    // preceding and following it
    if (isNullOrEmpty(doi)) {
      return false;
    }

    // general: '10.' then at least one character of any kind then '/' then at least
    // one character of any kind
    String matchDOIGeneral = "10\\..+\\/.+";
    // prefix: a dot is followed by at least one other character that is not a dot.
    // The first and last character are not a dot.
    String matchDOIPrefixPointWithFollower = "([^\\.]+[\\.])*[^\\.]+";

    try {
      byte[] myBytes = doi.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      return false;
    }

    if (!Pattern.matches(matchDOIGeneral, doi)) {
      return false;
    }
    String[] prefixPostfix = doi.split("/");
    String prefix = prefixPostfix[0];
    if (!Pattern.matches(matchDOIPrefixPointWithFollower, prefix)) {
      return false;
    }

    return true;
  }

  /**
   * Validate a ORCID number
   * 
   * @param orcid
   * @return number is valid or not
   */
  private boolean validateORCIDString(String orcid) {
    String ORCID_STRING = "(\\d{4}-){3}\\d{3}[\\dX]";
    final Pattern orcidPattern = Pattern.compile(ORCID_STRING);
    if (!orcid.isEmpty()) {
      return orcidPattern.matcher(orcid).matches();
    } else {
      return false;
    }
  }

  /**
   * Validate Geo-coordinates. <br/>
   * This method checks weather the string contains latitude and longitude as decimal numbers,
   * separated by a comma. <br/>
   * 
   * The Geo-coordinates must be of the form: 'Latitude(double value from -90 to 90),
   * Longitude(double value from -180 to 180)' e.g. 48.147870, 11.576709
   * 
   * @param geoCoordinates The geoCoordinates as String, containing: latitude, longitude
   * @return geoCoordinates are valid or not
   */
  private boolean validateGeoCoordinates(String geoCoordinates) {
    if (isNullOrEmpty(geoCoordinates)) {
      return false;
    }

    //only the following characters are allowed: +, -, numbers, commas, points and whitespaces
    String matchingCharacters = "^[+-[0-9]\\,\\.\\s]*$";
    if (!Pattern.matches(matchingCharacters, geoCoordinates)) {
      return false;
    }

    //only two values separated by one comma are allowed
    String[] geoCoordinatesArray = geoCoordinates.split(",");
    if (geoCoordinatesArray.length != 2) {
      return false;
    }

    try {
      //only decimal numbers for latitude and longitude are allowed
      double latitude = Double.parseDouble(geoCoordinatesArray[0]);
      double longitude = Double.parseDouble(geoCoordinatesArray[1]);

      //latitude and longitude must match the geographic coordinates range
      return (latitude >= -90.0 && latitude <= 90.0 && longitude >= -180.0 && longitude <= 180.0);
    } catch (Exception e) {
      //Strings could not be parsed to double -> Exception -> return false
      return false;
    }
  }

  /**
   * Validate linked collections
   * 
   * @return
   */
  private boolean validateLinkedCollections(CollectionImeji collection) {

    // (1) check if all external linked collections have a correct url	  
    for (LinkedCollection linkedCollection : collection.getLinkedCollections()) {
      if (!linkedCollection.isInternalCollectionType()) {
        if (!Validation.validateURLFormat(linkedCollection.getExternalCollectionUri())) {
          return false;
        }
      }
    }
    return true;
  }

  private void setException(UnprocessableError e) {
    this.exception = e;
  }

  private UnprocessableError getException() {
    return exception;
  }

  private void cleanUp(CollectionImeji c) {
    List<ContainerAdditionalInfo> toBeRemoved = new ArrayList<>();

    for (final ContainerAdditionalInfo info : c.getAdditionalInformations()) {

      if (isNullOrEmpty(info.getText()) && isNullOrEmpty(info.getUrl())) {
        toBeRemoved.add(info);
      }
    }
    c.getAdditionalInformations().removeAll(toBeRemoved);
  }
}
