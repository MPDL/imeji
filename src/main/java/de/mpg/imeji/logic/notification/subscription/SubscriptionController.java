package de.mpg.imeji.logic.notification.subscription;

import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.j2j.helper.J2JHelper;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.reader.ReaderFacade;
import de.mpg.imeji.logic.db.writer.WriterFacade;
import de.mpg.imeji.logic.generic.ImejiControllerAbstract;
import de.mpg.imeji.logic.model.Subscription;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.SubscriptionFactory;
import de.mpg.imeji.logic.security.authorization.util.SecurityUtil;

/**
 * Controller for {@link Subscription}
 * 
 * @author saquet
 *
 */
public class SubscriptionController extends ImejiControllerAbstract<Subscription>
    implements Serializable {
  private static final long serialVersionUID = 4481379886807835574L;
  private static final ReaderFacade READER = new ReaderFacade(Imeji.userModel);
  private static final WriterFacade WRITER = new WriterFacade(Imeji.userModel);

  @Override
  public List<Subscription> createBatch(List<Subscription> l, User user) throws ImejiException {
    WRITER.create(J2JHelper.cast2ObjectList(l), user);
    return l;
  }

  @Override
  public List<Subscription> retrieveBatch(List<String> ids, User user) throws ImejiException {
    List<Subscription> subscriptions = emtyListFactory(ids);
    READER.read(J2JHelper.cast2ObjectList(subscriptions), Imeji.adminUser);
    return subscriptions.stream().filter(s -> SecurityUtil.authorization().read(user, s))
        .collect(Collectors.toList());
  }

  @Override
  public List<Subscription> retrieveBatchLazy(List<String> ids, User user) throws ImejiException {
    return retrieveBatch(ids, user);
  }

  @Override
  public List<Subscription> updateBatch(List<Subscription> l, User user) throws ImejiException {
    WRITER.update(J2JHelper.cast2ObjectList(l), user, false);
    return l;
  }

  @Override
  public void deleteBatch(List<Subscription> l, User user) throws ImejiException {
    WRITER.delete(J2JHelper.cast2ObjectList(l), user);
  }

  /**
   * Build a list of empty Subscription with ids
   * 
   * @param ids
   * @return
   */
  private List<Subscription> emtyListFactory(List<String> ids) {
    return ids.stream().map(id -> new SubscriptionFactory().setId(URI.create(id)).build())
        .collect(Collectors.toList());
  }

}
