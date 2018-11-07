package de.mpg.imeji.j2j.transaction;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;

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
