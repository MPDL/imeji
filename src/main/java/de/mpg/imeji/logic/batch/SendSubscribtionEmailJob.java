package de.mpg.imeji.logic.batch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.collection.CollectionService;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.share.email.EmailService;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.navigation.Navigation;

/**
 * For each collection that has changed, send email to subscribed user with changes. (Only one email
 * per user)
 * 
 * @author jandura
 *
 */
public class SendSubscribtionEmailJob implements Callable<Integer> {
  private static final Logger LOGGER = Logger.getLogger(SendSubscribtionEmailJob.class);

  @Override
  public Integer call() throws ImejiException {
    Map<String, String> collectionText = new HashMap<String, String>();
    List<User> users = new UserService().retrieveAll();
    Calendar yesterday = Calendar.getInstance();
    yesterday.add(Calendar.DAY_OF_MONTH, -1);

    Collection<Item> items = new ItemService().retrieveAll(Imeji.adminUser).stream()
        .filter(i -> i.getModified().after(yesterday)).collect(Collectors.toList());

    for (User u : users) {
      String emailText = "";
      List<String> unsubscribe = new ArrayList<String>();
      for (String collectionId : u.getSubscriptionCollections()) {
        if (collectionText.containsKey(collectionId)) {
          emailText += "\n" + collectionText.get(collectionId);
        } else {
          String text = "";
          try {
            CollectionImeji c = new CollectionService().retrieve(collectionId, u);
            boolean newFiles = false;
            Collection<Item> createdItems = items.stream()
                .filter(i -> i.getCollection().equals(c.getId()) && i.getCreated().after(yesterday))
                .collect(Collectors.toList());
            for (Item i : createdItems) {
              if (!newFiles) {
                text += "\nNew Files:";
                newFiles = true;
              }
              text += "\n" + getItemText(c, i);
            }
            boolean modifiedFiles = false;
            Collection<Item> modifiedItems = items
                .stream().filter(i -> i.getCollection().equals(c.getId())
                    && !i.getCreated().after(yesterday) && i.getModified().after(yesterday))
                .collect(Collectors.toList());
            for (Item i : modifiedItems) {
              if (!modifiedFiles) {
                text += "\nModifiedFiles Files:";
                modifiedFiles = true;
              }
              text += "\n" + getItemText(c, i);
            }
            if (!"".equals(text)) {
              text = "\n" + getCollectionText(c) + text;
              collectionText.put(collectionId, text);
            }
          } catch (ImejiException e) {
            text = "\n Collection id " + collectionId
                + "\n This collection has either been deleted or you are not allowed to access it anymore. You are automatically unsubscribed.";
            unsubscribe.add(collectionId);
          }
          if (!"".equals(text)) {
            emailText += "\n" + text;
          }
        }
      }
      if (!emailText.equals("")) {
        String subject =
            Imeji.RESOURCE_BUNDLE.getMessage("email_subscribtion_subject", Locale.ENGLISH)
                .replace("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName());
        String body = Imeji.RESOURCE_BUNDLE.getMessage("email_subscribtion_body", Locale.ENGLISH)
            .replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName())
            .replaceAll("XXX_USER_NAME_XXX", u.getPerson().getCompleteName())
            .replace("XXX_TEXT_XXX", emailText);
        new EmailService().sendMail(u.getEmail(), null, subject, body);
      }
      for (String id : unsubscribe) {
        u.unsubscribeFromCollection(id);
        new UserService().update(u, Imeji.adminUser);
      }
    }

    return 1;
  }

  private String getCollectionText(CollectionImeji c) {
    return Imeji.RESOURCE_BUNDLE.getLabel("collection", Locale.ENGLISH) + " " + c.getTitle() + " ("
        + new Navigation().getCollectionUrl() + c.getIdString() + ")";
  }

  private String getItemText(CollectionImeji c, Item i) {
    return "* " + i.getFilename() + " (" + new Navigation().getCollectionUrl() + c.getIdString()
        + "/item/" + i.getIdString() + ")";
  }
}
