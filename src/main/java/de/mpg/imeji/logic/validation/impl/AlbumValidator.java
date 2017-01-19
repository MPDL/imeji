package de.mpg.imeji.logic.validation.impl;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;

/**
 * {@link Validator} for {@link Album}
 *
 * @author saquet
 *
 */
public class AlbumValidator extends ContainerValidator implements Validator<Album> {
  private UnprocessableError exception = new UnprocessableError(new HashSet<String>());


  @Override
  public void validate(Album album, Method method) throws UnprocessableError {
    exception = new UnprocessableError();
    setValidateForMethod(method);

    validateContainerMetadata(album);

    final List<Person> pers = new ArrayList<Person>();

    for (final Person c : album.getPersons()) {
      final List<Organization> orgs = new ArrayList<Organization>();
      for (final Organization o : c.getOrganizations()) {
        if (!isNullOrEmpty(o.getName().trim())) {
          orgs.add(o);
        } else {
          exception = new UnprocessableError("error_organization_need_name", exception);
        }
      }

      if (!isNullOrEmpty(c.getFamilyName().trim())) {
        if (orgs.size() > 0) {
          pers.add(c);
        } else {
          exception = new UnprocessableError("error_author_need_one_organization", exception);
        }
      } else {
        exception = new UnprocessableError("error_author_need_one_family_name", exception);
      }
    }

    if (pers.isEmpty()) {
      exception = new UnprocessableError("error_album_need_one_author", exception);
    }

    if (exception.hasMessages()) {
      throw exception;
    }

  }

  @Override
  protected UnprocessableError getException() {
    return exception;
  }

  @Override
  protected void setException(UnprocessableError e) {
    this.exception = e;
  }

}
