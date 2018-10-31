package de.mpg.imeji.presentation.util;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletResponse;

public class CacheControlPhaseListener implements PhaseListener {
	private static final long serialVersionUID = 4651265325754141047L;

	@Override
	public PhaseId getPhaseId() {
		return PhaseId.RENDER_RESPONSE;
	}

	@Override
	public void afterPhase(PhaseEvent event) {
	}

	@Override
	public void beforePhase(PhaseEvent event) {
		final FacesContext facesContext = event.getFacesContext();
		final HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
		// Set standard HTTP/1.1 no-cache headers.
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		// Set standard HTTP/1.0 no-cache header.
		response.setHeader("Pragma", "no-cache");
		// Proxies
		response.setDateHeader("Expires", 0);
	}
}
