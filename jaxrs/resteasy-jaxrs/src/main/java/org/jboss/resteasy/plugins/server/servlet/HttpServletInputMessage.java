package org.jboss.resteasy.plugins.server.servlet;

import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.core.SynchronousExecutionContext;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.plugins.providers.FormUrlEncodedProvider;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyAsynchronousContext;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.jboss.resteasy.util.Encode;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * Abstraction for an inbound http request on the server, or a response from a server to a client
 * <p/>
 * We have this abstraction so that we can reuse marshalling objects in a client framework and serverside framework
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class HttpServletInputMessage implements HttpRequest
{
   private final static Logger logger = Logger.getLogger(HttpServletInputMessage.class);
   protected ResteasyHttpHeaders httpHeaders;
   protected HttpServletRequest request;
   protected HttpServletResponse servletResponse;
   protected ServletContext servletContext;
   protected SynchronousDispatcher dispatcher;
   protected HttpResponse httpResponse;

   protected ResteasyUriInfo uri;
   protected String httpMethod;
   protected MultivaluedMap<String, String> formParameters;
   protected MultivaluedMap<String, String> decodedFormParameters;
   protected InputStream overridenStream;
   protected SynchronousExecutionContext executionContext;
   protected boolean wasForwarded;


   public HttpServletInputMessage(HttpServletRequest request, HttpServletResponse servletResponse, ServletContext servletContext, HttpResponse httpResponse, ResteasyHttpHeaders httpHeaders, ResteasyUriInfo uri, String httpMethod, SynchronousDispatcher dispatcher)
   {
      this.request = request;
      this.servletResponse = servletResponse;
      this.servletContext = servletContext;
      this.dispatcher = dispatcher;
      this.httpResponse = httpResponse;
      this.httpHeaders = httpHeaders;
      this.httpMethod = httpMethod;
      this.uri = uri;
      executionContext = new SynchronousExecutionContext(dispatcher, this, httpResponse);
   }

   @Override
   public MultivaluedMap<String, String> getMutableHeaders()
   {
      return httpHeaders.getMutableHeaders();
   }

   @Override
   public void setRequestUri(URI requestUri) throws IllegalStateException
   {
      uri = uri.setRequestUri(requestUri);
   }

   @Override
   public void setRequestUri(URI baseUri, URI requestUri) throws IllegalStateException
   {
      uri = new ResteasyUriInfo(baseUri.resolve(requestUri));
   }

   public MultivaluedMap<String, String> getPutFormParameters()
   {
      if (formParameters != null) return formParameters;
      if (MediaType.APPLICATION_FORM_URLENCODED_TYPE.isCompatible(getHttpHeaders().getMediaType()))
      {
         try
         {
            formParameters = FormUrlEncodedProvider.parseForm(getInputStream());
         }
         catch (IOException e)
         {
            throw new RuntimeException(e);
         }
      }
      else
      {
         throw new IllegalArgumentException("Request media type is not application/x-www-form-urlencoded");
      }
      return formParameters;
   }

   public MultivaluedMap<String, String> getPutDecodedFormParameters()
   {
      if (decodedFormParameters != null) return decodedFormParameters;
      decodedFormParameters = Encode.decode(getFormParameters());
      return decodedFormParameters;
   }


   @Override
   public Object getAttribute(String attribute)
   {
      return request.getAttribute(attribute);
   }

   @Override
   public void setAttribute(String name, Object value)
   {
      request.setAttribute(name, value);
   }

   @Override
   public void removeAttribute(String name)
   {
      request.removeAttribute(name);
   }

   @Override
   public Enumeration<String> getAttributeNames()
   {
      return request.getAttributeNames();
   }

   @Override
   public MultivaluedMap<String, String> getFormParameters()
   {
      if (formParameters != null) return formParameters;
      // Tomcat does not set getParameters() if it is a PUT request
      // so pull it out manually
      if (request.getMethod().equals("PUT") && (request.getParameterMap() == null || request.getParameterMap().isEmpty()))
      {
         return getPutFormParameters();
      }
      formParameters = Encode.encode(getDecodedFormParameters());
      return formParameters;
   }

   @Override
   public MultivaluedMap<String, String> getDecodedFormParameters()
   {
      if (decodedFormParameters != null) return decodedFormParameters;
      // Tomcat does not set getParameters() if it is a PUT request
      // so pull it out manually
      if (request.getMethod().equals("PUT") && (request.getParameterMap() == null || request.getParameterMap().isEmpty()))
      {
         return getPutDecodedFormParameters();
      }
      decodedFormParameters = new MultivaluedMapImpl<String, String>();
      Map<String, String[]> params = request.getParameterMap();
      for (Map.Entry<String, String[]> entry : params.entrySet())
      {
         String name = entry.getKey();
         String[] values = entry.getValue();
         MultivaluedMap<String, String> queryParams = uri.getQueryParameters();
         List<String> queryValues = queryParams.get(name);
         if (queryValues == null)
         {
            for (String val : values) decodedFormParameters.add(name, val);
         }
         else
         {
            for (String val : values)
            {
               if (!queryValues.contains(val))
               {
                  decodedFormParameters.add(name, val);
               }
            }
         }
      }
      return decodedFormParameters;

   }

   @Override
   public HttpHeaders getHttpHeaders()
   {
      return httpHeaders;
   }

   @Override
   public InputStream getInputStream()
   {
      if (overridenStream != null) return overridenStream;
      try
      {
         return request.getInputStream();
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   @Override
   public void setInputStream(InputStream stream)
   {
      this.overridenStream = stream;
   }

   @Override
   public ResteasyUriInfo getUri()
   {
      return uri;
   }

   @Override
   public String getHttpMethod()
   {
      return httpMethod;
   }

   @Override
   public void setHttpMethod(String method)
   {
      this.httpMethod = method;
   }

   @Override
   public ResteasyAsynchronousContext getAsyncContext()
   {
      return executionContext;
   }

   @Override
   public boolean isInitial()
   {
      return true;
   }

   @Override
   public void forward(String path)
   {
      try
      {
         wasForwarded = true;
         servletContext.getRequestDispatcher(path).forward(request, servletResponse);
      }
      catch (ServletException e)
      {
         throw new RuntimeException(e);
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   @Override
   public boolean wasForwarded()
   {
      return wasForwarded;
   }
}
