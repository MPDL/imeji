package de.mpg.imeji.rest.transfer;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.CollectionElement;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContainerAdditionalInfo;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Metadata;
import de.mpg.imeji.logic.model.Organization;
import de.mpg.imeji.logic.model.Person;
import de.mpg.imeji.logic.model.Properties;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.rest.helper.CommonUtils;
import de.mpg.imeji.rest.helper.UserNameCache;
import de.mpg.imeji.rest.to.CollectionElementTO;
import de.mpg.imeji.rest.to.CollectionTO;
import de.mpg.imeji.rest.to.ContainerAdditionalInformationTO;
import de.mpg.imeji.rest.to.IdentifierTO;
import de.mpg.imeji.rest.to.MetadataTO;
import de.mpg.imeji.rest.to.OrganizationTO;
import de.mpg.imeji.rest.to.PersonTO;
import de.mpg.imeji.rest.to.PersonTOBasic;
import de.mpg.imeji.rest.to.PropertiesTO;
import de.mpg.imeji.rest.to.UserTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultOrganizationTO;
import de.mpg.imeji.rest.to.defaultItemTO.predefinedEasyMetadataTO.DefaultConePersonTO;

public class TransferVOtoTO {
  private static UserNameCache userNameCache = new UserNameCache();
  private static final Logger LOGGER = LoggerFactory.getLogger(TransferVOtoTO.class);


  /**
   * Transfer an CollectionImeji to a CollectionTO
   *
   * @param vo
   * @param to
   */
  public static void transferCollection(CollectionImeji vo, CollectionTO to) {
    if (!StringHelper.isNullOrEmptyTrim(vo.getCollection())) {
      to.setCollectionId(ObjectHelper.getId(vo.getCollection()));
    }
    transferProperties(vo, to);
    to.setTitle(vo.getTitle());
    to.setDescription(vo.getDescription());
    to.setAdditionalInfos(transferAdditionalInfos(vo.getAdditionalInformations()));
    for (final Person p : vo.getPersons()) {
      final PersonTO pto = new PersonTO();
      transferPerson(p, pto);
      to.getContributors().add(pto);
    }
  }

  /**
   * Transfer a CollectionElement to a CollectionElementTO
   * 
   * @param element
   * @return
   */
  public static CollectionElementTO toCollectionelementTO(CollectionElement element) {
    CollectionElementTO to = new CollectionElementTO();
    to.setId(ObjectHelper.getId(URI.create(element.getUri())));
    to.setName(element.getName());
    to.setType(element.getType().name());
    return to;
  }

  /**
   * Transfer a list of ContainerAdditionalInfo to a list of ContainerAdditionalInformationTO
   *
   * @param vos
   * @return
   */
  private static List<ContainerAdditionalInformationTO> transferAdditionalInfos(
      List<ContainerAdditionalInfo> vos) {
    final List<ContainerAdditionalInformationTO> tos = new ArrayList<>();
    for (final ContainerAdditionalInfo vo : vos) {
      tos.add(new ContainerAdditionalInformationTO(vo.getLabel(), vo.getText(), vo.getUrl()));
    }
    return tos;
  }


  /**
   * Transfer a {@link User} to a {@link UserTO}
   *
   * @param vo
   * @param to
   */
  public static UserTO transferUser(User vo) {
    final UserTO to = new UserTO();
    transferPerson(vo.getPerson(), to.getPerson());
    to.setApiKey(vo.getApiKey());
    to.setEmail(vo.getEmail());
    to.setQuota(vo.getQuota());
    return to;
  }

  /**
   * Transfer a {@link Person} into a {@link PersonTO}
   *
   * @param p
   * @param pto
   */
  public static PersonTO transferPerson(Person p, PersonTO pto) {
    if (p != null && !StringHelper.isNullOrEmptyTrim(p.getFamilyName())) {
      pto.setId(CommonUtils.extractIDFromURI(p.getId()));
      pto.setFamilyName(p.getFamilyName());
      pto.setGivenName(p.getGivenName());
      if (!StringHelper.isNullOrEmptyTrim(p.getIdentifier())) {
        final IdentifierTO ito = new IdentifierTO();
        ito.setValue(p.getIdentifier());
        pto.setIdentifiers(Arrays.asList(ito));
      }
      // set oganizations
      transferContributorOrganizations(p.getOrganizations(), pto);
      return pto;
    }
    return null;
  }

  /**
   * Transfer a {@link Person} into a {@link DefaultConePersonTO}
   *
   * @param p
   * @param pTO
   */
  public static void transferDefaultPerson(Person p, DefaultConePersonTO pTO) {
    pTO.setFamilyName(p.getFamilyName());
    pTO.setGivenName(p.getGivenName());
    for (final Organization o : p.getOrganizations()) {
      final DefaultOrganizationTO oTO = new DefaultOrganizationTO();
      transferDefaultOrganization(o, oTO);
      pTO.getOrganizations().add(oTO);
    }
  }

  /**
   * Transfer an {@link Organization} into a {@link DefaultOrganizationTO}
   *
   * @param o
   * @param oTO
   */
  public static void transferDefaultOrganization(Organization o, DefaultOrganizationTO oTO) {
    oTO.setName(o.getName());
  }

  public static void transferContributorOrganizations(Collection<Organization> orgas,
      PersonTO pto) {
    for (final Organization orga : orgas) {
      final OrganizationTO oto = new OrganizationTO();
      oto.setId(CommonUtils.extractIDFromURI(orga.getId()));
      oto.setName(orga.getName());
      pto.getOrganizations().add(oto);
    }

  }

  /**
   * Transfer {@link Properties} to {@link PropertiesTO}
   *
   * @param vo
   * @param to
   */
  public static void transferProperties(Properties vo, PropertiesTO to) {
    // set ID
    to.setId(vo.getIdString());
    // set createdBy
    to.setCreatedBy(new PersonTOBasic(userNameCache.getUserName(vo.getCreatedBy()),
        ObjectHelper.getId(vo.getCreatedBy())));
    // set modifiedBy
    to.setModifiedBy(new PersonTOBasic(userNameCache.getUserName(vo.getModifiedBy()),
        ObjectHelper.getId(vo.getCreatedBy())));
    // set createdDate, modifiedDate, versionDate
    to.setCreatedDate(CommonUtils.formatDate(vo.getCreated().getTime()));
    to.setModifiedDate(CommonUtils.formatDate(vo.getModified().getTime()));
    // set status
    to.setStatus(vo.getStatus().toString());
    // set discardComment
    to.setDiscardComment(vo.getDiscardComment());
  }

  /**
   * Transfer {@link Item} to {@link DefaultItemTO}
   *
   * @param vo
   * @param to
   */
  public static void transferDefaultItem(Item vo, DefaultItemTO to) {
    transferProperties(vo, to);
    to.setCollectionId(CommonUtils.extractIDFromURI(vo.getCollection()));
    to.setFilename(vo.getFilename());
    to.setFileSize(vo.getFileSize());
    to.setMimetype(vo.getFiletype());
    to.setLicenses(transferLicense(vo.getLicenses()));
    to.setMetadata(transferMetadata(vo.getMetadata()));
    to.setFileUrl(URI.create(Imeji.PROPERTIES.getApplicationURL() + "file?itemId="
        + vo.getIdString() + "&resolution=original"));
  }

  public static DefaultItemTO toItemTO(Item vo) {
    DefaultItemTO to = new DefaultItemTO();
    transferProperties(vo, to);
    to.setCollectionId(CommonUtils.extractIDFromURI(vo.getCollection()));
    to.setFilename(vo.getFilename());
    to.setFileSize(vo.getFileSize());
    to.setMimetype(vo.getFiletype());
    to.setLicenses(transferLicense(vo.getLicenses()));
    to.setMetadata(transferMetadata(vo.getMetadata()));
    to.setFileUrl(URI.create(Imeji.PROPERTIES.getApplicationURL() + "file?itemId="
        + vo.getIdString() + "&resolution=original"));
    return to;
  }

  /**
   * Transfer a list of {@link Metadata} to a list of {@link MetadataTO}
   *
   * @param metadata
   * @return
   */
  public static List<MetadataTO> transferMetadata(List<Metadata> metadata) {
    final List<MetadataTO> tos = new ArrayList<>();
    for (final Metadata vo : metadata) {
      final MetadataTO to = new MetadataTO();
      to.setText(vo.getText());
      to.setDate(vo.getDate());
      to.setName(vo.getName());
      to.setTitle(vo.getTitle());
      if (!Double.isNaN(vo.getNumber())) {
        to.setNumber(vo.getNumber());
      }
      to.setUrl(vo.getUrl());
      to.setPerson(transferPerson(vo.getPerson(), new PersonTO()));
      to.setLatitude(vo.getLatitude());
      to.setLongitude(vo.getLongitude());
      to.setIndex(vo.getIndex());
      tos.add(to);
    }
    return tos;
  }

  /**
   * Transfer a list of LicenseVO to a list of LicenseTO
   *
   * @param licenses
   * @return
   */
  private static List<de.mpg.imeji.rest.to.LicenseTO> transferLicense(
      List<de.mpg.imeji.logic.model.License> licenses) {
    final List<de.mpg.imeji.rest.to.LicenseTO> licenseTOs = new ArrayList<>();
    for (final de.mpg.imeji.logic.model.License lic : licenses) {
      final de.mpg.imeji.rest.to.LicenseTO licTO = new de.mpg.imeji.rest.to.LicenseTO();
      licTO.setName(lic.getName());
      licTO.setLabel(lic.getLabel());
      licTO.setUrl(lic.getUrl());
      licTO.setStart(lic.getStartTime());
      licTO.setEnd(lic.getEndTime());
      licenseTOs.add(licTO);
    }
    return licenseTOs;
  }


  public static int getPosition(Map<Integer, String> positions, String statement) {
    if (!positions.containsValue(statement)) {
      positions.put(0, statement);
      return 0;
    } else {
      int i = 0;
      for (final Map.Entry<Integer, String> entry : positions.entrySet()) {
        if (statement.equals(entry.getValue())) {
          i++;
        }
      }
      positions.put(i, statement);
      return i;
    }
  }
}
