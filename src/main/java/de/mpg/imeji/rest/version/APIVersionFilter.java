package de.mpg.imeji.rest.version;

import static de.mpg.imeji.logic.util.StringHelper.isNullOrEmptyTrim;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import de.mpg.imeji.rest.version.exception.DeprecatedAPIVersionException;
import de.mpg.imeji.rest.version.exception.UnknowAPIVersionException;

/**
 * Filter which check if the version of the request to the API is the latest
 * one. <br/>
 * - If yes, redirect to the API without version number (latest) <br/>
 * - If no, send an not supported version exception back
 *
 * @author saquet
 *
 */
@WebFilter(urlPatterns = "/rest/*", asyncSupported = true)
public class APIVersionFilter implements Filter {
	private VersionManager versionManager;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		versionManager = new VersionManager();
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		try {
			versionManager.checkVersion(((HttpServletRequest) request).getRequestURI());
			if (versionManager.isCurrentVersion() && versionManager.hasVersion()) {
				// redirect to non latest api (i.e. without version in the url)
				final String q = ((HttpServletRequest) request).getQueryString();
				((HttpServletResponse) response)
						.sendRedirect(versionManager.getPathToLatestVersion() + (isNullOrEmptyTrim(q) ? "" : "?" + q));
			}
		} catch (final DeprecatedAPIVersionException e) {
			((HttpServletResponse) response).sendError(Status.GONE.getStatusCode(), e.getMessage());
		} catch (final UnknowAPIVersionException e) {
			((HttpServletResponse) response).sendError(Status.BAD_REQUEST.getStatusCode(), e.getMessage());
		} finally {
			chain.doFilter(request, response);
		}

	}

	@Override
	public void destroy() {
	}

}
