package org.jboss.resteasy.client;

import org.jboss.resteasy.spi.Link;
import org.jboss.resteasy.spi.LinkHeader;
import org.jboss.resteasy.util.GenericType;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Response extension for the RESTEasy client framework. Use this, or Response
 * in your client proxy interface method return type declarations if you want
 * access to the response entity as well as status and header information.
 *
 * @deprecated
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Deprecated
public abstract class ClientResponse<T> extends Response
{
   /**
    * This method returns the same exact map as Response.getMetadata() except as a map of strings rather than objects
    *
    * @return
    */
   public abstract MultivaluedMap<String, String> getResponseHeaders();

   public abstract Response.Status getResponseStatus();

   /**
    * Unmarshal the target entity from the response OutputStream.  You must have type information set via <T>
    * otherwise, this will not work.
    * <p/>
    * This method actually does the reading on the OutputStream.  It will only do the read once.  Afterwards, it will
    * cache the result and return the cached result.
    *
    * @return
    */
   public abstract T getEntity();

   /**
    * Extract the response body with the provided type information
    * <p/>
    * This method actually does the reading on the OutputStream.  It will only do the read once.  Afterwards, it will
    * cache the result and return the cached result.
    *
    * @param type
    * @param genericType
    * @param <T2>
    * @return
    */
   public abstract <T2> T2 getEntity(Class<T2> type);

   /**
    * Extract the response body with the provided type information
    * <p/>
    * This method actually does the reading on the OutputStream.  It will only do the read once.  Afterwards, it will
    * cache the result and return the cached result.
    *
    * @param type
    * @param genericType
    * @param <T2>
    * @return
    */
   public abstract <T2> T2 getEntity(Class<T2> type, Type genericType);

   /**
    * @param type
    * @param genericType
    * @param annotations
    * @param <T2>
    * @return
    */
   public abstract <T2> T2 getEntity(Class<T2> type, Type genericType, Annotation[] annotations);

   /**
    * Extract the response body with the provided type information.  GenericType is a trick used to
    * pass in generic type information to the resteasy runtime.
    * <p/>
    * For example:
    * <pre>
    * List<String> list = response.getEntity(new GenericType<List<String>() {});
    *
    *
    * This method actually does the reading on the OutputStream.  It will only do the read once.  Afterwards, it will
    * cache the result and return the cached result.
    *
    * @param type
    * @param <T2>
    * @return
    */
   public abstract <T2> T2 getEntity(GenericType<T2> type);

   /**
    * @param type
    * @param annotations
    * @param <T2>
    * @return
    */
   public abstract <T2> T2 getEntity(GenericType<T2> type, Annotation[] annotations);

   /**
    * Get the <a href="http://tools.ietf.org/html/draft-nottingham-http-link-header-10">link headers</a> of the response.
    * All Link objects returned will automatically have the same ClientExecutor as the request.
    *
    * @return non-null
    */
   public abstract LinkHeader getLinkHeader();

   /**
    * Get the Location header as a Link so you can easily execute on it.
    * All Link objects returned will automatically have the same ClientExecutor as the request.
    *
    * @return
    */
   public abstract Link getLocationLink();

   /**
    * Header is assumed to be a URL, a Link object is created from it if it exists.  Also, the type field of the
    * link with be initialized if there is another header appended with -Type.  i.e. if the header was "custom"
    * it will also look for a header of custom-type and expect that this is a media type.
    * <p/>
    * All Link objects returned will automatically have the same ClientExecutor as the request.
    *
    * @param headerName
    * @return null if it doesn't exist
    */
   public abstract Link getHeaderAsLink(String headerName);

   /**
    * Attempts to reset the InputStream of the response.  Useful for refetching an entity after a marshalling failure
    */
   public abstract void resetStream();

   public abstract void releaseConnection();

   /**
    * Used to pass information to and between interceptors.
    *
    * @return
    */
   public abstract Map<String, Object> getAttributes();
}
