package de.mpg.imeji.logic.core.collection;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.writer.JenaWriter;
import de.mpg.imeji.logic.events.listener.Listener;
import de.mpg.imeji.logic.events.messages.Message.MessageType;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.aspects.AccessMember.ActionType;
import de.mpg.imeji.logic.model.aspects.AccessMember.ChangeMember;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.util.ObjectHelper;

/**
 * Gets notified when a collection has been removed. Searches database for links to this collection
 * (linked collections) and deletes them.
 * 
 * @author breddin
 *
 */
public class RemoveLinkedCollectionsListener extends Listener {


  public RemoveLinkedCollectionsListener() {
    super(MessageType.DISCARD_COLLECTION);
  }

  @Override
  public Integer call() throws Exception {

    /*
    String idOfRemovedCollection = getMessage().getObjectId();
    URI uriOfRemovedCollection = ObjectHelper.getURI(CollectionImeji.class, idOfRemovedCollection);
    // get list of collections whose linked collections list contains the removed collection
    String sparqlQuery = JenaCustomQueries.findAllCollectionLinks(uriOfRemovedCollection.toString());
    List<String> urisOfLinkedCollections = ImejiSPARQL.exec(sparqlQuery, Imeji.collectionModel);
    List<ChangeMember> removeLinkedCollectionRequests = new ArrayList<ChangeMember>(urisOfLinkedCollections.size());
    String uri;
    ChangeMember changeMember;
    CollectionImeji collection;
    Field linkedCollectionsField = CollectionImeji.class.getDeclaredField("linkedCollections");
    for (String listUri : urisOfLinkedCollections) {
    // Linked collections are stored in a list. Extract uri of collection that owns the list:
    // query gives: "http://imeji.org/collection/YhDZ8gf_TGKsUD_S//linkedCollection@pos0", 
    // we need "http://imeji.org/collection/YhDZ8gf_TGKsUD_S"
    int separationIndex = listUri.indexOf("\\/\\/");
    uri = listUri.substring(0, separationIndex);
    collection = new CollectionImeji();
    collection.setId(URI.create(uri));
    changeMember = new ChangeMember(ActionType.REMOVE, collection, linkedCollectionsField, uriOfRemovedCollection);
    removeLinkedCollectionRequests.add(changeMember);
    }
    // we only need to do this in Jena, as linked collections only exist in Jena
    JenaWriter jenaWriter = new JenaWriter(Imeji.collectionModel);
    jenaWriter.editElements(removeLinkedCollectionRequests);
    */

    return 1;
  }



}
