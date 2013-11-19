package org.jboss.resteasy.spi;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import java.io.InputStream;
import java.net.URI;
import java.util.Enumeration;

/**
 * Bridge interface between the base Resteasy JAX-RS implementation and the actual HTTP transport (i.e. a servlet container)
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface HttpRequest
{
   HttpHeaders getHttpHeaders();

   MultivaluedMap<String, String> getMutableHeaders();

   InputStream getInputStream();

   /**
    * If you are using a servlet container, this will *NOT* override the HttpServletRequest.getInputStream().
    * It will only override it for the resteasy HttpRequest
    *
    * @param stream
    */
   void setInputStream(InputStream stream);

   ResteasyUriInfo getUri();

   String getHttpMethod();
   void setHttpMethod(String method);

   void setRequestUri(URI requestUri) throws IllegalStateException;
   void setRequestUri(URI baseUri, URI requestUri) throws IllegalStateException;
   /**
    * application/x-www-form-urlencoded parameters
    * <p/>
    * This is here because @FormParam needs it and for when there are servlet filters that eat up the input stream
    *
    * @return null if no parameters, this is encoded map
    */
   MultivaluedMap<String, String> getFormParameters();

   MultivaluedMap<String, String> getDecodedFormParameters();

   /**
    * Map of contextual data.  Similar to HttpServletRequest attributes
    *
    * @return
    */
   Object getAttribute(String attribute);

   void setAttribute(String name, Object value);

   void removeAttribute(String name);

   public Enumeration<String> getAttributeNames();


   public ResteasyAsynchronousContext getAsyncContext();

   public boolean isInitial();

   public void forward(String path);

   public boolean wasForwarded();

}
