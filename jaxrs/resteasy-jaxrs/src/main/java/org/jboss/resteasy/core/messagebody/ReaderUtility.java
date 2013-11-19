package org.jboss.resteasy.core.messagebody;

import org.jboss.resteasy.core.interception.AbstractReaderInterceptorContext;
import org.jboss.resteasy.core.interception.ClientReaderInterceptorContext;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ReaderException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.ReaderInterceptor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for accessing RESTEasy's MessageBodyReader setup
 *
 * @author <a href="mailto:sduskis@gmail.com">Solomon Duskis</a>
 * @version $Revision: 1 $
 */

@SuppressWarnings("unchecked")
public abstract class ReaderUtility
{
   private ResteasyProviderFactory factory;
   private ReaderInterceptor[] interceptors;

   public static <T> T read(Class<T> type, String contentType, String buffer)
           throws IOException
   {
      return read(type, contentType, buffer.getBytes());
   }

   public static <T> T read(Class<T> type, String contentType, byte[] buffer)
           throws IOException
   {
      return read(type, MediaType.valueOf(contentType),
              new ByteArrayInputStream(buffer));
   }

   public static <T> T read(Class<T> type, MediaType mediaType, byte[] buffer)
           throws IOException
   {
      return read(type, mediaType, new ByteArrayInputStream(buffer));
   }

   public static <T> T read(Class<T> type, MediaType mt, InputStream is)
           throws IOException
   {
      return new ReaderUtility()
      {
         @Override
         public RuntimeException createReaderNotFound(Type genericType,
                                                      MediaType mediaType)
         {
            throw new RuntimeException("Could not read type " + genericType
                    + " for media type " + mediaType);
         }
      }.doRead(type, mt, is);
   }

   public ReaderUtility()
   {
      this(ResteasyProviderFactory.getInstance(), null);
   }

   public ReaderUtility(ResteasyProviderFactory factory,
                        ReaderInterceptor[] interceptors)
   {
      this.factory = factory;
      this.interceptors = interceptors;
   }

   public <T> T doRead(Class<T> type, MediaType mediaType, InputStream is)
           throws IOException
   {
      return doRead(type, type, mediaType, null, null, is);
   }

   public <T> T doRead(Class<T> type, Type genericType, MediaType mediaType,
                       MultivaluedMap<String, String> requestHeaders, InputStream is)
           throws IOException
   {
      return doRead(type, genericType, mediaType, null, requestHeaders, is);
   }

   public Object doRead(HttpRequest request, Class type, Type genericType,
                        Annotation[] annotations, MediaType mediaType) throws IOException
   {
      return doRead(type, genericType, mediaType, annotations, request
              .getHttpHeaders().getRequestHeaders(), request.getInputStream());
   }

   public <T> T doRead(Class<T> type, Type genericType, MediaType mediaType,
                       Annotation[] annotations,
                       MultivaluedMap<String, String> requestHeaders, InputStream inputStream)
           throws IOException
   {
      try
      {
         final Map<String, Object> attributes = new HashMap<String, Object>();
         AbstractReaderInterceptorContext messageBodyReaderContext = new ClientReaderInterceptorContext(interceptors, factory, type,
                 genericType, annotations, mediaType, requestHeaders, inputStream, attributes);
         return (T) messageBodyReaderContext
                 .proceed();
      }
      catch (Exception e)
      {
         if (e instanceof ReaderException)
         {
            throw (ReaderException) e;
         }
         else
         {
            throw new ReaderException(e);
         }
      }
   }

   public abstract RuntimeException createReaderNotFound(Type genericType,
                                                         MediaType mediaType);
}
