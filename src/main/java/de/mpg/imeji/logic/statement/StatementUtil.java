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
    return Arrays.asList(str.split(",")).stream().map(s -> toUri(s)).collect(Collectors.toList());
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
    return l.stream()
        .collect(Collectors.toMap(Statement::getIndex, Function.identity(), (s1, s2) -> s1));
  }

  /**
   * Format an index to store it
   * 
   * @param index
   * @return
   */
  public static String formatIndex(String index) {
    return index != null ? new String(index.trim().replace(" ", "_").toLowerCase()) : "";
  }

  /**
   * True if 2 index are equals
   * 
   * @param index1
   * @param index2
   * @return
   */
  public static boolean indexEquals(String index1, String index2) {
    return formatIndex(index1).equals(formatIndex(index2));
  }

  /**
   * Return an index as an uri
   * 
   * @param index
   * @return
   */
  public static String toUri(String index) {
    return ObjectHelper.getURI(Statement.class, formatIndex(index)).toString();
  }
}
