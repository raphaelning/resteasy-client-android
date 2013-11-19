package org.jboss.resteasy.plugins.server.servlet;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * This is a ServletContextListener that creates the registry for resteasy and stuffs it as a servlet context attribute
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResteasyBootstrap implements ServletContextListener
{
   protected ResteasyDeployment deployment;

   public void contextInitialized(ServletContextEvent event)
   {
      ServletContext servletContext = event.getServletContext();

      ListenerBootstrap config = new ListenerBootstrap(event.getServletContext());
      deployment = config.createDeployment();
      deployment.start();

      servletContext.setAttribute(ResteasyProviderFactory.class.getName(), deployment.getProviderFactory());
      servletContext.setAttribute(Dispatcher.class.getName(), deployment.getDispatcher());
      servletContext.setAttribute(Registry.class.getName(), deployment.getRegistry());
   }

   public void contextDestroyed(ServletContextEvent event)
   {
      deployment.stop();
   }

}
