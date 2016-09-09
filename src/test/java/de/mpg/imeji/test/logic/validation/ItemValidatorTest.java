package de.mpg.imeji.test.logic.validation;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.controller.util.ImejiFactory;
import de.mpg.imeji.logic.validation.ValidatorFactory;
import de.mpg.imeji.logic.validation.impl.ItemValidator;
import de.mpg.imeji.logic.validation.impl.Validator;
import de.mpg.imeji.logic.validation.impl.Validator.Method;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.predefinedMetadata.ConePerson;
import de.mpg.imeji.logic.vo.predefinedMetadata.Date;
import de.mpg.imeji.logic.vo.predefinedMetadata.Geolocation;
import de.mpg.imeji.logic.vo.predefinedMetadata.License;
import de.mpg.imeji.logic.vo.predefinedMetadata.Link;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;
import de.mpg.imeji.logic.vo.predefinedMetadata.Publication;
import de.mpg.imeji.logic.vo.predefinedMetadata.Text;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata.Types;
import de.mpg.imeji.logic.vo.util.MetadataFactory;
import de.mpg.imeji.util.LocalizedString;

/**
 * Test {@link ItemValidator}
 * 
 * @author saquet
 *
 */
public class ItemValidatorTest {

  private static MetadataProfile profile;
  private static Validator<Item> validator;
  private static Statement TEXT_PREDEFINED =
      newStatement(Types.TEXT, "text", Arrays.asList(ValidatorPredefinedValues.TEXT1.value(),
          ValidatorPredefinedValues.TEXT2.value()), false);
  private static Statement TEXT_PREDEFINED_MULTIPLE =
      newStatement(Types.TEXT, "text multiple", Arrays.asList(
          ValidatorPredefinedValues.TEXT1.value(), ValidatorPredefinedValues.TEXT2.value()), true);
  private static Statement NUMBER_PREDEFINED = newStatement(Types.NUMBER, "number predefined",
      Arrays.asList(ValidatorPredefinedValues.NUMBER1.value(),
          ValidatorPredefinedValues.NUMBER2.value()),
      false);
  private static Statement NUMBER_PREDEFINED_MULTIPLE = newStatement(Types.NUMBER,
      "number predefined", Arrays.asList(ValidatorPredefinedValues.NUMBER1.value(),
          ValidatorPredefinedValues.NUMBER2.value()),
      true);
  private static Statement DATE_PREDEFINED =
      newStatement(Types.DATE, "date predefined", Arrays.asList(
          ValidatorPredefinedValues.DATE1.value(), ValidatorPredefinedValues.DATE2.value()), false);
  private static Statement DATE_PREDEFINED_MULTIPLE =
      newStatement(Types.DATE, "date predefined", Arrays.asList(
          ValidatorPredefinedValues.DATE1.value(), ValidatorPredefinedValues.DATE2.value()), true);
  private static Statement LINK_PREDEFINED =
      newStatement(Types.LINK, "link predefined", Arrays.asList(
          ValidatorPredefinedValues.LINK1.value(), ValidatorPredefinedValues.LINK2.value()), false);
  private static Statement LINK_PREDEFINED_MULTIPLE =
      newStatement(Types.LINK, "link predefined", Arrays.asList(
          ValidatorPredefinedValues.LINK1.value(), ValidatorPredefinedValues.LINK2.value()), true);
  private static Statement PERSON = newStatement(Types.CONE_PERSON, "person", null, false);
  private static Statement PERSON_MULTIPLE =
      newStatement(Types.CONE_PERSON, "person multiple", null, true);
  private static Statement GEOLOCATION =
      newStatement(Types.GEOLOCATION, "geolocation", null, false);
  private static Statement GEOLOCATION_MULTIPLE =
      newStatement(Types.GEOLOCATION, "geolocation multiple", null, true);
  private static Statement LICENSE = newStatement(Types.LICENSE, "license", null, false);
  private static Statement LICENSE_MULTIPLE =
      newStatement(Types.LICENSE, "license multiple", null, true);
  private static Statement PUBLICATION =
      newStatement(Types.PUBLICATION, "publication", null, false);
  private static Statement PUBLICATION_MULTIPLE =
      newStatement(Types.PUBLICATION, "publication multiple", null, true);

  /**
   * Predefined values for the Unit Tests
   * 
   * @author saquet
   *
   */
  private static enum ValidatorPredefinedValues {
    TEXT1("value1"), TEXT2("value2"), NUMBER1("1"), NUMBER2("2"), DATE1("2015-05-24"), DATE2(
        "2015-05-28"), LINK1("http://test.org"), LINK2("https://imeji.org");
    private String value;

    ValidatorPredefinedValues(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }
  }

  @BeforeClass
  public static void init() {
    initProfile();
    validator = (Validator<Item>) ValidatorFactory.newValidator(getItem(), Method.ALL);

  }

  @Test
  public void validateEmtpy() {
    try {
      validator.validate(getItem(), profile, Method.ALL);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void validateWithCorrectPredefinedValues() {
    Item item = getItem();
    Text md1 = (Text) MetadataFactory.createMetadata(TEXT_PREDEFINED);
    md1.setText(ValidatorPredefinedValues.TEXT1.value());
    de.mpg.imeji.logic.vo.predefinedMetadata.Number md2 =
        (de.mpg.imeji.logic.vo.predefinedMetadata.Number) MetadataFactory
            .createMetadata(NUMBER_PREDEFINED);
    md2.setNumber(Double.parseDouble(ValidatorPredefinedValues.NUMBER1.value()));
    Date md3 = (Date) MetadataFactory.createMetadata(DATE_PREDEFINED);
    md3.setDate(ValidatorPredefinedValues.DATE1.value());
    Link md4 = (Link) MetadataFactory.createMetadata(LINK_PREDEFINED);
    md4.setUri(URI.create(ValidatorPredefinedValues.LINK1.value()));
    item.getMetadataSet().getMetadata().add(md1);
    item.getMetadataSet().getMetadata().add(md2);
    item.getMetadataSet().getMetadata().add(md3);
    item.getMetadataSet().getMetadata().add(md4);
    try {
      validator.validate(item, profile, Method.ALL);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }

  }

  @Test
  public void validateWithWrongPredefinedValues() {
    Item item = getItem();
    // TEXT
    Text md1 = (Text) MetadataFactory.createMetadata(TEXT_PREDEFINED);
    md1.setText(ValidatorPredefinedValues.TEXT1.value() + "QWERTY");
    item.getMetadataSet().getMetadata().add(md1);
    try {
      validator.validate(item, profile, Method.ALL);
      Assert.fail("Validation for Text with wrong predefined value not working");
    } catch (UnprocessableError e) {
      // good...
    }
    // NUMBER
    de.mpg.imeji.logic.vo.predefinedMetadata.Number md2 =
        (de.mpg.imeji.logic.vo.predefinedMetadata.Number) MetadataFactory
            .createMetadata(NUMBER_PREDEFINED);
    md2.setNumber(0);
    item.getMetadataSet().getMetadata().add(md2);
    try {
      validator.validate(item, profile, Method.ALL);
      Assert.fail("Validation for Number with wrong predefined value not working");
    } catch (UnprocessableError e) {
      // good
    }
    // Date
    Date md3 = (Date) MetadataFactory.createMetadata(DATE_PREDEFINED);
    md3.setDate("2000");
    item.getMetadataSet().getMetadata().add(md3);
    try {
      validator.validate(item, profile, Method.ALL);
      Assert.fail("Validation for Date with wrong predefined value not working");
    } catch (UnprocessableError e) {
      // good
    }
    // Link
    Link md4 = (Link) MetadataFactory.createMetadata(LINK_PREDEFINED);
    md4.setUri(URI.create(ValidatorPredefinedValues.LINK1.value() + "/wrong"));
    item.getMetadataSet().getMetadata().add(md3);
    try {
      validator.validate(item, profile, Method.ALL);
      Assert.fail("Validation for Link with wrong predefined value not working");
    } catch (UnprocessableError e) {
      // good
    }

  }

  @Test
  public void validateMultipleValueNotAllowed() {
    // Text
    Text md1 = (Text) MetadataFactory.createMetadata(TEXT_PREDEFINED);
    md1.setText(ValidatorPredefinedValues.TEXT1.value());
    validateMultipleValueNotAllowed(md1);
    // Number
    de.mpg.imeji.logic.vo.predefinedMetadata.Number md2 =
        (de.mpg.imeji.logic.vo.predefinedMetadata.Number) MetadataFactory
            .createMetadata(NUMBER_PREDEFINED);
    md2.setNumber(Double.parseDouble(ValidatorPredefinedValues.NUMBER1.value()));
    validateMultipleValueNotAllowed(md2);
    // Date
    Date md3 = (Date) MetadataFactory.createMetadata(DATE_PREDEFINED);
    md3.setDate(ValidatorPredefinedValues.DATE1.value());
    validateMultipleValueNotAllowed(md3);
    // Link
    Link md4 = (Link) MetadataFactory.createMetadata(LINK_PREDEFINED);
    md4.setUri(URI.create(ValidatorPredefinedValues.LINK1.value()));
    validateMultipleValueNotAllowed(md4);
    // Person
    ConePerson md5 = (ConePerson) MetadataFactory.createMetadata(PERSON);
    Person p = ImejiFactory.newPerson();
    p.setFamilyName("TEST");
    Organization org = ImejiFactory.newOrganization();
    org.setName("TEST");
    p.getOrganizations().add(org);
    md5.setPerson(p);
    validateMultipleValueNotAllowed(md5);
    // Geolocation
    Geolocation md6 = (Geolocation) MetadataFactory.createMetadata(GEOLOCATION);
    md6.setLatitude(48.137368);
    md6.setLongitude(11.575127);
    md6.setName("Munich");
    validateMultipleValueNotAllowed(md6);
    // License
    License md7 = (License) MetadataFactory.createMetadata(LICENSE);
    md7.setLicense("test");
    validateMultipleValueNotAllowed(md7);
    // Publication
    Publication md8 = (Publication) MetadataFactory.createMetadata(PUBLICATION);
    md8.setCitation("test");
    md8.setUri(URI.create("http://test.org"));
    validateMultipleValueNotAllowed(md8);

  }

  private void validateMultipleValueNotAllowed(Metadata md) {
    Item item = getItem();
    item.getMetadataSet().getMetadata().add(md);
    item.getMetadataSet().getMetadata().add(md);
    try {
      validator.validate(item, profile, Method.ALL);
      Assert.fail("Error validating multiple values for " + md.getTypeNamespace()
          + ": multiple value should not be allowed");
    } catch (UnprocessableError e) {
      // Good...
    }
  }

  @Test
  public void validateMultipleValueAllowed() {
    // Text
    Text md1 = (Text) MetadataFactory.createMetadata(TEXT_PREDEFINED_MULTIPLE);
    md1.setText(ValidatorPredefinedValues.TEXT1.value());
    validateMultipleValueAllowed(md1);
    // Number
    de.mpg.imeji.logic.vo.predefinedMetadata.Number md2 =
        (de.mpg.imeji.logic.vo.predefinedMetadata.Number) MetadataFactory
            .createMetadata(NUMBER_PREDEFINED_MULTIPLE);
    md2.setNumber(Double.parseDouble(ValidatorPredefinedValues.NUMBER1.value()));
    validateMultipleValueAllowed(md2);
    // Date
    Date md3 = (Date) MetadataFactory.createMetadata(DATE_PREDEFINED_MULTIPLE);
    md3.setDate(ValidatorPredefinedValues.DATE1.value());
    validateMultipleValueAllowed(md3);
    // Link
    Link md4 = (Link) MetadataFactory.createMetadata(LINK_PREDEFINED_MULTIPLE);
    md4.setUri(URI.create(ValidatorPredefinedValues.LINK1.value()));
    validateMultipleValueAllowed(md4);
    // Person
    ConePerson md5 = (ConePerson) MetadataFactory.createMetadata(PERSON_MULTIPLE);
    Person p = ImejiFactory.newPerson();
    p.setFamilyName("TEST");
    ((List<Organization>) p.getOrganizations()).get(0).setName("TEST");
    md5.setPerson(p);
    validateMultipleValueAllowed(md5);
    // Geolocation
    Geolocation md6 = (Geolocation) MetadataFactory.createMetadata(GEOLOCATION_MULTIPLE);
    md6.setLatitude(48.137368);
    md6.setLongitude(11.575127);
    md6.setName("Munich");
    validateMultipleValueAllowed(md6);
    // License
    License md7 = (License) MetadataFactory.createMetadata(LICENSE_MULTIPLE);
    md7.setLicense("test");
    validateMultipleValueAllowed(md7);
    // Publication
    Publication md8 = (Publication) MetadataFactory.createMetadata(PUBLICATION_MULTIPLE);
    md8.setCitation("test");
    md8.setUri(URI.create("http://test.org"));
    validateMultipleValueAllowed(md8);
  }

  private void validateMultipleValueAllowed(Metadata md) {
    Item item = getItem();
    item.getMetadataSet().getMetadata().add(md);
    item.getMetadataSet().getMetadata().add(md);
    try {
      validator.validate(item, profile, Method.ALL);
    } catch (UnprocessableError e) {
      Assert.fail("Error validating multiple values for " + md.getTypeNamespace()
          + ": multiple value should be allowed: " + e.getMessage());
    }
  }

  @Test
  public void validateDateWrongFormat() {
    Date md = (Date) MetadataFactory.createMetadata(DATE_PREDEFINED_MULTIPLE);
    md.setDate("not a date");
    Item item = getItem();
    item.getMetadataSet().getMetadata().add(md);
    try {
      validator.validate(item, profile, Method.ALL);
      Assert.fail("Wrong date format, should not be validated");
    } catch (UnprocessableError e) {
      // good
    }
  }

  private static Item getItem() {
    Item item = new Item();
    item.getMetadataSet().setProfile(profile.getId());
    return item;
  }

  /**
   * Init the profile with {@link Statement}
   */
  private static void initProfile() {
    profile = new MetadataProfile();
    profile.getStatements().add(TEXT_PREDEFINED);
    profile.getStatements().add(TEXT_PREDEFINED_MULTIPLE);
    profile.getStatements().add(NUMBER_PREDEFINED);
    profile.getStatements().add(NUMBER_PREDEFINED_MULTIPLE);
    profile.getStatements().add(DATE_PREDEFINED);
    profile.getStatements().add(DATE_PREDEFINED_MULTIPLE);
    profile.getStatements().add(LINK_PREDEFINED);
    profile.getStatements().add(LINK_PREDEFINED_MULTIPLE);
    profile.getStatements().add(PERSON);
    profile.getStatements().add(PERSON_MULTIPLE);
    profile.getStatements().add(GEOLOCATION);
    profile.getStatements().add(GEOLOCATION_MULTIPLE);
    profile.getStatements().add(LICENSE);
    profile.getStatements().add(LICENSE_MULTIPLE);
    profile.getStatements().add(PUBLICATION);
    profile.getStatements().add(PUBLICATION_MULTIPLE);
  }

  /**
   * Create a new {@link Statement}
   * 
   * @param type
   * @param label
   * @param predefined
   * @param multiple
   * @return
   */
  private static Statement newStatement(Types type, String label, List<String> predefined,
      boolean multiple) {
    Statement s = new Statement();
    s.setType(URI.create(type.getClazzNamespace()));
    Collection<LocalizedString> labels = new ArrayList<LocalizedString>();
    labels.add(new LocalizedString(label, "en"));
    s.setLabels(labels);
    s.setLiteralConstraints(predefined);
    if (multiple)
      s.setMaxOccurs("unbounded");
    else
      s.setMaxOccurs("1");
    return s;
  }

}
