package de.mpg.imeji.logic.validation.impl;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContainerAdditionalInfo;
import de.mpg.imeji.logic.model.Organization;
import de.mpg.imeji.logic.model.Person;
import de.mpg.imeji.logic.util.StringHelper;

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
  }

  protected void validateContainerMetadata(CollectionImeji c) {
    if (isDelete()) {
      return;
    }
    if (StringHelper.hasInvalidTags(c.getDescription())) {
      setException(new UnprocessableError("error_bad_format_description", getException()));
    }
    if (isNullOrEmpty(c.getTitle())) {
      setException(new UnprocessableError("error_collection_need_title", getException()));
    }
    validateAdditionalInfos(c);
  }

  private void validateAdditionalInfos(CollectionImeji c) {
    for (final ContainerAdditionalInfo info : c.getAdditionalInformations()) {
      if (isNullOrEmpty(info.getLabel())) {
        setException(new UnprocessableError("error_additionalinfo_need_label", getException()));
      }
      if (isNullOrEmpty(info.getText()) && isNullOrEmpty(info.getUrl())) {
        setException(new UnprocessableError("error_additionalinfo_need_value", getException()));
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

  private void setException(UnprocessableError e) {
    this.exception = e;
  }

  private UnprocessableError getException() {
    return exception;
  }
}
