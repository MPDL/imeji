package de.mpg.imeji.rest.transfer;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Container;
import de.mpg.imeji.logic.vo.ContainerAdditionalInfo;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.Properties;
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
import de.mpg.imeji.rest.helper.CommonUtils;
import de.mpg.imeji.rest.helper.UserNameCache;
import de.mpg.imeji.rest.to.AlbumTO;
import de.mpg.imeji.rest.to.CollectionTO;
import de.mpg.imeji.rest.to.ContainerAdditionalInformationTO;
import de.mpg.imeji.rest.to.ContainerTO;
import de.mpg.imeji.rest.to.IdentifierTO;
import de.mpg.imeji.rest.to.ItemTO;
import de.mpg.imeji.rest.to.LabelTO;
import de.mpg.imeji.rest.to.LiteralConstraintTO;
import de.mpg.imeji.rest.to.MetadataProfileTO;
import de.mpg.imeji.rest.to.MetadataSetTO;
import de.mpg.imeji.rest.to.OrganizationTO;
import de.mpg.imeji.rest.to.PersonTO;
import de.mpg.imeji.rest.to.PersonTOBasic;
import de.mpg.imeji.rest.to.PropertiesTO;
import de.mpg.imeji.rest.to.StatementTO;
import de.mpg.imeji.rest.to.UserTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultOrganizationTO;
import de.mpg.imeji.rest.to.defaultItemTO.predefinedEasyMetadataTO.DefaultConePersonTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.ConePersonTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.DateTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.GeolocationTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.LicenseTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.LinkTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.NumberTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.PublicationTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.TextTO;
import de.mpg.imeji.util.LocalizedString;

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
   * Transfer a {@link MetadataProfile} into a {@link MetadataProfileTO}
   *
   * @param vo
   * @param to
   */
  public static void transferMetadataProfile(MetadataProfile vo, MetadataProfileTO to) {
    transferProperties(vo, to);
    to.setTitle(vo.getTitle());
    to.setDefault(vo.getDefault());
    to.setDescription(vo.getDescription());
    transferStatements(vo.getStatements(), to);
  }

  /**
   * Transfer a list Statement in to MetadataProfileTO
   *
   * @param stats
   * @param to
   */
  private static void transferStatements(Collection<Statement> stats, MetadataProfileTO to) {
    to.getStatements().clear();
    for (Statement t : stats) {
      StatementTO sto = new StatementTO();
      sto.setId(CommonUtils.extractIDFromURI(t.getId()));
      sto.setPos(t.getPos());
      sto.setType(t.getType());
      sto.setLabels(new ArrayList<LocalizedString>(t.getLabels()));
      sto.setVocabulary(t.getVocabulary());
      for (String s : t.getLiteralConstraints()) {
        LiteralConstraintTO lcto = new LiteralConstraintTO();
        lcto.setValue(s);
        sto.getLiteralConstraints().add(lcto);
      }
      sto.setMinOccurs(t.getMinOccurs());
      sto.setMaxOccurs(t.getMaxOccurs());
      if (t.getParent() != null) {
        sto.setParentStatementId(CommonUtils.extractIDFromURI(t.getParent()));
      }
      sto.setUseInPreview(t.isPreview());
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
    // in output jsen reference to mdprofile
    if (vo.getProfile() != null) {
      to.getProfile().setId(CommonUtils.extractIDFromURI(vo.getProfile()));
      to.getProfile().setMethod("");
    }
  }

  /**
   * Transfer an Album to an AlbumTO
   *
   * @param vo
   * @param to
   */
  public static void transferAlbum(Album vo, AlbumTO to) {
    transferContainer(vo, to);
  }

  /**
   * Transfer a container to a containerTO
   *
   * @param vo
   * @param to
   */
  private static void transferContainer(Container vo, ContainerTO to) {
    transferProperties(vo, to);
    to.setTitle(vo.getMetadata().getTitle());
    to.setDescription(vo.getMetadata().getDescription());
    to.setAdditionalInfos(transferAdditionalInfos(vo.getMetadata().getAdditionalInformations()));
    for (Person p : vo.getMetadata().getPersons()) {
      PersonTO pto = new PersonTO();
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
    List<ContainerAdditionalInformationTO> tos = new ArrayList<>();
    for (ContainerAdditionalInfo vo : vos) {
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
    UserTO to = new UserTO();
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
  public static void transferPerson(Person p, PersonTO pto) {

    // pto.setPosition(p.getPos());
    pto.setId(CommonUtils.extractIDFromURI(p.getId()));
    pto.setFamilyName(p.getFamilyName());
    pto.setGivenName(p.getGivenName());
    pto.setCompleteName(p.getCompleteName());
    pto.setAlternativeName(p.getAlternativeName());
    pto.setRole(p.getRole() == null ? "" : p.getRole().toString());
    IdentifierTO ito = new IdentifierTO();
    ito.setValue(p.getIdentifier());
    pto.getIdentifiers().add(ito);
    // set oganizations
    transferContributorOrganizations(p.getOrganizations(), pto);

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
    for (Organization o : p.getOrganizations()) {
      DefaultOrganizationTO oTO = new DefaultOrganizationTO();
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
    oTO.setDescription(o.getDescription());
    oTO.setCity(o.getCity());
    oTO.setCountry(o.getCountry());
  }

  public static void transferContributorOrganizations(Collection<Organization> orgas,
      PersonTO pto) {
    for (Organization orga : orgas) {
      OrganizationTO oto = new OrganizationTO();
      oto.setId(CommonUtils.extractIDFromURI(orga.getId()));
      oto.setName(orga.getName());
      oto.setDescription(orga.getDescription());
      IdentifierTO ito = new IdentifierTO();
      ito.setValue(orga.getIdentifier());
      oto.getIdentifiers().add(ito);
      oto.setCity(orga.getCity());
      oto.setCountry(orga.getCountry());
      pto.getOrganizations().add(oto);
    }

  }

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
    to.setVersionDate(
        (vo.getVersionDate() != null) ? CommonUtils.formatDate(vo.getVersionDate().getTime()) : "");
    // set status
    to.setStatus(vo.getStatus().toString());
    // set version
    to.setVersion(vo.getVersion());
    // set discardComment
    to.setDiscardComment(vo.getDiscardComment());
  }

  /**
   * Transfer an {@link Item} into a {@link ItemTO}
   *
   * @param vo
   * @param to
   */
  public static void transferItem(Item vo, ItemTO to, MetadataProfile profile) {
    transferProperties(vo, to);
    // set collectionID
    to.setCollectionId(CommonUtils.extractIDFromURI(vo.getCollection()));
    to.setFilename(vo.getFilename());
    to.setFileSize(vo.getFileSize());
    to.setMimetype(vo.getFiletype());
    to.setChecksumMd5(vo.getChecksum());
    to.setWebResolutionUrlUrl(vo.getWebImageUrl());
    to.setThumbnailUrl(vo.getThumbnailImageUrl());
    to.setFileUrl(vo.getFullImageUrl());
    transferItemMetadata(profile, vo.getMetadataSet().getMetadata(), to);
  }


  public static void transferDefaultItem(Item vo, DefaultItemTO to, MetadataProfile profile) {
    transferProperties(vo, to);
    // set collectionID
    to.setCollectionId(CommonUtils.extractIDFromURI(vo.getCollection()));
    to.setFilename(vo.getFilename());
    to.setFileSize(vo.getFileSize());
    to.setMimetype(vo.getFiletype());
    to.setChecksumMd5(vo.getChecksum());
    to.setWebResolutionUrlUrl(vo.getWebImageUrl());
    to.setThumbnailUrl(vo.getThumbnailImageUrl());
    to.setFileUrl(vo.getFullImageUrl());
    to.setLicenses(transferLicense(vo.getLicenses()));
    transferItemMetadataDefault(profile, vo.getMetadataSet().getMetadata(), to);
  }

  /**
   * Transfer a list of LicenseVO to a list of LicenseTO
   * 
   * @param licenses
   * @return
   */
  private static List<de.mpg.imeji.rest.to.LicenseTO> transferLicense(
      List<de.mpg.imeji.logic.vo.License> licenses) {
    List<de.mpg.imeji.rest.to.LicenseTO> licenseTOs = new ArrayList<>();
    for (de.mpg.imeji.logic.vo.License lic : licenses) {
      de.mpg.imeji.rest.to.LicenseTO licTO = new de.mpg.imeji.rest.to.LicenseTO();
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
      for (Map.Entry<Integer, String> entry : positions.entrySet()) {
        if (statement.equals(entry.getValue())) {
          i++;
        }
      }
      positions.put(i, statement);
      return i;
    }
  }

  /**
   * Transfer a {@link List} of {@link Metadata} into default Metadata json and set it to the
   * {@link DefaultItemTO}
   *
   * @param profile
   * @param voMds
   * @param to
   */
  public static void transferItemMetadataDefault(MetadataProfile profile,
      Collection<Metadata> voMds, DefaultItemTO to) {
    if (voMds.size() == 0) {
      return;
    }
    to.setMetadata(MetadataTransferHelper.serializeMetadataSet(voMds, profile));
  }

  public static void transferItemMetadata(MetadataProfile profile, Collection<Metadata> voMds,
      ItemTO to) {

    if (voMds.size() == 0) {
      // to.setMetadata(null);
      return;
    }

    // get all statements of the Profile!
    int mdPosition = 0;
    for (Metadata md : voMds) {
      md.getId();
      MetadataSetTO mdTO = new MetadataSetTO();
      // mdTO.setPosition(md.getPos());
      mdTO.setStatementUri(md.getStatement());
      mdTO.setTypeUri(URI.create(md.getTypeNamespace()));
      // NB
      mdTO.setPosition(mdPosition);

      if (profile.getStatements().size() > 0) {

        List<LabelTO> ltos = new ArrayList<LabelTO>();
        for (Statement s : profile.getStatements()) {
          if (s.getId().toString().equals(md.getStatement().toString())) {
            for (LocalizedString ls : s.getLabels()) {
              LabelTO lto = new LabelTO(ls.getLang(), ls.getValue());
              ltos.add(lto);
            }

            if (s.getParent() != null) {
              mdTO.setParentStatementUri(s.getParent().toString());
            }

            break;
          }
        }

        mdTO.setLabels(ltos);
      }

      switch (md.getClass().getName()) {
        case "de.mpg.imeji.logic.vo.predefinedMetadata.Text":
          Text mdText = (Text) md;
          TextTO tt = new TextTO();
          tt.setText(mdText.getText());
          mdTO.setValue(tt);
          break;
        case "de.mpg.imeji.logic.vo.predefinedMetadata.Number":
          Number mdNumber = (Number) md;
          NumberTO nt = new NumberTO();
          nt.setNumber(mdNumber.getNumber());
          mdTO.setValue(nt);
          break;
        case "de.mpg.imeji.logic.vo.predefinedMetadata.ConePerson":
          ConePerson mdCP = (ConePerson) md;
          ConePersonTO cpto = new ConePersonTO();
          PersonTO personTo = new PersonTO();
          cpto.setPerson(personTo);
          transferPerson(mdCP.getPerson(), cpto.getPerson());
          mdTO.setValue(cpto);
          break;
        case "de.mpg.imeji.logic.vo.predefinedMetadata.Date":
          de.mpg.imeji.logic.vo.predefinedMetadata.Date mdDate =
              (de.mpg.imeji.logic.vo.predefinedMetadata.Date) md;
          DateTO dt = new DateTO();
          dt.setDate(mdDate.getDate());
          mdTO.setValue(dt);
          break;
        case "de.mpg.imeji.logic.vo.predefinedMetadata.Geolocation":
          Geolocation mdGeo = (Geolocation) md;
          GeolocationTO gto = new GeolocationTO();
          gto.setName(mdGeo.getName());
          gto.setLongitude(mdGeo.getLongitude());
          gto.setLatitude(mdGeo.getLatitude());
          mdTO.setValue(gto);
          break;
        case "de.mpg.imeji.logic.vo.predefinedMetadata.License":
          License mdLicense = (License) md;
          LicenseTO lto = new LicenseTO();
          lto.setLicense(mdLicense.getLicense());
          final URI externalUri = mdLicense.getExternalUri();
          lto.setUrl(externalUri != null ? externalUri.toString() : "");
          mdTO.setValue(lto);
          break;
        case "de.mpg.imeji.logic.vo.predefinedMetadata.Link":
          Link mdLink = (Link) md;
          LinkTO llto = new LinkTO();
          llto.setLink(mdLink.getLabel());
          llto.setUrl(mdLink.getUri() != null ? mdLink.getUri().toString() : "");
          mdTO.setValue(llto);
          break;
        case "de.mpg.imeji.logic.vo.predefinedMetadata.Publication":
          Publication mdP = (Publication) md;
          PublicationTO pto = new PublicationTO();
          pto.setPublication(mdP.getUri() != null ? mdP.getUri().toString() : "");
          pto.setFormat(mdP.getExportFormat());
          pto.setCitation(mdP.getCitation());
          mdTO.setValue(pto);
          break;
      }

      to.getMetadata().add(mdTO);
    }
  }

}
