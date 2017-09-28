package de.mpg.imeji.logic.messaging.subscription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.reflections.Reflections;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.messaging.Message;
import de.mpg.imeji.logic.messaging.Message.MessageType;

/**
 * Service managing message subscription
 * 
 * @author saquet
 *
 */
public class SubscriptionService {
  private static Map<MessageType, List<Subscriber>> subscriptions = new HashMap<>();
  private static Logger LOGGER = Logger.getLogger(SubscriptionService.class);

  /**
   * Initialize the {@link SubscriptionService} by registering all existing {@link Subscriber}
   */
  public void init() {
    LOGGER.info("Initializing message subscribers...");
    for (Subscriber s : findAllSubscribers()) {
      register(s);
      LOGGER.info("Registered: " + s.getClass().getName());
    }
    LOGGER.info("Initializing message subscribers done!");
  }

  /**
   * Subscribe to a particular topic
   * 
   * @param subscriber
   */
  public void register(Subscriber subscriber) {
    List<Subscriber> l = subscriptions.getOrDefault(subscriber.getMessageType(), new ArrayList<>());
    l.add(subscriber);
    for (MessageType m : subscriber.getMessageType()) {
      subscriptions.put(m, l);
    }
  }

  /**
   * Call all subscriber to this message
   * 
   * @param message
   */
  public void notifySubscribers(Message message) {
    subscriptions.getOrDefault(message.getType(), new ArrayList<>()).stream()
        .peek(s -> s.send(message)).forEach(s -> Imeji.getEXECUTOR().submit(s));
  }

  /**
   * Find all Classes extending {@link Subscriber} and return an instance of it
   * 
   * @return
   */
  private List<Subscriber> findAllSubscribers() {
    Reflections reflections = new Reflections("de.mpg.imeji");
    Set<Class<? extends Subscriber>> subscriberClasses =
        reflections.getSubTypesOf(Subscriber.class);
    List<Subscriber> l = new ArrayList<>();
    for (Class<? extends Subscriber> c : subscriberClasses) {
      try {
        l.add(c.newInstance());
      } catch (InstantiationException | IllegalAccessException e) {
        LOGGER.error("Error instancing " + c, e);
      }
    }
    return l;
  }

}
