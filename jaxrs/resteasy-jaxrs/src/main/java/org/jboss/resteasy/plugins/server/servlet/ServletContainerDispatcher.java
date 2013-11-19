package org.jboss.resteasy.plugins.server.servlet;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.core.ThreadLocalResteasyProviderFactory;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.jboss.resteasy.util.GetRestful;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Helper/delegate class to unify Servlet and Filter dispatcher implementations
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletContainerDispatcher
{
   protected Dispatcher dispatcher;
   protected ResteasyProviderFactory providerFactory;
   private final static Logger logger = Logger.getLogger(ServletContainerDispatcher.class);
   private String servletMappingPrefix = "";
   protected ResteasyDeployment deployment = null;
   protected HttpRequestFactory requestFactory;
   protected HttpResponseFactory responseFactory;

   public Dispatcher getDispatcher()
   {
      return dispatcher;
   }


   public void init(ServletContext servletContext, ConfigurationBootstrap bootstrap, HttpRequestFactory requestFactory, HttpResponseFactory responseFactory) throws ServletException
   {
      this.requestFactory = requestFactory;
      this.responseFactory = responseFactory;
      ResteasyProviderFactory globalFactory = (ResteasyProviderFactory) servletContext.getAttribute(ResteasyProviderFactory.class.getName());
      Dispatcher globalDispatcher = (Dispatcher) servletContext.getAttribute(Dispatcher.class.getName());

      String application = bootstrap.getInitParameter("javax.ws.rs.Application");
      String useGlobalStr = bootstrap.getInitParameter("resteasy.servlet.context.deployment");
      boolean useGlobal = globalFactory != null;
      if (useGlobalStr != null) useGlobal = Boolean.parseBoolean(useGlobalStr);

      // use global is backward compatible with 2.3.x and earlier and will store and/or use the dispatcher and provider factory
      // in the servlet context
      if (useGlobal)
      {
         providerFactory = globalFactory;
         dispatcher = globalDispatcher;
         if ((providerFactory != null && dispatcher == null) || (providerFactory == null && dispatcher != null))
         {
            throw new ServletException("Unknown state.  You have a Listener messing up what resteasy expects");
         }
         // We haven't been initialized by an external entity so bootstrap ourselves
         if (providerFactory == null)
         {
            deployment = bootstrap.createDeployment();
            deployment.start();

            servletContext.setAttribute(ResteasyProviderFactory.class.getName(), deployment.getProviderFactory());
            servletContext.setAttribute(Dispatcher.class.getName(), deployment.getDispatcher());
            servletContext.setAttribute(Registry.class.getName(), deployment.getRegistry());

            dispatcher = deployment.getDispatcher();
            providerFactory = deployment.getProviderFactory();

         }
         else
         {
            // ResteasyBootstrap inited us.  Check to see if the servlet defines an Application class
            if (application != null)
            {
               try
               {
                  Map contextDataMap = ResteasyProviderFactory.getContextDataMap();
                  contextDataMap.putAll(dispatcher.getDefaultContextObjects());
                  Application app = ResteasyDeployment.createApplication(application.trim(), dispatcher, providerFactory);
                  // push context data so we can inject it
                  processApplication(app);
               }
               finally
               {
                  ResteasyProviderFactory.removeContextDataLevel();
               }
            }
         }
         servletMappingPrefix = bootstrap.getParameter(ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX);
         if (servletMappingPrefix == null) servletMappingPrefix = "";
         servletMappingPrefix = servletMappingPrefix.trim();
      }
      else
      {
         deployment = bootstrap.createDeployment();
         deployment.start();
         dispatcher = deployment.getDispatcher();
         providerFactory = deployment.getProviderFactory();

         servletMappingPrefix = bootstrap.getParameter(ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX);
         if (servletMappingPrefix == null) servletMappingPrefix = "";
         servletMappingPrefix = servletMappingPrefix.trim();
      }
   }

   public void destroy()
   {
      if (deployment != null)
      {
         deployment.stop();
      }
   }

   protected void processApplication(Application config)
   {
      logger.info("Deploying " + Application.class.getName() + ": " + config.getClass());
      ArrayList<Class> actualResourceClasses = new ArrayList<Class>();
      ArrayList<Class> actualProviderClasses = new ArrayList<Class>();
      ArrayList resources = new ArrayList();
      ArrayList providers = new ArrayList();
      if (config.getClasses() != null)
      {
         for (Class clazz : config.getClasses())
         {
            if (GetRestful.isRootResource(clazz))
            {
               logger.info("Adding class resource " + clazz.getName() + " from Application " + config.getClass());
               actualResourceClasses.add(clazz);
            }
            else
            {
               logger.info("Adding provider class " + clazz.getName() + " from Application " + config.getClass());
               actualProviderClasses.add(clazz);
            }
         }
      }
      if (config.getSingletons() != null)
      {
         for (Object obj : config.getSingletons())
         {
            if (GetRestful.isRootResource(obj.getClass()))
            {
               logger.info("Adding singleton resource " + obj.getClass().getName() + " from Application " + config.getClass());
               resources.add(obj);
            }
            else
            {
               logger.info("Adding singleton provider " + obj.getClass().getName() + " from Application " + config.getClass());
               providers.add(obj);
            }
         }
      }
      for (Class clazz : actualProviderClasses) providerFactory.registerProvider(clazz);
      for (Object obj : providers) providerFactory.registerProviderInstance(obj);
      for (Class clazz : actualResourceClasses) dispatcher.getRegistry().addPerRequestResource(clazz);
      for (Object obj : resources) dispatcher.getRegistry().addSingletonResource(obj);
   }


   public void setDispatcher(Dispatcher dispatcher)
   {
      this.dispatcher = dispatcher;
   }

   public void service(String httpMethod, HttpServletRequest request, HttpServletResponse response, boolean handleNotFound) throws IOException, NotFoundException
   {
      try
      {
         //logger.info(httpMethod + " " + request.getRequestURL().toString());
         //logger.info("***PATH: " + request.getRequestURL());
         // classloader/deployment aware RestasyProviderFactory.  Used to have request specific
         // ResteasyProviderFactory.getInstance()
         ResteasyProviderFactory defaultInstance = ResteasyProviderFactory.getInstance();
         if (defaultInstance instanceof ThreadLocalResteasyProviderFactory)
         {
            ThreadLocalResteasyProviderFactory.push(providerFactory);
         }
         ResteasyHttpHeaders headers = null;
         ResteasyUriInfo uriInfo = null;
         try
         {
            headers = ServletUtil.extractHttpHeaders(request);
            uriInfo = ServletUtil.extractUriInfo(request, servletMappingPrefix);
         }
         catch (Exception e)
         {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            // made it warn so that people can filter this.
            logger.warn("Failed to parse request.", e);
            return;
         }

         HttpResponse theResponse = responseFactory.createResteasyHttpResponse(response);
         HttpRequest in = requestFactory.createResteasyHttpRequest(httpMethod, request, headers, uriInfo, theResponse, response);

         try
         {
            ResteasyProviderFactory.pushContext(HttpServletRequest.class, request);
            ResteasyProviderFactory.pushContext(HttpServletResponse.class, response);

            ResteasyProviderFactory.pushContext(SecurityContext.class, new ServletSecurityContext(request));
            if (handleNotFound)
            {
               dispatcher.invoke(in, theResponse);
            }
            else
            {
               ((SynchronousDispatcher) dispatcher).invokePropagateNotFound(in, theResponse);
            }
         }
         finally
         {
            ResteasyProviderFactory.clearContextData();
         }
      }
      finally
      {
         ResteasyProviderFactory defaultInstance = ResteasyProviderFactory.getInstance();
         if (defaultInstance instanceof ThreadLocalResteasyProviderFactory)
         {
            ThreadLocalResteasyProviderFactory.pop();
         }

      }
   }
}