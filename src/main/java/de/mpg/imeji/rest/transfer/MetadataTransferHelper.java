package de.mpg.imeji.rest.transfer;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.MetadataSet;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.predefinedMetadata.ConePerson;
import de.mpg.imeji.logic.vo.predefinedMetadata.Geolocation;
import de.mpg.imeji.logic.vo.predefinedMetadata.License;
import de.mpg.imeji.logic.vo.predefinedMetadata.Link;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;
import de.mpg.imeji.logic.vo.predefinedMetadata.Publication;
import de.mpg.imeji.logic.vo.predefinedMetadata.Text;
import de.mpg.imeji.logic.vo.util.ObjectHelper;
import de.mpg.imeji.rest.helper.CommonUtils;
import de.mpg.imeji.rest.process.RestProcessUtils;
import de.mpg.imeji.rest.to.MetadataProfileTO;
import de.mpg.imeji.rest.to.MetadataSetTO;
import de.mpg.imeji.rest.to.OrganizationTO;
import de.mpg.imeji.rest.to.PersonTO;
import de.mpg.imeji.rest.to.StatementTO;
import de.mpg.imeji.rest.to.SuperMetadataBeanTO;
import de.mpg.imeji.rest.to.SuperMetadataTreeTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultOrganizationTO;
import de.mpg.imeji.rest.to.defaultItemTO.predefinedEasyMetadataTO.DefaultConePersonTO;
import de.mpg.imeji.rest.to.defaultItemTO.predefinedEasyMetadataTO.DefaultGeolocationTO;
import de.mpg.imeji.rest.to.defaultItemTO.predefinedEasyMetadataTO.DefaultLicenseTO;
import de.mpg.imeji.rest.to.defaultItemTO.predefinedEasyMetadataTO.DefaultLinkTO;
import de.mpg.imeji.rest.to.defaultItemTO.predefinedEasyMetadataTO.DefaultPublicationTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.ConePersonTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.DateTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.GeolocationTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.LicenseTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.LinkTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.MetadataTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.NumberTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.PublicationTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.TextTO;

/**
 * Utility class for transfer of {@link MetadataTO} to or from {@link Metadata}
 *
 * @author bastiens
 *
 */
public class MetadataTransferHelper {

  private static JsonFactory factory = new JsonFactory();
  private static ObjectMapper mapper = new ObjectMapper(factory);

  public static void validateParentStack(StatementTO statement, String label, String parentLabel,
      MetadataProfileTO profileTO) throws UnprocessableError {
    if (statement.getParentStatementId() != null && parentLabel.equals("")
        || (statement.getParentStatementId() != null
            && !parentLabel.equals(ProfileTransferHelper
                .getParentStatementLabel(statement.getParentStatementId(), profileTO))
        && !parentLabel.equals(label))) {
      String localParentsLabelStack = ProfileTransferHelper
          .getParentStatementLabels(statement.getParentStatementId(), profileTO, "");
      throw new UnprocessableError("Metadata with label \"" + label
          + "\", must be enclosed within metadata parents: " + localParentsLabelStack
          + ". Check your structure, and provide necessary values for all parents "
          + localParentsLabelStack
          + " in the metadata to enable proper assignment of values! Please check the template item!");
    }

    if (statement.getParentStatementId() == null && !parentLabel.equals("")
        && !label.equals(parentLabel)) {
      throw new UnprocessableError("Metadata with label \"" + label
          + "\", must not be within any metadata parents! Change your input or check the template item!");
    }
  }

  public static void validateInnerNodeParentStack(String currentLabel, String label,
      StatementTO innerStatement, MetadataProfileTO profileTO, boolean hasInnerParent)
          throws UnprocessableError {
    String parentStatementRealLabel = ProfileTransferHelper
        .getParentStatementLabel(innerStatement.getParentStatementId(), profileTO);
    if (parentStatementRealLabel != "" && !currentLabel.equals(label)) {
      if (!(label.equals(parentStatementRealLabel) && hasInnerParent)) {
        String parentsLabelStack = ProfileTransferHelper
            .getParentStatementLabels(innerStatement.getParentStatementId(), profileTO, "");
        throw new UnprocessableError("Metadata with label \"" + currentLabel
            + "\", must be enclosed within metadata parents: " + parentsLabelStack
            + ". Check your structure, and provide necessary values for all parents "
            + parentsLabelStack
            + " in the metadata to enable proper assignment of values! Please check the template item! ");
      }
    }
  }

  public static void validateInnerNodeParent(boolean isInnerParent, JsonNode nodeChild,
      String currentLabel, String maxOccurs) throws UnprocessableError {

    boolean emptyArrayValueCheck =
        nodeChild.isArray() && (nodeChild.get(0).isObject() && nodeChild.get(0).size() == 0
            || nodeChild.get(0).isValueNode()
                && StringUtils.trim(nodeChild.get(0).asText()).length() == 0);

    boolean emptySingleValueCheck =
        !nodeChild.isArray() && (nodeChild.isObject() && nodeChild.size() == 0
            || nodeChild.isValueNode() && StringUtils.trim(nodeChild.asText()).length() == 0);

    // boolean tooMuchArrayValueCheck = nodeChild.isArray() && nodeChild.size()>1 &&
    // !nodeChild.get(0).isObject() ;
    boolean tooMuchArrayValueCheck = nodeChild.isArray() && nodeChild.size() > 1;

    if (isInnerParent && (emptyArrayValueCheck || emptySingleValueCheck)) {
      // validate if node is empty parent is null
      throw new UnprocessableError(
          "Metadata with label \"" + currentLabel + "\" is a parent for other metadata! "
              + " Please provide a non-empty, non-null value for \"" + currentLabel
              + "\" to enable proper assignment of values at " + nodeChild
              + " or check the template item! ");
    }

    if (isInnerParent && tooMuchArrayValueCheck
        && (!nodeChild.get(0).isObject() && maxOccurs.equals("unbounded"))) {
      throw new UnprocessableError("Metadata with label \"" + currentLabel
          + "\" is a parent for other metadata! "
          + " You provided multiple values in the inner parent node: " + nodeChild
          + ". To support multiple values, you must explicitly add new \"" + currentLabel
          + "\" object which encloses appropriate children metadata! Please check the template item!");

    }
  }

  public static List<MetadataSetTO> parseMetadata(JsonNode json, StatementTO statement,
      boolean isParent, String parentLabel, MetadataProfileTO profileTO) throws UnprocessableError {
    List<MetadataSetTO> l = new ArrayList<MetadataSetTO>();
    String label = statement.getLabels().get(0).getValue();

    validateParentStack(statement, label, parentLabel, profileTO);

    ArrayNode tempNode = mapper.createArrayNode();

    // Small fix for original implementation, actually is a new feature
    // Even if users provide List to POST/PUT for single-valued elements, this will be validated and
    // updated accordingly to the single/multiple values
    //
    if (json.getNodeType() == JsonNodeType.ARRAY) {
      if (!statement.getMaxOccurs().equals("unbounded") && json.size() > 1) {
        throw new UnprocessableError("Metadata \"" + label + "\" can only have one value.");
      }
      tempNode = (ArrayNode) json;
    } else {
      tempNode.add(json);
    }

    for (Iterator<JsonNode> iterator = tempNode.elements(); iterator.hasNext();) {
      JsonNode node = iterator.next();

      if (!isParent) {
        l.add(parseMetadataTO(node, statement));
      } else {
        // Iterate over the Inner Node
        boolean hasDirectParentNode = false;
        boolean hasInnerParent = false;
        if (!node.has(label)) {
          throw new UnprocessableError("Metadata with label \"" + label
              + "\" is a parent for other metadata! " + " Please provide a value for \"" + label
              + "\" to enable proper assignment of values at " + node
              + " or check the template item!");
        }

        for (Iterator<String> iteratorChild = node.fieldNames(); iteratorChild.hasNext();) {


          String currentLabel = iteratorChild.next();

          JsonNode nodeChild = node.get(currentLabel);
          StatementTO innerStatement = ProfileTransferHelper
              .validateStatementByLabelAndParent(currentLabel, profileTO, statement.getId());

          boolean isInnerParent =
              ProfileTransferHelper.hasChildStatement(innerStatement.getId(), profileTO);
          if (isInnerParent) {
            hasInnerParent = true;
          }


          validateInnerNodeParent(isInnerParent, nodeChild, currentLabel,
              innerStatement.getMaxOccurs());
          // Force creation of parent node which has same label as the other one
          if (currentLabel.equals(label)) {
            isInnerParent = false;
            hasDirectParentNode = true;
          }

          validateInnerNodeParentStack(currentLabel, label, innerStatement, profileTO,
              hasInnerParent);

          // if only child is provided, without its parent node, throw Exception
          if (!isInnerParent && !hasDirectParentNode) {
            throw new UnprocessableError("Metadata with label \"" + currentLabel
                + "\" is a child of a metadata with label \"" + label
                + "\" ! Please provide a value for \"" + label
                + "\" to enable proper assignment of values! Please check the template item!");
          }

          List<MetadataSetTO> arrList = parseMetadata(nodeChild, innerStatement, isInnerParent,
              isInnerParent ? innerStatement.getLabels().get(0).getValue() : label, profileTO);

          l.addAll(arrList);
        }
      }
    } // for


    return l;
  }



  /*
   * ORIGINAL EASY METADATA
   *
   * public static List<MetadataSetTO> parseMetadata(JsonNode json, StatementTO statement) throws
   * BadRequestException { List<MetadataSetTO> l = new ArrayList<MetadataSetTO>(); String label =
   * statement.getLabels().get(0).getValue(); if (json.getNodeType() == JsonNodeType.ARRAY) { if
   * (!statement.getMaxOccurs().equals("unbounded")) { throw new BadRequestException("Metadata \"" +
   * label + "\" can only have one value"); } for (Iterator<JsonNode> iterator = json.elements();
   * iterator.hasNext();) { JsonNode node = (JsonNode) iterator.next(); l.add(parseMetadataTO(node,
   * statement)); } } else { l.add(parseMetadataTO(json, statement)); } return l; }
   *
   *
   */
  /**
   * Serialize a list of {@link Metadata} to one {@link JsonNode} by using
   * {@link SuperMetadataTreeTO}
   *
   * @param metadataSet
   * @param profile
   * @return
   */
  public static Map<String, JsonNode> serializeMetadataSet(Collection<Metadata> metadataSet,
      MetadataProfile profile) {

    SuperMetadataTreeTO metadataTree = new SuperMetadataTreeTO(metadataSet, profile);
    return metadataTree.getOMapWithJsonNodes();
  }


  /**
   * Generates template values for {@link Metadata} of a given {@link MetadataProfile} by using
   * {@link SuperMetadataTreeTO}
   *
   * @param metadataSet
   * @param profile
   * @return
   */
  public static Collection<Metadata> getTemplateMetadataSet(MetadataProfile profile) {
    LinkedList<Metadata> defaultMetadata = SuperMetadataTreeTO.generateDefaultValues(profile);
    return defaultMetadata;
  }


  /*
   * ORIGINAL EASY METADATA
   *
   * public static Map<String, JsonNode> serializeMetadataSet(Collection<Metadata> metadataSet,
   * MetadataProfile profile) { Map<String, JsonNode> json = new HashMap<String, JsonNode>(); for
   * (Statement statement : profile.getStatements()) { List<Metadata> l =
   * filterMetadataByStatement(metadataSet, statement); if (l.size() == 1) {
   * json.put(statement.getLabel(), serializeMetadata(l.get(0), statement)); } else if (l.size() >
   * 1) { List<JsonNode> nodes = new ArrayList<JsonNode>(); for (Metadata md : l) {
   * nodes.add(serializeMetadata(md, statement)); } json.put(statement.getLabel(),
   * RestProcessUtils.buildJsonNode(nodes)); } } return json; }
   *
   *
   *
   *
   */



  /**
   * Serialize a single {@link Metadata} value as a simple {@link JsonNode} (value or object node).
   * It is used in {@link SuperMetadataTreeTO} and {@link SuperMetadataBeanTO}} for generation of
   * JSON
   *
   * @param metadata
   * @param statement
   * @return
   */
  /*
   * ORIGINAL EASY METADATA method is "private" i.e.
   *
   * private static JsonNode serializeMetadata(Metadata metadata, Statement statement) {
   */
  public static JsonNode serializeMetadata(Metadata metadata, Statement statement) {
    switch (Metadata.Types.valueOfUri(statement.getType().toString())) {
      case TEXT:
        return RestProcessUtils.buildJsonNode(((Text) metadata).getText());
      case NUMBER:
        return RestProcessUtils.buildJsonNode(
            ((de.mpg.imeji.logic.vo.predefinedMetadata.Number) metadata).getNumber());
      case CONE_PERSON:
        DefaultConePersonTO pTO = new DefaultConePersonTO();
        TransferObjectFactory.transferDefaultPerson(((ConePerson) metadata).getPerson(), pTO);
        return RestProcessUtils.buildJsonNode(pTO);
      case DATE:
        return RestProcessUtils
            .buildJsonNode(((de.mpg.imeji.logic.vo.predefinedMetadata.Date) metadata).getDate());
      case GEOLOCATION:
        Geolocation mdGeo = (Geolocation) metadata;
        DefaultGeolocationTO dgto = new DefaultGeolocationTO();
        dgto.setName(mdGeo.getName());
        dgto.setLongitude(mdGeo.getLongitude());
        dgto.setLatitude(mdGeo.getLatitude());
        return RestProcessUtils.buildJsonNode(dgto);
      case LICENSE:
        License mdLicense = (License) metadata;
        DefaultLicenseTO dlto = new DefaultLicenseTO();
        dlto.setLicense(mdLicense.getLicense());
        URI externalUri = mdLicense.getExternalUri();
        dlto.setUrl(externalUri != null ? externalUri.toString() : "");
        return RestProcessUtils.buildJsonNode(dlto);
      case LINK:
        Link mdLink = (Link) metadata;
        DefaultLinkTO dllto = new DefaultLinkTO();
        dllto.setLink(mdLink.getLabel());
        dllto.setUrl(mdLink.getUri() != null ? mdLink.getUri().toString() : "");
        return RestProcessUtils.buildJsonNode(dllto);
      case PUBLICATION:
        Publication mdP = (Publication) metadata;
        DefaultPublicationTO dpto = new DefaultPublicationTO();
        dpto.setPublication(mdP.getUri() != null ? mdP.getUri().toString() : "");
        dpto.setFormat(mdP.getExportFormat());
        dpto.setCitation(mdP.getCitation());
        return RestProcessUtils.buildJsonNode(dpto);
    }
    return null;
  }


  /**
   * Parse simple {@link JsonNode} (i.e. a value or object node) into a {@link MetadataSetTO}.
   *
   * @param json
   * @param statement
   * @return
   * @throws UnprocessableError
   */
  private static MetadataSetTO parseMetadataTO(JsonNode json, StatementTO statement)
      throws UnprocessableError {
    String type = statement.getType().toString();
    String label = statement.getLabels().get(0).getValue();
    MetadataSetTO metadata = new MetadataSetTO(statement);
    switch (Metadata.Types.valueOfUri(type)) {
      case TEXT:
        String textValue = json.textValue();
        if (textValue == null) {
          throw new UnprocessableError("Wrong value <" + json.toString() + "> in metadata of type "
              + type + " with label " + label);
        } else {
          TextTO newT = new TextTO();
          newT.setText(textValue);
          metadata.setValue(newT);
        }
        break;
      case NUMBER:
        NumberTO newNT = new NumberTO();
        // Fix bug #362
        if (!json.isNumber()) {
          throw new UnprocessableError("Wrong value <" + json.toString() + "> in metadata of type "
              + type + " with label " + label);
        }
        newNT.setNumber(json.asDouble());
        metadata.setValue(newNT);
        break;
      case CONE_PERSON:
        DefaultConePersonTO easyCPTO = null;
        try {
          easyCPTO = mapper.readValue(json.toString(), new TypeReference<DefaultConePersonTO>() {});
        } catch (Exception e) {
          throw new UnprocessableError("Error transfering " + label, e);
        }
        ConePersonTO newCone =
            metadata.getValue() != null ? (ConePersonTO) metadata.getValue() : new ConePersonTO();
        PersonTO newP = metadata.getValue() != null ? newCone.getPerson() : new PersonTO();
        newP.setFamilyName(easyCPTO.getFamilyName());
        newP.setGivenName(easyCPTO.getGivenName());
        for (DefaultOrganizationTO dTO : easyCPTO.getOrganizations()) {
          newP.getOrganizations().add(new OrganizationTO(dTO));
        }
        newCone.setPerson(newP);
        metadata.setValue(newCone);
        break;
      case DATE:
        DateTO newDT = new DateTO();
        newDT.setDate(json.textValue());
        metadata.setValue(newDT);
        break;
      case GEOLOCATION:
        DefaultGeolocationTO easyGeoTO = null;
        try {
          easyGeoTO =
              mapper.readValue(json.toString(), new TypeReference<DefaultGeolocationTO>() {});
        } catch (Exception e) {
          throw new UnprocessableError("Error Transferring" + label, e);
        }
        GeolocationTO newGT = new GeolocationTO();
        newGT.setName(easyGeoTO.getName());
        newGT.setLatitude(easyGeoTO.getLatitude());
        newGT.setLongitude(easyGeoTO.getLongitude());
        metadata.setValue(newGT);
        break;
      case LICENSE:
        DefaultLicenseTO easyLTO = null;
        try {
          easyLTO = mapper.readValue(json.toString(), new TypeReference<DefaultLicenseTO>() {});
        } catch (Exception e) {
          throw new UnprocessableError("Error Transferring" + label, e);
        }
        LicenseTO newLicense = new LicenseTO();
        newLicense.setLicense(easyLTO.getLicense());
        newLicense.setUrl(easyLTO.getUrl());
        metadata.setValue(newLicense);
        break;
      case LINK:
        DefaultLinkTO easyLinkTO = null;
        try {
          easyLinkTO = mapper.readValue(json.toString(), new TypeReference<DefaultLinkTO>() {});
        } catch (Exception e) {
          throw new UnprocessableError("Error Transferring" + label, e);
        }
        LinkTO newLink = new LinkTO();
        newLink.setLink(easyLinkTO.getLink());
        newLink.setUrl(easyLinkTO.getUrl());
        metadata.setValue(newLink);
        break;
      case PUBLICATION:
        DefaultPublicationTO easyPTO = null;
        try {
          easyPTO = mapper.readValue(json.toString(), new TypeReference<DefaultPublicationTO>() {});
        } catch (Exception e) {
          throw new UnprocessableError("Error Transferring" + label, e);
        }
        PublicationTO newPub = new PublicationTO();
        newPub.setCitation(easyPTO.getCitation());
        newPub.setFormat(easyPTO.getFormat());
        newPub.setPublication(easyPTO.getPublication());
        metadata.setValue(newPub);
        break;
    }
    return metadata;
  }

  public static Object readItemTemplateForProfile(String collectionId, String profileId, User u)
      throws ImejiException {

    URI profileURI =
        profileId != null ? ObjectHelper.getURI(MetadataProfile.class, profileId) : null;
    if (profileURI == null && collectionId != null) {
      CollectionController cc = new CollectionController();
      CollectionImeji col =
          cc.retrieve(ObjectHelper.getURI(CollectionImeji.class, collectionId), u);
      profileURI = col.getProfile();
    }

    Item vo = new Item();
    vo.setId(URI.create("newid"));
    if (collectionId != null) {
      vo.setCollection(URI.create(collectionId));
    } else {
      vo.setCollection(URI.create("provide-your-collection-id-here"));
    }
    vo.setFilename(
        "<change-the-file-name-here-or-provide-separate-field-for-fetch-or-reference-url-see-API-Documentation>");
    MetadataSet mds = new MetadataSet();
    ProfileController pc = new ProfileController();
    MetadataProfile profile =
        profileURI != null ? pc.retrieve(profileURI, u) : new MetadataProfile();

    mds.getMetadata().addAll(MetadataTransferHelper.getTemplateMetadataSet(profile));
    List<MetadataSet> metadataSets = new ArrayList<MetadataSet>();
    metadataSets.add(mds);
    vo.setMetadataSets(metadataSets);
    DefaultItemTO to = new DefaultItemTO();
    to.setId(vo.getId().toString());
    to.setCollectionId(CommonUtils.extractIDFromURI(vo.getCollection()));
    to.setFilename(vo.getFilename());
    TransferObjectFactory.transferItemMetadataDefault(profile, vo.getMetadataSet().getMetadata(),
        to);

    return to;
  }

}
