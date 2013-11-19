package org.jboss.resteasy.plugins.spring;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import javax.servlet.ServletContext;

/**
 * Provides access to RESTEasy's SpringContextLoader implementation without having
 * to extend {@link org.springframework.web.context.ContextLoader}.  This is useful
 * if you have your own SpringContextLoaderListener and dont' want to 
 * return RESTEasy's {@link SpringContextLoader}.
 *
 * Usage:
 * <pre>
 * public class MyCustomSpringContextLoader extends ContextLoader 
 * {
 *    private SpringContextLoaderSupport springContextLoaderSupport = 
 *       new SpringContextLoaderSupport();
 *
 *     protected void customizeContext(
 *        ServletContext servletContext, 
 *        ConfigurableWebApplicationContext configurableWebApplicationContext)
 *    {
 *       super.customizeContext(servletContext, configurableWebApplicationContext);
 *
 *       // Your custom code 
 *
 *       this.springContextLoaderSupport.customizeContext(servletContext.configurableWebApplicationContext);
 *
 *       // Your custom code 
 *    }
 * }
 * </pre>
 */
public class SpringContextLoaderSupport 
{
   public void customizeContext(ServletContext servletContext, ConfigurableWebApplicationContext configurableWebApplicationContext)
   {
      ResteasyProviderFactory providerFactory = (ResteasyProviderFactory) servletContext.getAttribute(ResteasyProviderFactory.class.getName());
      if (providerFactory == null)
         throw new RuntimeException("RESTeasy Provider Factory is null, do you have the ResteasyBootstrap listener configured?");

      Registry registry = (Registry) servletContext.getAttribute(Registry.class.getName());
      if (registry == null)
         throw new RuntimeException("RESTeasy Registry is null, do ou have the ResteasyBootstrap listener configured?");

      Dispatcher dispatcher = (Dispatcher) servletContext.getAttribute(Dispatcher.class.getName());
      if (dispatcher == null)
         throw new RuntimeException("RESTeasy Dispatcher is null, do ou have the ResteasyBootstrap listener configured?");

      SpringBeanProcessor processor = new SpringBeanProcessor(dispatcher, registry, providerFactory);
      configurableWebApplicationContext.addBeanFactoryPostProcessor(processor);
      configurableWebApplicationContext.addApplicationListener(processor);
   }
}
