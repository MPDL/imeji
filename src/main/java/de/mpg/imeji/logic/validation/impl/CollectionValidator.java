package de.mpg.imeji.logic.validation.impl;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.ContainerAdditionalInfo;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;

/**
 * {@link Validator} for {@link CollectionImeji}
 *
 * @author saquet
 *
 */
public class CollectionValidator extends ObjectValidator implements Validator<CollectionImeji> {

  private UnprocessableError exception = new UnprocessableError();
  private static final Pattern DOI_VALIDATION_PATTERN = Pattern.compile("10\\.\\d+\\/\\S+");

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
   * Valid a DOI according to predefined pattern. If not valid, add a message to the exception
   *
   * @param doi
   */
  private void validateDOI(String doi) {
    if (!isNullOrEmpty(doi) && !DOI_VALIDATION_PATTERN.matcher(doi).find()) {
      exception = new UnprocessableError("error_doi_creation_error_doi_format", exception);
    }

  }

  private void setException(UnprocessableError e) {
    this.exception = e;
  }

  private UnprocessableError getException() {
    return exception;
  }
}
