package de.mpg.imeji.logic.db.repositories;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.writer.DbWriter;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class UserDbRepository extends DbRepository<User> {

    private static Logger LOGGER = LogManager.getLogger(UserDbRepository.class);

    public UserDbRepository() {
        super(User.class);
    }


    public List<User> readByEmail(String email) throws ImejiException {
        return inSession(em -> {
            return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getResultList();
        });
    }

    public List<User> retrieveAllAdmins() throws ImejiException {
        return inSession(em -> {
            String adminRule = "ADMIN," + Imeji.PROPERTIES.getBaseURI();
            return em.createNativeQuery("select * from users where jsonb_exists(grants, :adminGrant);", User.class)
                    .setParameter("adminGrant", adminRule)
                    .getResultList();
        });
    }

    public List<User> retrieveAllUsersForGroup(String userGroupId) throws ImejiException {
        return inSession(em -> {
            //String adminRule = "ADMIN," + Imeji.PROPERTIES.getBaseURI();
            UserGroup ug = em.find(UserGroup.class, userGroupId);
            return em.createQuery("select u from User u where u.dbId IN :uIds", User.class)
                    .setParameter("uIds", ug.getUsers().stream().map(uri -> uri.toString()).toList())
                    .getResultList();
        });
    }

    public List<String> retrieveAllDomains() throws ImejiException {
        return inSession(em -> {
            //String adminRule = "ADMIN," + Imeji.PROPERTIES.getBaseURI();
            return em.createNativeQuery("SELECT DISTINCT substring(email FROM '@(.*)$') AS domain FROM users;", String.class)
                    .getResultList();
        });
    }

    public int countObjectsModifiedOrCreated(String id) throws ImejiException {
        return inSession(em -> {
            String adminRule = "ADMIN," + Imeji.PROPERTIES.getBaseURI();
            Object result = em.createNativeQuery("select count(*) from collection c, item i, statement s , facet f, users u where " +
                            "c.modifiedBy = :userId OR c.createdBy :userId OR" +
                            "i.modifiedBy = :userId OR i.createdBy :userId OR" +
                            "s.modifiedBy = :userId OR s.createdBy :userId OR" +
                            "f.modifiedBy = :userId OR f.createdBy :userId OR" +
                            "u.modifiedBy = :userId OR u.createdBy :userId")
                    .setParameter("userId", id)
                    .getSingleResult();
            return ((Number) result).intValue();
        });
    }

    public long getFileSize(String userId) throws ImejiException {
        return inSession(em -> {
            Object result = em.createNativeQuery("select sum(filesize) from item where createdBy=:creatorId AND status='http://imeji.org/terms/status#WITHDRAWN';")
                    .setParameter("creatorId", userId)
                    .getSingleResult();
            if(result == null) {
                return 0L;
            }
            else  {
                return ((Number) result).longValue();
            }
        });
    }

    public long getFileSizeForDomain(String mailDomain) throws ImejiException {
        return inSession(em -> {
            Object result = em.createNativeQuery("SELECT sum(filesize) FROM content INNER JOIN item ON content.itemid = item.id INNER JOIN users on item.createdby = users.id WHERE users.email ilike :mailDomain")
                    .setParameter("mailDomain", "%@" + mailDomain)
                    .getSingleResult();
            if(result == null) {
                return 0L;
            }
            else  {
                return ((Number) result).longValue();
            }
        });
    }

    public long getFileSizeForAll() throws ImejiException {
        return inSession(em -> {
            Object result = em.createNativeQuery("SELECT sum(filesize) FROM item")
                    .getSingleResult();
            if(result == null) {
                return 0L;
            }
            else  {
                return ((Number) result).longValue();
            }
        });
    }

    public void removeGrantsForObject(String objectId) throws ImejiException {
        inSession(em -> {
            List<User> result = em.createNativeQuery("SELECT * FROM users u WHERE u.grants::text ILIKE :objectId", User.class)
                    .setParameter("objectId", "%," + objectId + "%")
                    .getResultList();
            for (User user : result) {
                LOGGER.info("Removing grant for object " + objectId + " from user " + user.getEmail());
                user.getGrants().removeIf(g -> g.contains("," + objectId));
                em.merge(user);
            }
            return null;
        });
    }

    public void removeZombieGrants() throws ImejiException {
        inSession(em -> {

            String query = """
                    SELECT DISTINCT grantForId, u.dbId AS userId FROM users u JOIN LATERAL (SELECT substring(jsonb_array_elements_text(grants) FROM ',(.*)$') AS grantForId) AS x ON true
                                                        WHERE grantForId LIKE 'http://imeji.org/collection/%'
                                                        AND grantForId NOT IN (SELECT dbid FROM collection);
                    """;
            List<Object[]> result = em.createNativeQuery(query).getResultList();
            for (Object[] res : result) {
                User user = em.find(User.class, (String)res[1]);
                String grantForId = (String)res[0];
                LOGGER.info("Removing grant for object " + grantForId + " from user " + user.getEmail());
                user.getGrants().removeIf(g -> g.contains("," + grantForId));
                em.merge(user);
            }
            return null;
        });
    }




}
