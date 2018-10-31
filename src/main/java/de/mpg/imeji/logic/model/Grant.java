package de.mpg.imeji.logic.model;

import java.io.Serializable;
import java.net.URI;

import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * Grant of one {@link GrantType} for one {@link User} used for imeji
 * authorization
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class Grant implements Serializable {
	private static final long serialVersionUID = -6318969286926194883L;

	/**
	 * The types of possible {@link Grant} in imeji
	 *
	 * @author saquet (initial creation)
	 * @author $Author$ (last modification)
	 * @version $Revision$ $LastChangedDate$
	 */
	public enum GrantType {
		READ, EDIT, ADMIN;

		/**
		 * True if the role is the same or bigger than the granttype
		 * 
		 * @param role
		 * @return
		 */
		public boolean isSameOrBigger(GrantType role) {
			switch (this) {
				case READ :
					return true;
				case EDIT :
					return role == EDIT || role == ADMIN;
				case ADMIN :
					return role == ADMIN;
				default :
					return false;
			}
		}

		/**
		 * True if the role is the same or bigger than the granttype
		 * 
		 * @param role
		 * @return
		 */
		public boolean isSameOrBigger(String role) {
			return isSameOrBigger(valueOf(role));
		}
	}

	@j2jResource("http://imeji.org/terms/grantType")
	private String grantType;
	@j2jResource("http://imeji.org/terms/grantFor")
	private String grantFor;

	/**
	 * Constructor: no ids is created with this constructor
	 */
	public Grant() {

	}

	/**
	 * Create a {@link Grant} of type {@link GrantType} for the object with the
	 * {@link URI} grantfor. Define the id
	 *
	 * @param gt
	 * @param gf
	 */
	public Grant(GrantType grantType, String grantFor) {
		this.grantType = grantType.name();
		this.grantFor = grantFor;
	}

	/**
	 * Create a Grant with a String with the format: GrantType,GrantFor
	 *
	 * @param grantString
	 */
	public Grant(String grantString) {
		if (!StringHelper.isNullOrEmptyTrim(grantString)) {
			final String[] s = grantString.split(",");
			if (s.length == 2) {
				this.grantType = s[0];
				this.grantFor = s[1];
			}
		}
	}

	/**
	 * REturn the {@link Grant} as a {@link GrantType}
	 *
	 * @return
	 */
	public GrantType asGrantType() {
		if (grantType != null) {
			return GrantType.valueOf(grantType);
		}
		return null;
	}

	/**
	 * Return a {@link Grant} serialized as s String
	 *
	 * @return
	 */
	public String toGrantString() {
		return grantType + "," + grantFor;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Grant) {
			return grantFor.equals(((Grant) obj).getGrantFor()) && grantType.equals(((Grant) obj).getGrantType());
		}
		return false;
	}

	/**
	 * @return the grantType
	 */
	public String getGrantType() {
		return grantType;
	}

	/**
	 * @param grantType
	 *            the grantType to set
	 */
	public void setGrantType(String grantType) {
		this.grantType = grantType;
	}

	/**
	 * @return the grantFor
	 */
	public String getGrantFor() {
		return grantFor;
	}

	/**
	 * @param grantFor
	 *            the grantFor to set
	 */
	public void setGrantFor(String grantFor) {
		this.grantFor = grantFor;
	}

}
