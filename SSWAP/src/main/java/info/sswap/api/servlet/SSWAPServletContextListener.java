/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.servlet;

import info.sswap.api.model.Config;

import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Responds to events like initialization/destruction of the servlet context.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class SSWAPServletContextListener implements ServletContextListener {
	
	public static final String CONFIG_PATH_PARAM = "ConfigPath";

	@Override
    public void contextDestroyed(ServletContextEvent ev) {
		if (RRGCache.get().isActive()) {
			RRGCache.get().shutdown();
		}
	    
		if (ContentCache.get().isActive()) {
			ContentCache.get().shutdown();
		}
	}

	@Override
    public void contextInitialized(ServletContextEvent ev) {
		String configPath = ev.getServletContext().getInitParameter(CONFIG_PATH_PARAM);
		
		if (configPath != null) {
			try {
				FileInputStream fis = new FileInputStream(ev.getServletContext().getRealPath(configPath));
				Config.get().load(fis);
				fis.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}		
    }
}
