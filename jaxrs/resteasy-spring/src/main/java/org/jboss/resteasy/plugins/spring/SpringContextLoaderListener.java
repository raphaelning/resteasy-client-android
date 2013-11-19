package org.jboss.resteasy.plugins.spring;

import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;

import javax.servlet.ServletContextEvent;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SpringContextLoaderListener extends ContextLoaderListener
{
   @Override
   public void contextInitialized(ServletContextEvent event)
   {
      boolean scanProviders = false;
      boolean scanResources = false;

      String sProviders = event.getServletContext().getInitParameter("resteasy.scan.providers");
      if (sProviders != null)
      {
         scanProviders = Boolean.valueOf(sProviders.trim());
      }
      String scanAll = event.getServletContext().getInitParameter("resteasy.scan");
      if (scanAll != null)
      {
         boolean tmp = Boolean.valueOf(scanAll.trim());
         scanProviders = tmp || scanProviders;
         scanResources = tmp || scanResources;
      }
      String sResources = event.getServletContext().getInitParameter("resteasy.scan.resources");
      if (sResources != null)
      {
         scanResources = Boolean.valueOf(sResources.trim());
      }

      if (scanProviders || scanResources)
      {
         throw new RuntimeException("You cannot use resteasy.scan, resteasy.scan.resources, or resteasy.scan.providers with the SpringContextLoaderLister as this may cause serious deployment errors in your application");
      }


      super.contextInitialized(event);
   }

   protected ContextLoader createContextLoader()
   {
      return new SpringContextLoader();
   }
}
