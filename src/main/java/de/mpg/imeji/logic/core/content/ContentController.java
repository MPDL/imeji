package de.mpg.imeji.logic.core.content;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.reader.ReaderFacade;
import de.mpg.imeji.logic.db.writer.WriterFacade;
import de.mpg.imeji.logic.generic.ImejiControllerAbstract;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.util.ObjectHelper;

/**
 * Contoller for {@link ContentVO}
 * 
 * @author saquet
 *
 */
class ContentController extends ImejiControllerAbstract<ContentVO> {
	private static final ReaderFacade READER = new ReaderFacade(Imeji.contentModel);
	private static final WriterFacade WRITER = new WriterFacade(Imeji.contentModel);

	@Override
	public List<ContentVO> createBatch(List<ContentVO> l, User user) throws ImejiException {
		l.stream().forEach(c -> c.setId(createID(c)));
		WRITER.create(toObjectList(l), user);
		return l;
	}

	/**
	 * Create the ID of the content with the same value than its item: <br/>
	 * * Item ID: http://imeji.org/item/abc123 <br/>
	 * * Content ID: http://imeji.org/content/abc123
	 * 
	 * @param content
	 * @return
	 */
	private URI createID(ContentVO content) {
		return getContentId(URI.create(content.getItemId()));
	}

	/**
	 * Return the contentId of an item
	 * 
	 * @param itemId
	 * @return
	 */
	public URI getContentId(URI itemId) {
		return ObjectHelper.getURI(ContentVO.class, ObjectHelper.getId(itemId));
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
		l.stream().forEach(c -> c.setId(createID(c)));
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
		return ids.stream().map(id -> ImejiFactory.newContent().setId(id).build()).collect(Collectors.toList());
	}

}
