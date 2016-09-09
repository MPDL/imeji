package de.mpg.imeji.test.logic.validation;

import static java.util.Arrays.asList;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URI;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.validation.impl.ProfileValidator;
import de.mpg.imeji.logic.validation.impl.Validator.Method;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata.Types;
import de.mpg.imeji.util.LocalizedString;

public class ProfileValidatorTest {


  private static ProfileValidator validator;
  private static MetadataProfile profile;

  /**
   * Create a new {@link Statement}
   */
  private static Statement newStatement(Types type, String label, String label2) {
    Statement s = new Statement();
    s.setType(URI.create(type.getClazzNamespace()));
    s.setLabels(asList(new LocalizedString(label, "en"), new LocalizedString(label2, "de")));
    return s;
  }

  @Before
  public void init() {
    validator = new ProfileValidator();
    profile = new MetadataProfile();
    profile.setTitle("Profile");
  }

  @Test
  public void validateUniqueness_sameLabelsInDifferentStatements() {
    profile.setStatements(asList(newStatement(Types.TEXT, "text1", "text2"),
        newStatement(Types.TEXT, "text1", "text2")));
    try {
      validator.validate(profile, Method.ALL);
      fail("Validation of uniqueness... false positive");
    } catch (UnprocessableError e) {
      assertThat(e.getMessage(), equalTo("labels_have_to_be_unique"));
    }
  }


  @Test
  public void validateLabelSyntax_numberHash() {
    profile.setStatements(asList(newStatement(Types.TEXT, "12345#text", "text")));
    try {
      validator.validate(profile, Method.ALL);
    } catch (UnprocessableError e) {
      fail("Unexpected error:" + e.getLocalizedMessage());
    }
  }

  @Test
  public void validateUniqueness_sameLanguageInOneStatement() {
    Statement s = new Statement();
    s.setType(URI.create(Types.TEXT.getClazzNamespace()));
    s.setLabels(asList(new LocalizedString("text1", "en"), new LocalizedString("text2", "en")));
    profile.setStatements(asList(s));
    try {
      validator.validate(profile, Method.ALL);
      Assert.fail("Validation of label syntax... false positive");
    } catch (UnprocessableError e) {
      assertThat(e.getMessage(), equalTo("labels_duplicate_lang"));
    }
  }

  @Test
  public void validateLabelSyntax_notNumberNumberHash() {
    profile.setStatements(asList(newStatement(Types.TEXT, "abcd12345#text", "text")));
    try {
      validator.validate(profile, Method.ALL);
    } catch (UnprocessableError e) {
      fail("Unexpected error:" + e.getLocalizedMessage());
    }

  }

}
