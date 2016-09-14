/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.history;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import de.mpg.imeji.presentation.util.ServletUtil;

/**
 * JavaBean for the http session object related to the history
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class HistorySession {
  /**
   * {@link List} of {@link HistoryPage} stored in the history
   */
  private List<HistoryPage> pages = new ArrayList<HistoryPage>();
  /**
   * The maximum count of {@link HistoryPage} stored in the history
   */
  private static int HISTORY_SIZE = 10;

  /**
   * Create new {@link HistorySession}
   */
  public HistorySession() {}

  /**
   * Return the {@link HistorySession}
   *
   * @param req
   * @return
   */
  public static HistorySession getHistorySession(HttpServletRequest req) {
    return (HistorySession) ServletUtil.getSession(req, HistorySession.class.getSimpleName());
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
        if (!page.getParams().containsKey("edituploaded")) {
          getCurrentPage().setParams(page.getParams());
        }
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
   * Remove a {@link HistoryPage} of the history according to its position in the history
   *
   * @param pos
   */
  public void remove(int pos) {
    for (int i = 0; i < pages.size(); i++) {
      if (i > pos) {
        pages.remove(i);
        i--;
      }
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

  /**
   * Return the size of the history
   *
   * @return
   */
  public int getHistorySize() {
    return pages.size();
  }

  /**
   * Getter- Return the {@link List} of {@link HistoryPage} of the history
   *
   * @return
   */
  public List<HistoryPage> getPages() {
    return pages;
  }

  /**
   * setter
   *
   * @param pages
   */
  public void setPages(List<HistoryPage> pages) {
    this.pages = pages;
  }
}
