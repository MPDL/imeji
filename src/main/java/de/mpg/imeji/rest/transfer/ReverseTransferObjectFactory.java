package de.mpg.imeji.rest.transfer;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.ContainerAdditionalInfo;
import de.mpg.imeji.logic.vo.ContainerMetadata;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.predefinedMetadata.ConePerson;
import de.mpg.imeji.logic.vo.predefinedMetadata.Geolocation;
import de.mpg.imeji.logic.vo.predefinedMetadata.License;
import de.mpg.imeji.logic.vo.predefinedMetadata.Link;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;
import de.mpg.imeji.logic.vo.predefinedMetadata.Number;
import de.mpg.imeji.logic.vo.predefinedMetadata.Publication;
import de.mpg.imeji.logic.vo.predefinedMetadata.Text;
import de.mpg.imeji.rest.to.AlbumTO;
import de.mpg.imeji.rest.to.CollectionTO;
import de.mpg.imeji.rest.to.ContainerAdditionalInformationTO;
import de.mpg.imeji.rest.to.ContainerTO;
import de.mpg.imeji.rest.to.IdentifierTO;
import de.mpg.imeji.rest.to.ItemTO;
import de.mpg.imeji.rest.to.LiteralConstraintTO;
import de.mpg.imeji.rest.to.MetadataProfileTO;
import de.mpg.imeji.rest.to.MetadataSetTO;
import de.mpg.imeji.rest.to.OrganizationTO;
import de.mpg.imeji.rest.to.PersonTO;
import de.mpg.imeji.rest.to.StatementTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.ConePersonTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.DateTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.GeolocationTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.LicenseTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.LinkTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.NumberTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.PublicationTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.TextTO;

public class ReverseTransferObjectFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReverseTransferObjectFactory.class);

  public enum TRANSFER_MODE {
    CREATE, UPDATE
  }

  /**
   * Transfer an {@link CollectionTO} to a {@link CollectionImeji}
   *
   * @param to
   * @param vo
   * @param mode
   * @param u
   */
  public static void transferCollection(CollectionTO to, CollectionImeji vo, TRANSFER_MODE mode,
      User u) {
    vo.setMetadata(transferContainerMetatadata(to, mode, u));
  }

  /**
   * Transfer an {@link AlbumTO} to an {@link Album}
   *
   * @param to
   * @param vo
   * @param mode
   * @param u
   */
  public static void transferAlbum(AlbumTO to, Album vo, TRANSFER_MODE mode, User u) {
    vo.setMetadata(transferContainerMetatadata(to, mode, u));
  }

  /**
   * Transfer the ContainerTO into a ContainerMedatata
   *
   * @param to
   * @param mode
   * @param u
   * @return
   */
  private static ContainerMetadata transferContainerMetatadata(ContainerTO to, TRANSFER_MODE mode,
      User u) {
    ContainerMetadata metadata = new ContainerMetadata();
    metadata.setTitle(to.getTitle());
    metadata.setDescription(to.getDescription());
    metadata.setAdditionalInformations(transferAdditionalInfos(to.getAdditionalInfos()));
    // set contributors
    transferCollectionContributors(to.getContributors(), metadata, u, mode);
    return metadata;
  }

  /**
   * Transfer the list of ContainerAdditionalInformationTO to List of ContainerAdditionalInfo
   *
   * @param infosTO
   * @return
   */
  private static List<ContainerAdditionalInfo> transferAdditionalInfos(
      List<ContainerAdditionalInformationTO> infosTO) {
    List<ContainerAdditionalInfo> infos = new ArrayList<>();
    for (ContainerAdditionalInformationTO infoTO : infosTO) {
      infos.add(new ContainerAdditionalInfo(infoTO.getLabel(), infoTO.getText(), infoTO.getUrl()));
    }
    return infos;
  }



  /**
   * Transfer a {@link DefaultItemTO} to an {@link Item}
   *
   * @param to
   * @param vo
   * @param u
   * @param mode
   * @throws ImejiException
   * @throws JsonMappingException
   * @throws JsonParseException
   */
  public static void transferDefaultItem(DefaultItemTO to, Item vo, MetadataProfile profile, User u,
      TRANSFER_MODE mode) throws ImejiException {
    if (mode == TRANSFER_MODE.CREATE) {

      if (!isNullOrEmpty(to.getCollectionId())) {
        vo.setCollection(ObjectHelper.getURI(CollectionImeji.class, to.getCollectionId()));
      }
    }
    if (!isNullOrEmpty(to.getFilename())) {
      vo.setFilename(to.getFilename());
    }
    vo.getLicenses().addAll(transferLicenses(to.getLicenses()));
    transferDefaultMetadata(to, vo, profile, u, mode);
  }

  /**
   * Transfer a LicenseVO to a LicenseTO
   * 
   * @param licenseTOs
   * @return
   */
  private static List<de.mpg.imeji.logic.vo.License> transferLicenses(
      List<de.mpg.imeji.rest.to.LicenseTO> licenseTOs) {
    List<de.mpg.imeji.logic.vo.License> licenses = new ArrayList<>();
    for (de.mpg.imeji.rest.to.LicenseTO licTO : licenseTOs) {
      de.mpg.imeji.logic.vo.License lic = new de.mpg.imeji.logic.vo.License();
      lic.setLabel(
          StringHelper.isNullOrEmptyTrim(licTO.getLabel()) ? licTO.getName() : licTO.getLabel());
      lic.setName(licTO.getName());
      lic.setUrl(licTO.getUrl());
      licenses.add(lic);
    }
    return licenses;
  }


  /**
   * Transfer Metadata of an {@link ItemTO} to an {@link Item}
   *
   * @param to
   * @param vo
   * @param mp
   * @param u
   * @param mode
   * @throws ImejiException
   */
  public static void transferItemMetadata(ItemTO to, Item vo, MetadataProfile mp, User u,
      TRANSFER_MODE mode) throws ImejiException {
    List<Metadata> voMDs = (List<Metadata>) vo.getMetadataSet().getMetadata();
    voMDs.clear();
    if (mp != null) {
      validateMetadata(to, mp);
    }
    int i = 0;
    for (MetadataSetTO mds : to.getMetadata()) {
      switch (mds.getTypeUri().toString()) {
        case "http://imeji.org/terms/metadata#text":
          TextTO textTO = (TextTO) mds.getValue();
          if (!isNullOrEmpty(textTO.getText())) {
            Text mdVO = new Text();
            mdVO.setStatement(mds.getStatementUri());
            mdVO.setText(textTO.getText());
            mdVO.setPos(i);
            i++;
            voMDs.add(mdVO);
          }
          break;
        case "http://imeji.org/terms/metadata#geolocation":
          GeolocationTO geoTO = (GeolocationTO) mds.getValue();
          if (geoTO != null) {
            Geolocation mdVO = new Geolocation();
            mdVO.setStatement(mds.getStatementUri());
            mdVO.setName(geoTO.getName());
            mdVO.setLatitude(geoTO.getLatitude());
            mdVO.setLongitude(geoTO.getLongitude());
            mdVO.setPos(i);
            i++;
            voMDs.add(mdVO);
          }
          break;
        case "http://imeji.org/terms/metadata#number":
          NumberTO numberTO = (NumberTO) mds.getValue();
          if (numberTO != null) {
            Number mdVO = new Number();
            mdVO.setStatement(mds.getStatementUri());
            mdVO.setNumber(numberTO.getNumber());
            mdVO.setPos(i);
            i++;
            voMDs.add(mdVO);
          }
          break;
        case "http://imeji.org/terms/metadata#conePerson":
          ConePersonTO personTO = (ConePersonTO) mds.getValue();
          if (personTO != null) {
            ConePerson mdVO = new ConePerson();
            mdVO.setStatement(mds.getStatementUri());
            mdVO.setPerson(new Person());
            transferPerson(personTO.getPerson(), mdVO.getPerson(), mode);
            mdVO.setPos(i);
            i++;
            voMDs.add(mdVO);
          }
          break;
        case "http://imeji.org/terms/metadata#date":
          DateTO dateTO = (DateTO) mds.getValue();
          if (!isNullOrEmpty(dateTO.getDate())) {
            de.mpg.imeji.logic.vo.predefinedMetadata.Date mdVO =
                new de.mpg.imeji.logic.vo.predefinedMetadata.Date();
            mdVO.setStatement(mds.getStatementUri());
            mdVO.setDate(dateTO.getDate());
            mdVO.setPos(i);
            i++;
            voMDs.add(mdVO);
          }
          break;
        case "http://imeji.org/terms/metadata#license":
          LicenseTO licenseTO = (LicenseTO) mds.getValue();
          final String lic = licenseTO.getLicense();
          final String url = licenseTO.getUrl();
          if (!isNullOrEmpty(lic) || !isNullOrEmpty(url)) {
            License mdVO = new License();
            mdVO.setStatement(mds.getStatementUri());
            // set license to uri if empty
            mdVO.setLicense(isNullOrEmpty(lic) ? url : lic);
            if (!isNullOrEmpty(url)) {
              mdVO.setExternalUri(URI.create(url));
            }
            mdVO.setPos(i);
            i++;
            voMDs.add(mdVO);
          }
          break;
        case "http://imeji.org/terms/metadata#publication":
          PublicationTO pubTO = (PublicationTO) mds.getValue();
          if (!isNullOrEmpty(pubTO.getPublication())) {
            Publication mdVO = new Publication();
            mdVO.setStatement(mds.getStatementUri());
            mdVO.setUri(URI.create(pubTO.getPublication()));
            mdVO.setExportFormat(pubTO.getFormat());
            mdVO.setCitation(pubTO.getCitation());
            mdVO.setPos(i);
            i++;
            voMDs.add(mdVO);
          }
          break;
        case "http://imeji.org/terms/metadata#link":
          LinkTO linkTO = (LinkTO) mds.getValue();
          if (!isNullOrEmpty(linkTO.getUrl())) {
            Link mdVO = new Link();
            mdVO.setStatement(mds.getStatementUri());
            mdVO.setLabel(linkTO.getLink());
            mdVO.setUri(URI.create(linkTO.getUrl()));
            mdVO.setPos(i);
            i++;
            voMDs.add(mdVO);
          }
          break;
      }
    }
  }

  /**
   * Transfer a {@link MetadataProfileTO} into a {@link MetadataProfile}
   *
   * @param to
   * @param vo
   * @param mode
   */
  public static void transferMetadataProfile(MetadataProfileTO to, MetadataProfile vo,
      TRANSFER_MODE mode) {
    if (mode == TRANSFER_MODE.CREATE) {
      vo.setTitle(to.getTitle());
      vo.setDescription(to.getDescription());
      vo.setDefault(to.getDefault());
      vo.getStatements().clear();

      Map<String, String> statementsIdMap = new HashMap<String, String>();
      boolean hasParents = false;

      for (StatementTO stTO : to.getStatements()) {
        Statement stVO = new Statement();
        // Put Old Statement (as Key) and new Statement(as Value) in a Map

        statementsIdMap.put(stTO.getId(), stVO.getId().toString());
        stVO.setType(stTO.getType());
        stVO.setLabels(stTO.getLabels());
        stVO.setVocabulary(stTO.getVocabulary());

        for (LiteralConstraintTO lc : stTO.getLiteralConstraints()) {
          stVO.getLiteralConstraints().add(lc.getValue());
        }
        stVO.setMinOccurs(stTO.getMinOccurs());
        stVO.setMaxOccurs(stTO.getMaxOccurs());
        if (!isNullOrEmpty(stTO.getParentStatementId())) {
          stVO.setParent(URI.create(stTO.getParentStatementId()));
          hasParents = true;
        }
        vo.getStatements().add(stVO);
      }

      if (hasParents) {
        for (Statement s : vo.getStatements()) {
          // Set the new parent statement ID, taken from the Map
          if ((s.getParent() != null)) {
            vo.getStatement(s.getId().toString())
                .setParent(URI.create(statementsIdMap.get(s.getParent().toString())));
          }
        }
      }
    }

  }

  /**
   * Check all item metadata statement/types: they should be presented in the MetadataProfile
   * statements
   *
   * @param to
   * @param mp
   * @throws de.mpg.imeji.exceptions.BadRequestException
   */
  private static void validateMetadata(ItemTO to, MetadataProfile mp) throws BadRequestException {
    for (MetadataSetTO md : to.getMetadata()) {
      try {
        lookUpStatement(mp.getStatements(), md.getTypeUri(), md.getStatementUri());
      } catch (NoSuchElementException e) {
        throw new BadRequestException("Cannot find { typeUri: " + md.getTypeUri()
            + " , statementUri: " + md.getStatementUri() + "} in profile: " + mp.getId(), e);
      }
    }
  }

  /**
   * Check that a statement exists
   *
   * @param statements
   * @param type
   * @param statementUri
   * @return
   */
  private static Statement lookUpStatement(Collection<Statement> statements, final URI type,
      final URI statementUri) {
    return Iterables.find(statements, new Predicate<Statement>() {
      @Override
      public boolean apply(Statement st) {
        return st.getType().equals(type) && st.getId().equals(statementUri);
      }
    });
  }



  /**
   * Transfer a {@link PersonTO} into a {@link Person}
   *
   * @param pto
   * @param p
   * @param mode
   */
  public static void transferPerson(PersonTO pto, Person p, TRANSFER_MODE mode) {
    if (mode == TRANSFER_MODE.CREATE) {
      IdentifierTO ito = new IdentifierTO();
      ito.setValue(pto.getIdentifiers().isEmpty() ? null : pto.getIdentifiers().get(0).getValue());
      p.setIdentifier(ito.getValue());
    }
    p.setRole(URI.create(pto.getRole()));
    p.setFamilyName(pto.getFamilyName());
    p.setGivenName(pto.getGivenName());
    p.setCompleteName(pto.getCompleteName());
    p.setAlternativeName(pto.getAlternativeName());
    // set organizations
    transferContributorOrganizations(pto.getOrganizations(), p, mode);
  }


  public static void transferCollectionContributors(List<PersonTO> persons,
      ContainerMetadata metadata, User u, TRANSFER_MODE mode) {
    for (PersonTO pTO : persons) {
      Person person = new Person();
      person.setFamilyName(pTO.getFamilyName());
      person.setGivenName(pTO.getGivenName());
      person.setCompleteName(pTO.getCompleteName());
      person.setAlternativeName(pTO.getAlternativeName());
      person.setRole(URI.create(pTO.getRole()));
      if (pTO.getIdentifiers().size() == 1) {
        // set the identifier of current person
        IdentifierTO ito = new IdentifierTO();
        ito.setValue(pTO.getIdentifiers().get(0).getValue());
        person.setIdentifier(ito.getValue());
      } else if (pTO.getIdentifiers().size() > 1) {
        LOGGER.warn("Multiple identifiers found for Person: " + pTO.getId());
      }
      // set organizations
      transferContributorOrganizations(pTO.getOrganizations(), person, mode);
      metadata.getPersons().add(person);
    }

    if (metadata.getPersons().size() == 0 && TRANSFER_MODE.CREATE.equals(mode) && u != null) {
      Person personU = new Person();
      PersonTO pTo = new PersonTO();
      personU.setFamilyName(u.getPerson().getFamilyName());
      personU.setGivenName(u.getPerson().getGivenName());
      personU.setCompleteName(u.getPerson().getCompleteName());
      personU.setAlternativeName(u.getPerson().getAlternativeName());
      if (!isNullOrEmpty(u.getPerson().getIdentifier())) {
        IdentifierTO ito = new IdentifierTO();
        ito.setValue(u.getPerson().getIdentifier());
        personU.setIdentifier(ito.getValue());
      }
      personU.setOrganizations(u.getPerson().getOrganizations());
      personU.setRole(URI.create(pTo.getRole()));
      metadata.getPersons().add(personU);
    }

  }

  public static void transferContributorOrganizations(List<OrganizationTO> orgs, Person person,
      TRANSFER_MODE mode) {
    for (OrganizationTO orgTO : orgs) {
      Organization org = new Organization();

      if (mode == TRANSFER_MODE.CREATE) {
        // TODO: Organization can have only one identifier, why
        // OrganizationTO has many?
        // get only first one!
        if (orgTO.getIdentifiers().size() > 0) {
          IdentifierTO ito = new IdentifierTO();
          ito.setValue(orgTO.getIdentifiers().get(0).getValue());
          org.setIdentifier(ito.getValue());
          if (orgTO.getIdentifiers().size() > 1) {
            LOGGER.info("Have more organization identifiers than needed");
          }
        }
      }

      org.setName(orgTO.getName());
      org.setDescription(orgTO.getDescription());
      org.setCity(orgTO.getCity());
      org.setCountry(orgTO.getCountry());
      person.getOrganizations().add(org);
    }
  }

  /**
   * Transfer a {@link DefaultItemTO} to an Item TODO Check Performance and refactor
   *
   * @param defaultTO
   * @param item
   * @param profileTO
   * @param updatedAll
   * @param u
   * @param mode
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws ImejiException
   */
  public static void transferDefaultMetadata(DefaultItemTO defaultTO, Item item,
      MetadataProfile profile, User u, TRANSFER_MODE mode) throws ImejiException {

    if (profile == null) {
      return;
    }
    ItemTO itemTO = new ItemTO();
    MetadataProfileTO profileTO = new MetadataProfileTO();
    TransferObjectFactory.transferMetadataProfile(profile, profileTO);
    for (String label : defaultTO.getMetadata().keySet()) {
      // Get statement according to the label - Note: this is always the toplevel
      StatementTO statement = ProfileTransferHelper.findStatementByLabel(label, profileTO);
      List<MetadataSetTO> newMetadata = new ArrayList<MetadataSetTO>();
      // Get the new metadata according to the json and the statement
      boolean isParent = ProfileTransferHelper.hasChildStatement(statement.getId(), profileTO);
      newMetadata.addAll(MetadataTransferHelper.parseMetadata(defaultTO.getMetadata().get(label),
          statement, isParent, "", profileTO));
      // add/replace the metadata to the itemto
      if (!newMetadata.isEmpty()) {
        // remove all the metadata with the same statement
        itemTO.clearMetadata(newMetadata.get(0).getStatementUri());
        // add the new metadata
        itemTO.getMetadata().addAll(newMetadata);
      }
    }
    transferItemMetadata(itemTO, item, profile, u, mode);
  }
}

