package de.mpg.imeji.presentation.session;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.sun.faces.application.view.ViewScopeManager;

/**
 * Workaround to limit session to grow to much. Related to known issue:
 * https://java.net/jira/browse/JAVASERVERFACES-4015 <br/>
 * Should be removed (the class and the definition in web.xml) when fixed
 * (probably with mojarra 2.3)
 *
 * @author bastiens
 *
 */
public class SetActiveViewMapsSizeSessionListener implements HttpSessionListener {
	@Override
	public void sessionCreated(HttpSessionEvent event) {
		event.getSession().setAttribute(ViewScopeManager.ACTIVE_VIEW_MAPS_SIZE, 5);
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent arg0) {
		// TODO Auto-generated method stub

	}
}
