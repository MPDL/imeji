package de.mpg.imeji.util;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.j2j.controler.ResourceController;
import de.mpg.imeji.j2j.queries.Queries;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.db.reader.JenaReader;
import de.mpg.imeji.logic.db.reader.Reader;
import de.mpg.imeji.logic.db.repositories.*;
import de.mpg.imeji.logic.init.ImejiInitializer;
import de.mpg.imeji.logic.model.*;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.model.factory.UserFactory;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.security.authentication.impl.APIKeyAuthentication;
import de.mpg.imeji.logic.security.authorization.AuthorizationPredefinedRoles;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.TempFileUtil;
import org.apache.jena.tdb1.base.block.FileMode;
import org.apache.jena.tdb1.sys.SystemTDB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jose4j.lang.JoseException;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class JenaToDbMigration {

    private static final Logger LOGGER = LogManager.getLogger(JenaToDbMigration.class);

    private User admin;

    public void migrate() throws ImejiException{

       List<String> admins = ImejiSPARQL.exec(JenaCustomQueries.selectUserSysAdmin(), Imeji.userModel);


        try {
            admin = new User();
            admin.setId(URI.create(admins.get(0)));
            admin.setPerson(ImejiFactory.newPerson("Admin", "imeji", "Max Planck Digital Library"));
            admin.setEmail(Imeji.ADMIN_EMAIL_INIT);
            admin.setEncryptedPassword(StringHelper.md5(Imeji.ADMIN_PASSWORD_INIT));
            admin.setApiKey(APIKeyAuthentication.generateKey(this.admin.getId(), Integer.MAX_VALUE));
            admin.setGrants(AuthorizationPredefinedRoles.imejiAdministrator(Imeji.PROPERTIES.getBaseURI()));


            migrate(Imeji.collectionModel, JenaCustomQueries.selectCollectionAll(), new CollectionsDbRepository(), CollectionImeji.class);
            migrate(Imeji.imageModel, JenaCustomQueries.selectItemAll(), new ItemsDbRepository(), Item.class);
            migrate(Imeji.contentModel, JenaCustomQueries.selectContentAll(), new ContentDbRepository(), ContentVO.class);
            migrate(Imeji.statementModel, JenaCustomQueries.selectStatementAll(), new StatementDbRepository(), Statement.class);
            migrate(Imeji.facetModel, JenaCustomQueries.selectFacetAll(), new FacetDbRepository(), Facet.class);
            //Subscriptions are handled under the user Model in Jena
            migrate(Imeji.userModel, JenaCustomQueries.selectSubscriptionAll(), new SubscriptionDbRepository(), Subscription.class);
            migrateUsers();
        } catch (JoseException e) {
            throw new ImejiException("Error in migration",e);
        }


    }

    private void migrateUsers() throws ImejiException {
        LOGGER.info("Migrating users...");
        final Reader reader = new JenaReader(Imeji.userModel);
        //final ResourceController rc = new ResourceController(Imeji.dataset.getNamedModel(Imeji.userModel), false);
        final UserDbRepository userDbRepository = new UserDbRepository();
        final List<String> uris = ImejiSPARQL.exec(JenaCustomQueries.selectUserAll(), Imeji.userModel);
        LOGGER.info("Found {} users in Jena", uris.size());
        //final List<User> users = new ArrayList<>();
        int count = 0;
        for (final String uri : uris) {
            LOGGER.info(count + ": Loading object from Jena " + uri);
            //User emptyUser = new User();
            //emptyUser.setId(URI.create(uri));
            User result = (User) reader.read(uri, this.admin, new User());
            //User result = (User) rc.read(emptyUser);
            LOGGER.info("User successfully loaded from Jena " + uri);
            loadUsersUserGroups(result, reader);
            LOGGER.info("Writing object to database " + uri);
            userDbRepository.create(result);
            count++;
        }
        LOGGER.info("Finished migrating users");
    }

    private void migrate(String model, String jenaQueryAll, DbRepository dbRepository, Class objectClass) throws ImejiException {
        LOGGER.info("Migrating "+ model + "...");
        final Reader reader = new JenaReader(model);
        //final CollectionsDbRepository userDbRepository = new CollectionsDbRepository();
        final List<String> uris = ImejiSPARQL.exec(jenaQueryAll, model);
        LOGGER.info("Found {} objects in Jena", uris.size());
        //final List<User> users = new ArrayList<>();
        int count = 0;
        for (final String uri : uris) {
            LOGGER.info(count + ": Loading object from Jena " + uri);
            //User emptyUser = new User();
            //emptyUser.setId(URI.create(uri));
            Object result = null;
            try {
                result = reader.read(uri, this.admin, objectClass.newInstance());
            } catch (InstantiationException|IllegalAccessException e) {
                throw new ImejiException("Error creating instance of class " + objectClass.getName(), e);
            }
            //User result = (User) rc.read(emptyUser);
            LOGGER.info("Object successfully loaded from Jena " + uri);
            //loadUsersUserGroups(result, reader);
            LOGGER.info("Writing object to database " + uri);
            dbRepository.create(result);
            count++;
        }
        LOGGER.info("Finished migrating objects for " + model + " Count: " + count);
    }



  private void loadUsersUserGroups(User user, Reader resourceController) throws ImejiException {

      UserGroupDbRepository userGroupDbRepository = new UserGroupDbRepository();
    String getUserGroupsOfUserQuery = JenaCustomQueries.selectUserGroupOfUser(user);
    final List<String> groupURIs = ImejiSPARQL.exec(getUserGroupsOfUserQuery, Imeji.userModel);
    //List<String> groupURIs = Queries.executeSPARQLQueryAndGetResults(getUserGroupsOfUserQuery, dataset, userModelName);
    //List<UserGroup> groups = this.issuingUser.getGroups();
    if (groupURIs.size() > 0) {
      List<UserGroup> userGroupsWithUserInThem = new ArrayList<UserGroup>(groupURIs.size());
      for (String groupURI : groupURIs) {
        //UserGroup groupToRead = new UserGroup();
        //groupToRead.setId(URI.create(groupURI));
        Object readGroup = resourceController.read(groupURI, this.admin, new UserGroup());
        if (readGroup instanceof UserGroup) {
          UserGroup groupToRead = (UserGroup) readGroup;
          UserGroup ugFromDb = userGroupDbRepository.read(groupToRead.getId().toString());
          if(ugFromDb == null) {
              userGroupDbRepository.create(groupToRead);
          }
          userGroupsWithUserInThem.add(groupToRead);
        }
      }
      user.setGroups(userGroupsWithUserInThem);
    }







    }
}
