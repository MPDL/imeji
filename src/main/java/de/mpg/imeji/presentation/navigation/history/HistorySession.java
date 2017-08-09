package de.mpg.imeji.presentation.navigation.history;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

/**
 * JavaBean for the http session object related to the history
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "HistorySession")
@ViewScoped
public class HistorySession implements Serializable {
  private static final long serialVersionUID = 28762010528555885L;
  /**
   * {@link List} of {@link HistoryPage} stored in the history
   */
  private List<HistoryPage> pages = new ArrayList<HistoryPage>();
  /**
   * The maximum count of {@link HistoryPage} stored in the history
   */
  private static int HISTORY_SIZE = 2;

  /**
   * Create new {@link HistorySession}
   */
  public HistorySession() {
    // creates...
  }

  /**
   * Add a {@link HistoryPage} to the history
   *
   * @param page
   * @param id - the ids defined in the url
   */
  public void addPage(HistoryPage page) {
    if (page != null) {
      if (!page.isSame(getCurrentPage())) {
        // If new page, add it to the history
        page.setPos(pages.size());
        pages.add(page);
        removeOldPages();
      } else {
        // If the page is the same, it might be that the params have
        // been changed
        pages.remove(pages.size() - 1);
        page.setPos(pages.size());
        pages.add(page);
      }
    }
  }

  /**
   * Remove {@link HistoryPage} of the history, when the size of the history is greater thant the
   * maximum size
   */
  private void removeOldPages() {
    while (pages.size() > HISTORY_SIZE) {
      pages.remove(0);
    }
  }

  /**
   * Return the current {@link HistoryPage}
   *
   * @return
   */
  public HistoryPage getCurrentPage() {
    if (!pages.isEmpty()) {
      return pages.get(pages.size() - 1);
    }
    return null;
  }

  /**
   * Return the previous {@link HistoryPage} in the history
   *
   * @return
   */
  public HistoryPage getPreviousPage() {
    if (!pages.isEmpty()) {
      return pages.size() > 2 ? pages.get(pages.size() - 2) : pages.get(0);
    }
    return null;
  }
}
