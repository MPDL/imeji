package de.mpg.imeji.logic.core.item.messaging;

import java.util.List;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.events.Message.MessageType;
import de.mpg.imeji.logic.events.subscription.Subscriber;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.util.StatementUtil;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;

/**
 * Triggered when a statement is changed
 * 
 * @author saquet
 *
 */
public class StatementChangedSubscriber extends Subscriber {

  public StatementChangedSubscriber() {
    super(MessageType.STATEMENT_CHANGED);
  }

  @Override
  public Integer call() throws Exception {
    String oldIndex = getMessage().getObjectId();
    String newIndex = getMessage().getContent().get("newIndex");
    updateItemIndex(oldIndex, newIndex, Imeji.adminUser);
    return null;
  }

  /**
   * Update all item using "before" with "after"
   * 
   * @param before
   * @param after
   * @param user
   * @throws ImejiException
   */
  private void updateItemIndex(String oldIndex, String newIndex, User user) throws ImejiException {
    ItemService itemService = new ItemService();
    SearchQuery q = new SearchFactory()
        .addElement(new SearchPair(SearchFields.index, StatementUtil.formatIndex(oldIndex)),
            LOGICAL_RELATIONS.AND)
        .build();
    List<Item> items = itemService.searchAndRetrieve(q, null, user, -1, 0);
    items.stream().flatMap(item -> item.getMetadata().stream())
        .filter(md -> StatementUtil.indexEquals(oldIndex, md.getIndex()))
        .forEach(md -> md.setIndex(newIndex));
    itemService.updateBatch(items, user);
  }
}
