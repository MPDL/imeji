package de.mpg.imeji.logic.validation.impl;

import java.util.regex.Pattern;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.model.Organization;
import de.mpg.imeji.logic.model.Person;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * Validator for person
 *
 * @author bastiens
 *
 */
public class PersonValidator extends ObjectValidator implements Validator<Person> {

  @Override
  public void validate(Person p, Method method) throws UnprocessableError {
    UnprocessableError e = new UnprocessableError();
    if (StringHelper.isNullOrEmptyTrim(p.getFamilyName())) {
      e = new UnprocessableError("error_author_need_one_family_name", e);
    }
    if (!hasAtLeastOneOrganisation(p)) {
      e = new UnprocessableError("error_author_need_one_organization", e);
    }
    if (!isValidORCID(p)) {
      e = new UnprocessableError("error_orcid_format", e);
    }
    /*
     * for (final Organization org : p.getOrganizations()) { if
     * (!isValidOrganization(org)) { e = new
     * UnprocessableError("error_organization_need_name", e); break; } }
     */
    if (e.hasMessages()) {
      throw e;
    }
  }

  /**
   * True if the person has at least one valid org
   *
   * @param p
   * @return
   */
  private boolean hasAtLeastOneOrganisation(Person p) {
    for (final Organization org : p.getOrganizations()) {
      if (isValidOrganization(org)) {
        return true;
      }
    }
    return false;
  }

  /**
   * True if the organization has a name
   *
   * @param o
   * @return
   */
  private boolean isValidOrganization(Organization o) {
    return !StringHelper.isNullOrEmptyTrim(o.getName());
  }

  /**
   * True if ORCID is valid
   *
   * @param p
   * @return
   */
  private boolean isValidORCID(Person p) {
    String ORCID_STRING = "(\\d{4}-){3}\\d{3}[\\dX]";
    final Pattern orcidPattern = Pattern.compile(ORCID_STRING);
    return orcidPattern.matcher(p.getOrcid()).matches();
  }

}
