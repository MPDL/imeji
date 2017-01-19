package de.mpg.imeji.rest.transfer;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Container;
import de.mpg.imeji.logic.vo.ContainerAdditionalInfo;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.factory.MetadataFactory;
import de.mpg.imeji.rest.to.AlbumTO;
import de.mpg.imeji.rest.to.CollectionTO;
import de.mpg.imeji.rest.to.ContainerAdditionalInformationTO;
import de.mpg.imeji.rest.to.IdentifierTO;
import de.mpg.imeji.rest.to.ItemTO;
import de.mpg.imeji.rest.to.MetadataTO;
import de.mpg.imeji.rest.to.OrganizationTO;
import de.mpg.imeji.rest.to.PersonTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;

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
    vo.setTitle(to.getTitle());
    vo.setDescription(to.getDescription());
    vo.setAdditionalInformations(transferAdditionalInfos(to.getAdditionalInfos()));
    // set contributors
    transferCollectionContributors(to.getContributors(), vo, u, mode);
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
    vo.setTitle(to.getTitle());
    vo.setDescription(to.getDescription());
    vo.setAdditionalInformations(transferAdditionalInfos(to.getAdditionalInfos()));
    // set contributors
    transferCollectionContributors(to.getContributors(), vo, u, mode);
  }


  /**
   * Transfer the list of ContainerAdditionalInformationTO to List of ContainerAdditionalInfo
   *
   * @param infosTO
   * @return
   */
  private static List<ContainerAdditionalInfo> transferAdditionalInfos(
      List<ContainerAdditionalInformationTO> infosTO) {
    final List<ContainerAdditionalInfo> infos = new ArrayList<>();
    for (final ContainerAdditionalInformationTO infoTO : infosTO) {
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
  public static void transferDefaultItem(DefaultItemTO to, Item vo, User u, TRANSFER_MODE mode)
      throws ImejiException {
    if (mode == TRANSFER_MODE.CREATE) {

      if (!isNullOrEmpty(to.getCollectionId())) {
        vo.setCollection(ObjectHelper.getURI(CollectionImeji.class, to.getCollectionId()));
      }
    }
    if (!isNullOrEmpty(to.getFilename())) {
      vo.setFilename(to.getFilename());
    }
    vo.getLicenses().addAll(transferLicenses(to.getLicenses()));

    transferItemMetadata(to, vo, u, mode);
  }

  /**
   * Transfer a LicenseVO to a LicenseTO
   *
   * @param licenseTOs
   * @return
   */
  private static List<de.mpg.imeji.logic.vo.License> transferLicenses(
      List<de.mpg.imeji.rest.to.LicenseTO> licenseTOs) {
    final List<de.mpg.imeji.logic.vo.License> licenses = new ArrayList<>();
    if (licenseTOs != null) {
      for (final de.mpg.imeji.rest.to.LicenseTO licTO : licenseTOs) {
        final de.mpg.imeji.logic.vo.License lic = new de.mpg.imeji.logic.vo.License();
        lic.setLabel(
            StringHelper.isNullOrEmptyTrim(licTO.getLabel()) ? licTO.getName() : licTO.getLabel());
        lic.setName(licTO.getName());
        lic.setUrl(licTO.getUrl());
        licenses.add(lic);
      }
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
  public static void transferItemMetadata(DefaultItemTO to, Item vo, User u, TRANSFER_MODE mode)
      throws ImejiException {
    final List<Metadata> voMDs = vo.getMetadata();
    voMDs.clear();
    for (final MetadataTO mdTO : to.getMetadata()) {
      final Metadata mdVO = new MetadataFactory().setStatementId(mdTO.getStatementId())
          .setText(mdTO.getText()).setNumber(mdTO.getNumber()).setUrl(mdTO.getUrl())
          .setPerson(transferPerson(mdTO.getPerson(), new Person(), mode))
          .setLatitude(mdTO.getLatitude()).setLongitude(mdTO.getLongitude()).build();
      vo.getMetadata().add(mdVO);
    }
  }

  /**
   * Transfer a {@link PersonTO} into a {@link Person}
   *
   * @param pto
   * @param p
   * @param mode
   */
  public static Person transferPerson(PersonTO pto, Person p, TRANSFER_MODE mode) {
    if (mode == TRANSFER_MODE.CREATE) {
      final IdentifierTO ito = new IdentifierTO();
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
    return p;
  }


  public static void transferCollectionContributors(List<PersonTO> persons, Container vo, User u,
      TRANSFER_MODE mode) {
    for (final PersonTO pTO : persons) {
      final Person person = new Person();
      person.setFamilyName(pTO.getFamilyName());
      person.setGivenName(pTO.getGivenName());
      person.setCompleteName(pTO.getCompleteName());
      person.setAlternativeName(pTO.getAlternativeName());
      person.setRole(URI.create(pTO.getRole()));
      if (pTO.getIdentifiers().size() == 1) {
        // set the identifier of current person
        final IdentifierTO ito = new IdentifierTO();
        ito.setValue(pTO.getIdentifiers().get(0).getValue());
        person.setIdentifier(ito.getValue());
      } else if (pTO.getIdentifiers().size() > 1) {
        LOGGER.warn("Multiple identifiers found for Person: " + pTO.getId());
      }
      // set organizations
      transferContributorOrganizations(pTO.getOrganizations(), person, mode);
      vo.getPersons().add(person);
    }

    if (vo.getPersons().size() == 0 && TRANSFER_MODE.CREATE.equals(mode) && u != null) {
      final Person personU = new Person();
      final PersonTO pTo = new PersonTO();
      personU.setFamilyName(u.getPerson().getFamilyName());
      personU.setGivenName(u.getPerson().getGivenName());
      personU.setCompleteName(u.getPerson().getCompleteName());
      personU.setAlternativeName(u.getPerson().getAlternativeName());
      if (!isNullOrEmpty(u.getPerson().getIdentifier())) {
        final IdentifierTO ito = new IdentifierTO();
        ito.setValue(u.getPerson().getIdentifier());
        personU.setIdentifier(ito.getValue());
      }
      personU.setOrganizations(u.getPerson().getOrganizations());
      personU.setRole(URI.create(pTo.getRole()));
      vo.getPersons().add(personU);
    }

  }

  public static void transferContributorOrganizations(List<OrganizationTO> orgs, Person person,
      TRANSFER_MODE mode) {
    for (final OrganizationTO orgTO : orgs) {
      final Organization org = new Organization();

      if (mode == TRANSFER_MODE.CREATE) {
        // TODO: Organization can have only one identifier, why
        // OrganizationTO has many?
        // get only first one!
        if (orgTO.getIdentifiers().size() > 0) {
          final IdentifierTO ito = new IdentifierTO();
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
}

