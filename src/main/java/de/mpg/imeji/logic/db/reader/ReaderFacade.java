package de.mpg.imeji.logic.db.reader;

import java.util.Arrays;
import java.util.List;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.j2j.helper.J2JHelper;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.security.authorization.Authorization;
import de.mpg.imeji.logic.security.authorization.util.SecurityUtil;

/**
 * Facade for using {@link Reader}. Check {@link Authorization} to readen objects
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ReaderFacade implements Reader {
  private final Reader reader;

  /**
   * Constructor for a reader within a model
   */
  public ReaderFacade(String modelURI) {
    this.reader = ReaderFactory.create(modelURI);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.reader.Reader#read(java.lang.String, de.mpg.imeji.logic.vo.User,
   * java.lang.Object)
   */
  @Override
  public Object read(String uri, User user, Object o) throws ImejiException {
    o = reader.read(uri, user, o);
    checkSecurity(Arrays.asList(o), user);
    return o;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.reader.Reader#readLazy(java.lang.String, de.mpg.imeji.logic.vo.User,
   * java.lang.Object)
   */
  @Override
  public Object readLazy(String uri, User user, Object o) throws ImejiException {
    o = reader.readLazy(uri, user, o);
    if (o == null) {
      throw new NotFoundException("Object is not found or authentication is required.");
    }
    checkSecurity(Arrays.asList(o), user);
    return o;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.reader.Reader#read(java.util.List, de.mpg.imeji.logic.vo.User)
   */
  @Override
  public List<Object> read(List<Object> l, User user) throws ImejiException {
    l = reader.read(l, user);
    checkSecurity(l, user);
    return l;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.reader.Reader#readLazy(java.util.List, de.mpg.imeji.logic.vo.User)
   */
  @Override
  public List<Object> readLazy(List<Object> l, User user) throws ImejiException {
    l = reader.readLazy(l, user);
    checkSecurity(l, user);
    return l;
  }


  /**
   * @param list
   * @param user
   * @throws ImejiException
   */
  private void checkSecurity(List<Object> list, User user) throws ImejiException {
    for (int i = 0; i < list.size(); i++) {
      if (!SecurityUtil.authorization().read(user, list.get(i))) {
        final String id = J2JHelper.getId(list.get(i)).toString();
        String email = "Not logged in";
        if (user != null) {
          email = user.getEmail();
          throw new NotAllowedError(email + " not allowed to read " + id);
        } else if (user == null) {
          throw new AuthenticationError("Authentication is required for " + id);
        }
      }
    }
  }


}
