/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.apache.log4j.Logger;

/**
 * With this listener, it can be avoided that Faces Messages are lost when doing an redirect instead
 * of applying a navigation rule
 * 
 * @author Markus Haarlaender (initial creation)
 * @author $Author: mfranke $ (last modification)
 * @version $Revision: 1891 $ $LastChangedDate: 2008-12-23 11:13:59 +0100 (Di, 23 Dez 2008) $
 */
public class FacesMessagesPhaseListener implements PhaseListener {
  private static final Logger LOGGER = Logger.getLogger(FacesMessagesPhaseListener.class);
  private static final String sessionToken = "REDIRECT_MESSAGES_SUPPORT";

  // private Map<String, Collection<FacesMessage>> messageCache = Collections.synchronizedMap(new
  // HashMap<String,
  // Collection<FacesMessage>>());
  /**
   * Caches Faces Messages after the Invoke Application phase and clears the cache after the Render
   * Response phase.
   */
  @Override
  public synchronized void afterPhase(PhaseEvent event) {
    LOGGER.trace(event.getPhaseId().toString() + " - After Phase");
    if (event.getPhaseId() == PhaseId.INVOKE_APPLICATION) {
      cacheMessages(event.getFacesContext());
    } else if (event.getPhaseId() == PhaseId.RENDER_RESPONSE) {
      removeFromCache(event.getFacesContext());
    }
  }

  /**
   * Restores the messages from the cache before the Restore View phase.
   */
  @Override
  public synchronized void beforePhase(PhaseEvent event) {
    LOGGER.trace(event.getPhaseId().toString() + " - Before Phase");
    if (event.getPhaseId() == PhaseId.RESTORE_VIEW) {
      restoreMessages(event.getFacesContext());
    }
  }

  /**
   * Clears the whole cache
   * 
   * @param context
   */
  private void removeFromCache(FacesContext context) {
    getMessageCache(context).clear();
    LOGGER.trace("Message Cache cleared");
  }

  /**
   * Caches messages from current faces context to a session object
   * 
   * @param context
   * @return
   */
  private int cacheMessages(FacesContext context) {
    int cachedCount = 0;
    Iterator<String> clientIdsWithMessages = context.getClientIdsWithMessages();
    while (clientIdsWithMessages.hasNext()) {
      String clientId = clientIdsWithMessages.next();
      Iterator<FacesMessage> iterator = context.getMessages(clientId);
      Collection<FacesMessage> cachedMessages = getMessageCache(context).get(clientId);
      if (cachedMessages == null) {
        // cachedMessages = new TreeSet<FacesMessage>(new FacesMessageComparator());
        cachedMessages = new ArrayList<FacesMessage>();
        getMessageCache(context).put(clientId, cachedMessages);
      }
      while (iterator.hasNext()) {
        FacesMessage facesMessage = iterator.next();
        if (cachedMessages.add(facesMessage)) {
          cachedCount++;
        }
      }
    }
    LOGGER.trace("Saved " + cachedCount + " messages in cache");
    return cachedCount;
  }

  /**
   * Restores messages from session to faces context
   * 
   * @param context
   */
  private void restoreMessages(FacesContext context) {
    if (!getMessageCache(context).isEmpty()) {
      for (String clientId : getMessageCache(context).keySet()) {
        for (FacesMessage message : getMessageCache(context).get(clientId)) {
          context.addMessage(clientId, message);
        }
      }
      LOGGER.trace("Restored Messages from Cache");
    }
  }

  @Override
  public PhaseId getPhaseId() {
    return PhaseId.ANY_PHASE;
  }

  private Map<String, Collection<FacesMessage>> getMessageCache(FacesContext context) {
    if (context.getExternalContext().getSessionMap().get(sessionToken) != null) {
      return (Map<String, Collection<FacesMessage>>) context.getExternalContext().getSessionMap()
          .get(sessionToken);
    } else {
      Map<String, Collection<FacesMessage>> messageCache =
          Collections.synchronizedMap(new HashMap<String, Collection<FacesMessage>>());
      context.getExternalContext().getSessionMap().put(sessionToken, messageCache);
      return messageCache;
    }
  }
}
