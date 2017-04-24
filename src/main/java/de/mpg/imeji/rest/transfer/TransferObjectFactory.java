package de.mpg.imeji.rest.transfer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.ContainerAdditionalInfo;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.Properties;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.rest.helper.CommonUtils;
import de.mpg.imeji.rest.helper.UserNameCache;
import de.mpg.imeji.rest.to.CollectionTO;
import de.mpg.imeji.rest.to.ContainerAdditionalInformationTO;
import de.mpg.imeji.rest.to.ContainerTO;
import de.mpg.imeji.rest.to.IdentifierTO;
import de.mpg.imeji.rest.to.LiteralConstraintTO;
import de.mpg.imeji.rest.to.MetadataProfileTO;
import de.mpg.imeji.rest.to.MetadataTO;
import de.mpg.imeji.rest.to.OrganizationTO;
import de.mpg.imeji.rest.to.PersonTO;
import de.mpg.imeji.rest.to.PersonTOBasic;
import de.mpg.imeji.rest.to.PropertiesTO;
import de.mpg.imeji.rest.to.StatementTO;
import de.mpg.imeji.rest.to.UserTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultOrganizationTO;
import de.mpg.imeji.rest.to.defaultItemTO.predefinedEasyMetadataTO.DefaultConePersonTO;

public class TransferObjectFactory {
  private static UserNameCache userNameCache = new UserNameCache();
  private static final Logger LOGGER = LoggerFactory.getLogger(TransferObjectFactory.class);

  /**
   * Transfer a {@link DefaultItemTO} into an {@link ItemTO} according to {@link MetadataProfileTO}
   *
   * @param profileTO
   * @param easyTO
   * @param itemTO
   * @throws BadRequestException
   * @throws JsonParseException
   * @throws JsonMappingException
   */


  /**
   * Transfer a list Statement in to MetadataProfileTO
   *
   * @param statements
   * @param to
   */
  private static void transferStatements(Collection<Statement> statements, MetadataProfileTO to) {
    to.getStatements().clear();
    for (final Statement t : statements) {
      final StatementTO sto = new StatementTO();
      sto.setId(t.getIndex());
      sto.setIndex(t.getIndex());
      sto.setType(t.getType().name());
      sto.setVocabulary(t.getVocabulary());
      for (final String s : t.getLiteralConstraints()) {
        final LiteralConstraintTO lcto = new LiteralConstraintTO();
        lcto.setValue(s);
        sto.getLiteralConstraints().add(lcto);
      }
      to.getStatements().add(sto);
    }

  }

  /**
   * Transfer an CollectionImeji to a CollectionTO
   *
   * @param vo
   * @param to
   */
  public static void transferCollection(CollectionImeji vo, CollectionTO to) {
    transferContainer(vo, to);
  }

  /**
   * Transfer a container to a containerTO
   *
   * @param vo
   * @param to
   */
  private static void transferContainer(CollectionImeji vo, ContainerTO to) {
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
      final IdentifierTO ito = new IdentifierTO();
      ito.setValue(p.getIdentifier());
      pto.getIdentifiers().add(ito);
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
      if (Double.isNaN(vo.getNumber())) {
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
      List<de.mpg.imeji.logic.vo.License> licenses) {
    final List<de.mpg.imeji.rest.to.LicenseTO> licenseTOs = new ArrayList<>();
    for (final de.mpg.imeji.logic.vo.License lic : licenses) {
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
