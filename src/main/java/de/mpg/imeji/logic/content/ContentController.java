package de.mpg.imeji.logic.content;

import java.util.List;
import java.util.stream.Collectors;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.db.reader.ReaderFacade;
import de.mpg.imeji.logic.db.writer.WriterFacade;
import de.mpg.imeji.logic.service.ImejiControllerAbstract;
import de.mpg.imeji.logic.util.IdentifierUtil;
import de.mpg.imeji.logic.vo.ContentVO;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;

public class ContentController extends ImejiControllerAbstract<ContentVO> {
  private static final ReaderFacade READER = new ReaderFacade(Imeji.contentModel);
  private static final WriterFacade WRITER = new WriterFacade(Imeji.contentModel);

  @Override
  public List<ContentVO> createBatch(List<ContentVO> l, User user) throws ImejiException {
    l.stream().forEach(c -> c.setId(IdentifierUtil.newURI(ContentVO.class)));
    WRITER.create(toObjectList(l), user);
    return l;
  }

  @Override
  public List<ContentVO> retrieveBatch(List<String> ids, User user) throws ImejiException {
    List<ContentVO> contents = initializeEmptyList(ids);
    READER.read(toObjectList(contents), Imeji.adminUser);
    return contents;
  }

  @Override
  public List<ContentVO> retrieveBatchLazy(List<String> ids, User user) throws ImejiException {
    List<ContentVO> contents = initializeEmptyList(ids);
    READER.readLazy(toObjectList(contents), Imeji.adminUser);
    return contents;
  }

  @Override
  public List<ContentVO> updateBatch(List<ContentVO> l, User user) throws ImejiException {
    WRITER.update(toObjectList(l), user, true);
    return l;
  }

  @Override
  public void deleteBatch(List<ContentVO> l, User user) throws ImejiException {
    WRITER.delete(toObjectList(l), user);
  }

  /**
   * Initialize a list of empty content
   * 
   * @param ids
   * @return
   */
  private List<ContentVO> initializeEmptyList(List<String> ids) {
    return ids.stream().map(id -> ImejiFactory.newContent().setId(id).build())
        .collect(Collectors.toList());
  }

}
