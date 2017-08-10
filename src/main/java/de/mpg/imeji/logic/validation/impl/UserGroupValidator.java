package de.mpg.imeji.logic.validation.impl;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.UserGroup;

/**
 * Validator for UserGroup
 *
 * @author bastiens
 *
 */
public class UserGroupValidator extends ObjectValidator implements Validator<UserGroup> {

  @Override
  public void validate(UserGroup userGroup, Method method) throws UnprocessableError {
    if (StringHelper.isNullOrEmptyTrim(userGroup.getName())) {
      throw new UnprocessableError("user_group_need_name");
    }
    if (groupNameAlreadyExists(userGroup)) {
      throw new UnprocessableError("group_name_already_exists");
    }
  }

  /**
   * True if {@link UserGroup} name already used by another {@link UserGroup}
   *
   * @param group
   * @return
   */
  public boolean groupNameAlreadyExists(UserGroup g) {
    for (final String id : ImejiSPARQL.exec(JenaCustomQueries.selectUserGroupByName(g.getName()),
        null)) {
      if (!id.equals(g.getId().toString())) {
        return true;
      }
    }
    return false;
  }

}
