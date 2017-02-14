package de.mpg.imeji.logic.statement;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.Statement;

/**
 * Utility class for {@link Statement}
 * 
 * @author saquet
 *
 */
public class StatementUtil {

  private StatementUtil() {
    // avoid constructor
  }

  /**
   * Transform a String of {@link Statement} names (ex: title,Description) into a {@link List} of
   * {@link Statement} ids. <br/>
   * IMPORTANT: This method doens't check if the statements exists!
   * 
   * @param str
   * @return
   */
  public static List<String> toStatementUriList(String str) {
    return Arrays.asList(str.split(",")).stream()
        .map(s -> ObjectHelper.getURI(Statement.class, s.toLowerCase()).toString())
        .collect(Collectors.toList());
  }

  /**
   * Return a list of statement as a String of all their names separated by a comma
   * 
   * @param statements
   * @return
   */
  public static String toStatementNamesString(List<Statement> statements) {
    return statements.stream().map(s -> ObjectHelper.getId(s.getUri()))
        .collect(Collectors.joining(","));
  }


  /**
   * Transform a list of statement to a map of statement with its id as key
   * 
   * @param l
   * @return
   */
  public static Map<String, Statement> statementListToMap(List<Statement> l) {
    return l.stream().collect(Collectors.toMap(Statement::getIndex, Function.identity()));
  }
}
