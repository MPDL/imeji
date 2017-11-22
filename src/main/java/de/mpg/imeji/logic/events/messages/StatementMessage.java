package de.mpg.imeji.logic.events.messages;

import de.mpg.imeji.logic.model.Statement;

/**
 * Message for events related to Statement
 * 
 * @author saquet
 *
 */
public class StatementMessage extends Message {
  private static final long serialVersionUID = -7957725319732095366L;
  private final String previousIndex;
  private final String index;

  public StatementMessage(MessageType type, Statement statement) {
    super(type, statement.getUri());
    this.index = statement.getIndex();
    this.previousIndex = null;
  }


  public StatementMessage(MessageType type, Statement statement, String previousIndex) {
    super(type, statement.getUri());
    this.index = statement.getIndex();
    this.previousIndex = previousIndex;
  }


  /**
   * @return the previousIndex
   */
  public String getPreviousIndex() {
    return previousIndex;
  }


  /**
   * @return the index
   */
  public String getIndex() {
    return index;
  }


}
