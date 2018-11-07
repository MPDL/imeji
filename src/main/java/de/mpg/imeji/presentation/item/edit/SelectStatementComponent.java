package de.mpg.imeji.presentation.item.edit;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.faces.model.SelectItem;

import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.StatementType;
import de.mpg.imeji.logic.model.util.StatementUtil;
import de.mpg.imeji.presentation.statement.StatementForm;

/**
 * HTML Component for Statement
 *
 * @author saquet
 *
 */
public class SelectStatementComponent implements Serializable {
	private static final long serialVersionUID = 2521052242334769127L;
	private String index;
	private Statement statement;
	private final Map<String, Statement> statementMap;
	private StatementForm statementForm = new StatementForm();

	public SelectStatementComponent(Map<String, Statement> statementMap) {
		this.statementMap = statementMap;
		this.index = null;
	}

	public SelectStatementComponent(String index, Map<String, Statement> statementMap) {
		this(statementMap);
		init(index);
	}

	/**
	 * Initialize the component
	 *
	 * @param index
	 */
	public void init(String index) {
		this.index = index;
		statement = statementMap.get(index);
		statementForm.setName(index);
	}

	public List<String> searchForIndex(List<SelectItem> statementMenu) {
		index = index == null ? "" : index;
		return statementMenu.stream().map(i -> i.getValue().toString())
				.filter(s -> StatementUtil.formatIndex(s).contains(StatementUtil.formatIndex(index.toLowerCase())))
				.sorted((s1, s2) -> s1.toLowerCase().compareTo(s2.toLowerCase())).collect(Collectors.toList());
	}

	/**
	 * Remove the first ":" form the containerId to reuse it in javascript methods
	 * 
	 * @param containerId
	 * @return
	 */
	public String normalizeContainerId(String containerId) {
		return containerId.startsWith(":") ? containerId.substring(1) : containerId;
	}

	public boolean indexExists() {
		return statementMap.values().stream().filter(i -> StatementUtil.indexEquals(i.getIndex(), index)).findAny()
				.isPresent();
	}

	public void reset() {
		this.index = null;
		this.statement = null;
	}

	/**
	 * Listener when the value of the component has been changed
	 */
	public void listener() {
		init(index);
	}

	/**
	 * @return the index
	 */
	public String getIndex() {
		return index;
	}

	/**
	 * @param index
	 *            the index to set
	 */
	public void setIndex(String index) {
		this.index = index;
	}

	/**
	 * @return the exists
	 */
	public boolean isExists() {
		return statement != null;
	}

	public List<StatementType> getTypes() {
		return Arrays.asList(StatementType.values());
	}

	/**
	 * @return the statementForm
	 */
	public StatementForm getStatementForm() {
		return statementForm;
	}

	/**
	 * @param statementForm
	 *            the statementForm to set
	 */
	public void setStatementForm(StatementForm statementForm) {
		this.statementForm = statementForm;
	}

	public Statement asStatement() {
		return statementMap.containsKey(index) ? statementMap.get(index) : statementForm.asStatement();
	}

	/**
	 * @return the statement
	 */
	public Statement getStatement() {
		return statement;
	}

	/**
	 * @param statement
	 *            the statement to set
	 */
	public void setStatement(Statement statement) {
		this.statement = statement;
	}

}
