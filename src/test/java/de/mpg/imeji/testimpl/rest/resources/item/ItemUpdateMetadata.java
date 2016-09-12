package de.mpg.imeji.testimpl.rest.resources.item;

import static de.mpg.imeji.logic.util.ResourceHelper.getStringFromPath;
import static de.mpg.imeji.test.rest.resources.test.integration.MyTestContainerFactory.STATIC_CONTEXT_REST;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.httpclient.HttpStatus;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.predefinedMetadata.ConePerson;
import de.mpg.imeji.logic.vo.predefinedMetadata.Geolocation;
import de.mpg.imeji.logic.vo.predefinedMetadata.License;
import de.mpg.imeji.logic.vo.predefinedMetadata.Link;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;
import de.mpg.imeji.logic.vo.predefinedMetadata.Publication;
import de.mpg.imeji.logic.vo.predefinedMetadata.Text;
import de.mpg.imeji.logic.vo.util.ObjectHelper;
import de.mpg.imeji.rest.api.DefaultItemService;
import de.mpg.imeji.rest.process.RestProcessUtils;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemWithFileTO;
import de.mpg.imeji.rest.transfer.ReverseTransferObjectFactory;
import de.mpg.imeji.rest.transfer.ReverseTransferObjectFactory.TRANSFER_MODE;
import de.mpg.imeji.test.rest.resources.test.integration.ItemTestBase;
import util.JenaUtil;

/**
 * Created by vlad on 09.12.14.
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ItemUpdateMetadata extends ItemTestBase {

  private static final Logger LOGGER = LoggerFactory.getLogger(ItemUpdateMetadata.class);

  protected static String updateJSON;
  private static final String PATH_PREFIX = "/rest/items";



  @BeforeClass
  public static void specificSetup() throws Exception {
    updateJSON = getStringFromPath(STATIC_CONTEXT_REST + "/easyUpdateItemBasic.json");
    initCollectionWithProfile(getBasicStatements());
    initItemWithFullMetadata();
  }

  @Test
  public void test_1_UpdateItem_1_Change_Metadata_Statements_Allowed_Common()
      throws IOException, UnprocessableError, ImejiException {

    final String CHANGED = "allowed_change";
    double NUM = 90;
    final String REP_CHANGED = "$1\"" + CHANGED + "\"";
    final String NUM_CHANGED = "$1" + NUM;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    Date date = new Date();
    final String DATE_CHANGED = "$1\"" + dateFormat.format(date) + "\"";

    FormDataMultiPart multiPart = new FormDataMultiPart();

    multiPart.field("json",
        updateJSON.replace("___FILE_NAME___", CHANGED).replace("___COLLECTION_ID___", collectionId)
            .replace("___ITEM_ID___", itemId)
            // text
            .replaceAll("(\"text\"\\s*:\\s*)\"(.+)\"", REP_CHANGED)
            // person
            .replaceAll("(\"familyName\"\\s*:\\s*)\"(.+)\"", REP_CHANGED)
            .replaceAll("(\"givenName\"\\s*:\\s*)\"(.+)\"", REP_CHANGED)
            .replaceAll("(\"completeName\"\\s*:\\s*)\"(.+)\"", REP_CHANGED)
            .replaceAll("(\"alternativeName\"\\s*:\\s*)\"(.+)\"", REP_CHANGED)
            .replaceAll("(\"role\"\\s*:\\s*)\"(.+)\"", REP_CHANGED)
            // organization
            .replaceAll("(\"name\"\\s*:\\s*)\"(.+)\"", REP_CHANGED)
            .replaceAll("(\"description\"\\s*:\\s*)\"(.+)\"", REP_CHANGED)
            .replaceAll("(\"country\"\\s*:\\s*)\"(.+)\"", REP_CHANGED)
            .replaceAll("(\"city\"\\s*:\\s*)\"(.+)\"", REP_CHANGED)
            // number
            .replaceAll("(\"number\"\\s*:\\s*).+", NUM_CHANGED + ", ")
            // geo
            .replaceAll("(\"longitude\"\\s*:\\s*).+", NUM_CHANGED + ",")
            .replaceAll("(\"latitude\"\\s*:\\s*).+", NUM_CHANGED)
            // date
            .replaceAll("(\"date\"\\s*:\\s*)\"(.+)\"", DATE_CHANGED)
            // license
            .replaceAll("(\"license\"\\s*:\\s*)\"(.+)\"", REP_CHANGED)
            .replaceAll("(\"url\"\\s*:\\s*)\"(.+)\"", REP_CHANGED)
            // link
            .replaceAll("(\"link\"\\s*:\\s*)\"(.+)\"", REP_CHANGED)
            // publication
            .replaceAll("(\"format\"\\s*:\\s*)\"(.+)\"", REP_CHANGED)
            .replaceAll("(\"publication\"\\s*:\\s*)\"(.+)\"", REP_CHANGED)
            .replaceAll("(\"citation\"\\s*:\\s*)\"(.+)\"", REP_CHANGED));

    // LOGGER.info(multiPart.getField("json").getValue());

    Response response =
        target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
            .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(OK.getStatusCode(), response.getStatus());

    itemTO = (DefaultItemTO) response.readEntity(DefaultItemTO.class);

    // Read collection profile
    MetadataProfile profile = new ProfileController().retrieveByCollectionId(
        ObjectHelper.getURI(CollectionImeji.class, itemTO.getCollectionId()), JenaUtil.testUser);

    // Transfer the response (ItemTO) into a VO (Item)
    Item item = new Item();
    ReverseTransferObjectFactory.transferDefaultItem(itemTO, item, profile, JenaUtil.testUser,
        null);

    // Tests
    assertThat(itemTO.getFilename(), equalTo(CHANGED));
    List<Metadata> mds = (List<Metadata>) item.getMetadataSet().getMetadata();

    // text
    assertThat(((Text) mds.get(0)).getText(), equalTo(CHANGED));


    // geolocation
    final Geolocation geolocation = (Geolocation) mds.get(3);
    assertThat(geolocation.getName(), equalTo(CHANGED));
    assertThat(geolocation.getLatitude(), equalTo(NUM));
    assertThat(geolocation.getLongitude(), equalTo(NUM));

    // number
    assertThat(((de.mpg.imeji.logic.vo.predefinedMetadata.Number) mds.get(1)).getNumber(),
        equalTo(NUM));

    // person
    Person person = ((ConePerson) mds.get(2)).getPerson();
    assertThat(person.getFamilyName(), equalTo(CHANGED));
    assertThat(person.getGivenName(), equalTo(CHANGED));
    final Organization organization = person.getOrganizations().iterator().next();
    assertThat(organization.getName(), equalTo(CHANGED));
    assertThat(organization.getCountry(), equalTo(CHANGED));
    assertThat(organization.getCity(), equalTo(CHANGED));
    assertThat(organization.getDescription(), equalTo(CHANGED));

    // date
    assertThat(((de.mpg.imeji.logic.vo.predefinedMetadata.Date) mds.get(4)).getDate(),
        equalTo(dateFormat.format(date)));

    // license
    final License license = (License) mds.get(5);
    assertThat(license.getLicense(), equalTo(CHANGED));
    assertThat(license.getExternalUri().toString(), equalTo(CHANGED));

    // link
    assertThat(((Link) mds.get(6)).getUri().toString(), equalTo(CHANGED));
    assertThat(((Link) mds.get(6)).getLabel(), equalTo(CHANGED));

    // publication
    assertThat(((Publication) mds.get(7)).getExportFormat(), equalTo(CHANGED));
    assertThat(((Publication) mds.get(7)).getUri().toString(), equalTo(CHANGED));
  }



  @Test
  public void test_2_UpdateItem_2_Change_Metadata_Statements_Not_Allowed()
      throws IOException, UnprocessableError, ImejiException {

    // final String CHANGED = "not_allowed_change";
    // final String REP_CHANGED = "$1\"" + CHANGED + "\"";
    //
    // FormDataMultiPart multiPart = new FormDataMultiPart();
    //
    // MetadataProfileTO profileTO = new MetadataProfileTO();
    // TransferObjectFactory.transferMetadataProfile(profile, profileTO);
    //
    // Item item = new Item();
    // ReverseTransferObjectFactory.transferDefaultItem(itemTO, item, profileTO, JenaUtil.testUser,
    // null);
    //
    // for (Metadata md : item.getMetadataSet().getMetadata()) {
    // if (md instanceof ConePerson) {
    // ((ConePerson) md).getPerson().setIdentifier(CHANGED);
    // for (Organization org : ((ConePerson) md).getPerson().getOrganizations()) {
    // org.setIdentifier(CHANGED);
    // }
    // }
    // }
    //
    //
    //
    // // identifiers
    // MetadataSetTO md =
    // itemTO.filterMetadataByTypeURI(MetadataTO.getTypeURI(ConePersonTO.class)).get(0);
    // PersonTO p = ((ConePersonTO) md.getValue()).getPerson();
    //
    // for (IdentifierTO ident : p.getIdentifiers()) {
    // ident.setType(CHANGED);
    // ident.setValue(CHANGED);
    // }
    // for (OrganizationTO org : p.getOrganizations()) {
    // for (IdentifierTO ident : org.getIdentifiers()) {
    // ident.setType(CHANGED);
    // ident.setValue(CHANGED);
    // }
    // }
    //
    // int i = 0;
    // for (IdentifierTO identifier : p.getIdentifiers()) {
    // if (i == 0) {
    // identifier.setValue(CHANGED);
    // identifier.setType(CHANGED);
    // identifier = p.getOrganizations().get(i).getIdentifiers().get(i);
    // identifier.setValue(CHANGED);
    // identifier.setType(CHANGED);
    // }
    // }
    //
    // // labels
    // for (MetadataSetTO mds : itemTO.getMetadata()) {
    // for (LabelTO lb : mds.getLabels()) {
    // lb.setValue(CHANGED);
    // lb.setLanguage(CHANGED);
    // }
    // }
    //
    // multiPart.field("json",
    // updateJSON.replace("___COLLECTION_ID___", collectionId).replace("___ITEM_ID___", itemId)
    // // item properties
    // .replaceAll("(\"fullname\"\\s*:\\s*)\"(.*)\"", REP_CHANGED)
    // .replaceAll("(\"userId\"\\s*:\\s*)\"(.*)\"", REP_CHANGED)
    // .replaceAll("(\"createdDate\"\\s*:\\s*)\"(.*)\"", REP_CHANGED)
    // .replaceAll("(\"modifiedDate\"\\s*:\\s*)\"(.*)\"", REP_CHANGED)
    // .replaceAll("(\"versionDate\"\\s*:\\s*)\"(.*)\"", REP_CHANGED)
    // .replaceAll("(\"status\"\\s*:\\s*)\"(.*)\"", REP_CHANGED)
    // .replaceAll("(\"version\"\\s*:\\s*)(.*)", "$1-1,")
    // .replaceAll("(\"discardComment\"\\s*:\\s*)\"(.*)\"", REP_CHANGED)
    // .replaceAll("(\"visibility\"\\s*:\\s*)\"(.*)\"", REP_CHANGED)
    // .replaceAll("(\"mimetype\"\\s*:\\s*)\"(.*)\"", REP_CHANGED)
    // .replaceAll("(\"checksumMd5\"\\s*:\\s*)\"(.*)\"", REP_CHANGED)
    // .replaceAll("(\"webResolutionUrlUrl\"\\s*:\\s*)\"(.*)\"", REP_CHANGED)
    // .replaceAll("(\"thumbnailUrl\"\\s*:\\s*)\"(.*)\"", REP_CHANGED)
    // .replaceAll("(\"fileUrl\"\\s*:\\s*)\"(.*)\"", REP_CHANGED));
    //
    // // LOGGER.info(multiPart.getField("json").getValue());
    //
    // Response response =
    // target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
    // .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
    // .put(Entity.entity(multiPart, multiPart.getMediaType()));
    // assertEquals(OK.getStatusCode(), response.getStatus());
    //
    // ItemTO updItem = new ItemTO();
    // updItem.setCollectionId(collectionId);
    // updItem = ItemProcess.prepareDefaultItemTOAsItemTO(false, response.readEntity(String.class),
    // updItem, itemId, JenaUtil.testUser);
    // itemTO = updItem;
    //
    // // LOGGER.info(buildJSONFromObject(updItem));
    //
    // // assertThat(updItem.getCreatedBy().getFullname(), not(equalTo(CHANGED)));
    // assertThat(updItem.getCreatedBy().getUserId(), not(equalTo(CHANGED)));
    // assertThat(updItem.getModifiedBy().getFullname(), not(equalTo(CHANGED)));
    // assertThat(updItem.getModifiedBy().getUserId(), not(equalTo(CHANGED)));
    //
    // assertThat(updItem.getCreatedDate(), not(equalTo(CHANGED)));
    // assertThat(updItem.getModifiedDate(), not(equalTo(CHANGED)));
    // assertThat(updItem.getVersionDate(), not(equalTo(CHANGED)));
    //
    // assertThat(updItem.getStatus(), not(equalTo(CHANGED)));
    //
    // assertThat(updItem.getVersion(), not(equalTo(-1)));
    //
    // assertThat(updItem.getDiscardComment(), not(equalTo(CHANGED)));
    // assertThat(updItem.getVisibility(), not(equalTo(CHANGED)));
    // assertThat(updItem.getCollectionId(), not(equalTo(CHANGED)));
    // assertThat(updItem.getMimetype(), not(equalTo(CHANGED)));
    // assertThat(updItem.getChecksumMd5(), not(equalTo(CHANGED)));
    // assertThat(updItem.getWebResolutionUrlUrl().toString(), not(equalTo(CHANGED)));
    // assertThat(updItem.getThumbnailUrl().toString(), not(equalTo(CHANGED)));
    // assertThat(updItem.getFileUrl().toString(), not(equalTo(CHANGED)));
    //
    //
    // List<MetadataSetTO> mdsList = updItem.getMetadata();
    //
    // for (MetadataSetTO mds : mdsList) {
    // // statements
    // assertThat(mds.getStatementUri().toString(), not(equalTo(CHANGED)));
    // assertThat(mds.getTypeUri().toString(), not(equalTo(CHANGED)));
    // // labels
    // for (LabelTO l : mds.getLabels()) {
    // assertThat(l.getValue(), not(equalTo(CHANGED)));
    // assertThat(l.getLanguage(), not(equalTo(CHANGED)));
    // }
    // }
    //
    // // person
    // p = ((ConePersonTO) mdsList.get(2).getValue()).getPerson();
    // assertThat(p.getId(), not(equalTo(CHANGED)));
    // // person identifiers
    // for (IdentifierTO ident : p.getIdentifiers()) {
    // assertThat(ident.getType(), not(equalTo(CHANGED)));
    // assertThat(ident.getValue(), not(equalTo(CHANGED)));
    // }
    // for (OrganizationTO org : p.getOrganizations()) {
    // assertThat(org.getId(), not(equalTo(CHANGED)));
    // for (IdentifierTO ident : org.getIdentifiers()) {
    // assertThat(ident.getType(), not(equalTo(CHANGED)));
    // assertThat(ident.getValue(), not(equalTo(CHANGED)));
    // }
    // }
  }



  @Test
  public void test_3_UpdateItem_1_Change_Metadata_Statements_EmptyValues()
      throws IOException, BadRequestException {

    final String REP_CHANGED = "$1\"\"";
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json",
        updateJSON.replace("___COLLECTION_ID___", collectionId).replace("___ITEM_ID___", itemId)
            .replaceAll("(\"text\"\\s*:\\s*)\"(.+)\"", REP_CHANGED)
            .replaceAll("(\"date\"\\s*:\\s*)\"(.+)\"", REP_CHANGED)
            .replaceAll("(\"license\"\\s*:\\s*)\"(.+)\"", REP_CHANGED)
            .replaceAll("(\"link\"\\s*:\\s*)\"(.+)\"", REP_CHANGED)
            .replaceAll("(\"url\"\\s*:\\s*)\"(.+)\"", REP_CHANGED)
            // /only publication should be filled, not citation or format
            .replaceAll("(\"publication\"\\s*:\\s*)\"(.+)\"", REP_CHANGED));

    // LOGGER.info(multiPart.getField("json").getValue());

    Response response =
        target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
            .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(response.getStatus(), OK.getStatusCode());

    final String json = response.readEntity(String.class);
    // LOGGER.info(json);

    assertThat(json, not(containsString("\"text\"")));
    assertThat(json, not(containsString("\"date\"")));
    assertThat(json, allOf(not(containsString("\"license\"")), not(containsString("\"url\""))));
    assertThat(json, allOf(not(containsString("\"link\"")), not(containsString("\"url\""))));
    assertThat(json, allOf(not(containsString("\"format\"")),
        not(containsString("\"publication\"")), not(containsString("\"citation\""))));
  }


  @Test
  public void test_3_UpdateItem_2_Change_Metadata_Statements_EmptyStatements_SomeSections()
      throws IOException, UnprocessableError, ImejiException {

    FormDataMultiPart multiPart = new FormDataMultiPart();

    multiPart.field("json",
        updateJSON.replace("___COLLECTION_ID___", collectionId).replace("___ITEM_ID___", itemId));

    Response responseUpd =
        target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
            .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(OK.getStatusCode(), responseUpd.getStatus());
    String originalJ = responseUpd.readEntity(String.class);
    // LOGGER.info("ORIGINAL= "+originalJ);

    DefaultItemTO updItem =
        (DefaultItemTO) RestProcessUtils.buildTOFromJSON(originalJ, DefaultItemTO.class);

    multiPart = new FormDataMultiPart();

    multiPart.field("json", originalJ.replaceAll("\"text.*\",", "").replaceAll("\"date.*\",", ""));

    // LOGGER.info("UPDATE"+multiPart.getField("json").getValue());

    Response responseUpd2 =
        target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
            .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(OK.getStatusCode(), responseUpd2.getStatus());

    DefaultItemTO newUpdItem = (DefaultItemTO) RestProcessUtils
        .buildTOFromJSON(responseUpd2.readEntity(String.class), DefaultItemTO.class);

    assertThat(newUpdItem.getMetadata().size(), equalTo(updItem.getMetadata().size() - 2));

  }


  @Test
  public void test_3_UpdateItem_3_Change_Metadata_Statements_EmptyStatements_CompleteSection()
      throws IOException, UnprocessableError, ImejiException {

    FormDataMultiPart multiPart = new FormDataMultiPart();

    multiPart.field("json", updateJSON.replace("___COLLECTION_ID___", collectionId)
        .replace("___ITEM_ID___", itemId).replaceAll("\"metadata\".*", "\"metadata\" : {} }"));

    Response response =
        target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
            .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(OK.getStatusCode(), response.getStatus());
    String newJson = response.readEntity(String.class);

    DefaultItemTO newUpdItem =
        (DefaultItemTO) RestProcessUtils.buildTOFromJSON(newJson, DefaultItemTO.class);

    assertThat(newUpdItem.getMetadata().size(), equalTo(0));

  }


  @Test
  public void test_4_UpdateItem_3_Change_Metadata_Statements_Add_MultipleStatement()
      throws Exception {

    initCollectionWithProfile(getMultipleStatements());
    initItemWithMultipleStatements();

    String ADDED_TITLE = "addedMultipleText";
    FormDataMultiPart multiPart = new FormDataMultiPart();

    itemTO = (DefaultItemTO) RestProcessUtils.buildTOFromJSON(
        getStringFromPath(STATIC_CONTEXT_REST + "/easyUpdateItemBasicMultipleStatements.json"),
        DefaultItemTO.class);

    String jsonAddNewTitle =
        getStringFromPath(STATIC_CONTEXT_REST + "/easyUpdateItemBasicMultipleStatements.json")
            .replace("___COLLECTION_ID___", collectionId).replace("___ITEM_ID___", itemId)
            .replaceAll("\"text\".*],",
                "\"text\": [\"value1\", \"value2\",\"" + ADDED_TITLE + "\"],");

    multiPart.field("json", jsonAddNewTitle);
    // LOGGER.info("MODIEIF "+jsonAddNewTitle );

    Response response =
        target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
            .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(OK.getStatusCode(), response.getStatus());

    DefaultItemTO updItem = (DefaultItemTO) RestProcessUtils
        .buildTOFromJSON(response.readEntity(String.class), DefaultItemTO.class);

    Item item = new Item();
    ReverseTransferObjectFactory.transferDefaultItem(updItem, item, profile, JenaUtil.testUser,
        TRANSFER_MODE.UPDATE);
    Item originalItem = new Item();
    ReverseTransferObjectFactory.transferDefaultItem(itemTO, originalItem, profile,
        JenaUtil.testUser, TRANSFER_MODE.UPDATE);
    assertThat(item.getMetadataSet().getMetadata(),
        hasSize(originalItem.getMetadataSet().getMetadata().size() + 1));
    int i = 0;
    String updatedTextString = "";
    // Find the statementId for the text Metadata
    String textStatementId = null;
    for (Statement st : profile.getStatements()) {
      if ("text".equals(st.getLabel())) {
        textStatementId = st.getId().toString();
      }
    }
    for (Metadata md : item.getMetadataSet().getMetadata()) {
      if (md.getStatement().toString().equals(textStatementId) && md instanceof Text) {
        i++;
        updatedTextString += ((Text) md).getText();
      }
    }
    assertThat(i, equalTo(3));
    assertThat(updatedTextString, containsString(ADDED_TITLE));


  }


  @Test
  public void test_4_UpdateItem_3_Change_Metadata_Statements_Add_NonMultipleStatement()
      throws Exception {

    initCollectionWithProfile(getMultipleStatements());
    initItemWithMultipleStatements();

    FormDataMultiPart multiPart = new FormDataMultiPart();
    String jsonAddNewTitle =
        getStringFromPath(STATIC_CONTEXT_REST + "/easyUpdateItemBasicMultipleStatements.json")
            .replace("___COLLECTION_ID___", collectionId).replace("___ITEM_ID___", itemId)
            .replaceAll("\"date\".*\",", "\"date\": [\"2015-12-01\", \"2015-12-02\"],");

    multiPart.field("json", jsonAddNewTitle);

    Response response =
        target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
            .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

  }

  /**
   * Initialize an Item with a Metadata of each type
   * 
   * @throws Exception
   */
  protected static void initItemWithFullMetadata() throws Exception {
    DefaultItemService service = new DefaultItemService();
    String json = updateJSON.replace("___COLLECTION_ID___", collectionId);
    DefaultItemWithFileTO defaultItemWithFileTO =
        (DefaultItemWithFileTO) RestProcessUtils.buildTOFromJSON(json, DefaultItemWithFileTO.class);
    defaultItemWithFileTO.setFile(new File("src/test/resources/storage/test.png"));
    itemTO = service.create(defaultItemWithFileTO, JenaUtil.testUser);
    itemId = itemTO.getId();
  }

  /**
   * Initialize an Item with multiple metadata
   * 
   * @throws Exception
   */
  private static void initItemWithMultipleStatements() throws Exception {
    DefaultItemService service = new DefaultItemService();
    String json =
        getStringFromPath(STATIC_CONTEXT_REST + "/easyUpdateItemBasicMultipleStatements.json")
            .replace("___COLLECTION_ID___", collectionId);
    DefaultItemWithFileTO defaultItemWithFileTO =
        (DefaultItemWithFileTO) RestProcessUtils.buildTOFromJSON(json, DefaultItemWithFileTO.class);
    defaultItemWithFileTO.setFile(new File("src/test/resources/storage/test.png"));
    itemTO = service.create(defaultItemWithFileTO, JenaUtil.testUser);
    itemId = itemTO.getId();
  }


}
