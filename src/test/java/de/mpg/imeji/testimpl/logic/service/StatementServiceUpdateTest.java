package de.mpg.imeji.testimpl.logic.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.core.CombinableMatcher.both;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.core.statement.StatementService;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.StatementType;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.user.UserService.USER_TYPE;
import de.mpg.imeji.test.logic.service.SuperServiceTest;

public class StatementServiceUpdateTest extends SuperServiceTest {

  private static StatementService statementService;
  private static User sysadmin;

  @BeforeClass
  public static void testSetup() throws ImejiException {
    statementService = new StatementService();
    UserService userService = new UserService();

    sysadmin = ImejiFactory.newUser().setEmail("admin4@test.org").setPerson("admin4", "admin4", "org").setPassword("password")
        .setQuota(Long.MAX_VALUE).build();
    userService.create(sysadmin, USER_TYPE.ADMIN);
  }

  @Test
  public void updateFullTextStatement() throws ImejiException {
    //Test data
    Statement createdStatement = ImejiFactory.newStatement().setIndex("Default Name").setType(StatementType.TEXT)
        .setLiteralsConstraints(Arrays.asList("Constraint A", "Constraint B")).setNamespace("Default Namespace").build();
    statementService.create(createdStatement, sysadmin);

    String name = "Updated name";
    StatementType type = StatementType.DATE;
    List<String> predefinedValues = Arrays.asList("Updated constraint");
    String namespace = "Updated namespace";

    Statement statementToUpdate =
        ImejiFactory.newStatement().setIndex(name).setType(type).setLiteralsConstraints(predefinedValues).setNamespace(namespace).build();

    //Test
    Statement updatedStatement = statementService.update(createdStatement, statementToUpdate, sysadmin);

    //Assertion
    Statement retrievedStatement = statementService.retrieve(updatedStatement.getUri().toString(), sysadmin);

    assertThat(retrievedStatement, both(hasProperty("type", is(type))).and(hasProperty("index", is(name)))
        .and(hasProperty("literalConstraints", is(predefinedValues))).and(hasProperty("namespace", is(namespace))));
  }

  //Test updateEmptyStatement

  //Test updateGeolocationStatement

  //Test updatePartialStatement

}
