package de.mpg.imeji.j2j.transaction;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.Syntax;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;

import de.mpg.imeji.exceptions.ImejiException;

/**
 * {@link Transaction} for SPARQL update Query
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SPARQLUpdateTransaction extends Transaction {
	private final String query;

	/**
	 * @param modelURI
	 */
	public SPARQLUpdateTransaction(String modelURI, String query) {
		super(modelURI);
		this.query = query;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * de.mpg.j2j.transaction.Transaction#execute(com.hp.hpl.jena.query.Dataset)
	 */
	@Override
	protected void execute(Dataset ds) throws ImejiException {
		final UpdateRequest request = UpdateFactory.create(query, Syntax.syntaxARQ);
		UpdateAction.execute(request, ds);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.mpg.j2j.transaction.Transaction#getLockType()
	 */
	@Override
	protected ReadWrite getLockType() {
		return ReadWrite.WRITE;
	}
}
