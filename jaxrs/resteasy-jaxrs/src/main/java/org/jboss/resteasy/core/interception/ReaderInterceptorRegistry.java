package org.jboss.resteasy.core.interception;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.interception.MessageBodyReaderContext;
import org.jboss.resteasy.spi.interception.MessageBodyReaderInterceptor;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ReaderInterceptorRegistry extends JaxrsInterceptorRegistry<ReaderInterceptor>
{
   protected LegacyPrecedence precedence;

   public ReaderInterceptorRegistry(ResteasyProviderFactory providerFactory, LegacyPrecedence precedence)
   {
      super(providerFactory, ReaderInterceptor.class);
      this.precedence = precedence;
   }

   public ReaderInterceptorRegistry clone(ResteasyProviderFactory factory)
   {
      ReaderInterceptorRegistry clone = new ReaderInterceptorRegistry(factory, precedence);
      clone.interceptors.addAll(interceptors);
      return clone;
   }

   private static class MessageBodyReaderContextFacade implements MessageBodyReaderContext
   {
      protected final ReaderInterceptorContext readerInterceptorContext;

      private MessageBodyReaderContextFacade(ReaderInterceptorContext readerInterceptorContext)
      {
         this.readerInterceptorContext = readerInterceptorContext;
      }

      @Override
      public Class getType()
      {
         return readerInterceptorContext.getType();
      }

      @Override
      public void setType(Class type)
      {
         readerInterceptorContext.setType(type);
      }

      @Override
      public Type getGenericType()
      {
         return readerInterceptorContext.getGenericType();
      }

      @Override
      public void setGenericType(Type genericType)
      {
         readerInterceptorContext.setGenericType(genericType);
      }

      @Override
      public Annotation[] getAnnotations()
      {
         return readerInterceptorContext.getAnnotations();
      }

      @Override
      public void setAnnotations(Annotation[] annotations)
      {
         readerInterceptorContext.setAnnotations(annotations);
      }

      @Override
      public MediaType getMediaType()
      {
         return readerInterceptorContext.getMediaType();
      }

      @Override
      public void setMediaType(MediaType mediaType)
      {
         readerInterceptorContext.setMediaType(mediaType);
      }

      @Override
      public MultivaluedMap<String, String> getHeaders()
      {
         return readerInterceptorContext.getHeaders();
      }

      @Override
      public InputStream getInputStream()
      {
         return readerInterceptorContext.getInputStream();
      }

      @Override
      public void setInputStream(InputStream is)
      {
         readerInterceptorContext.setInputStream(is);
      }

      @Override
      public Object getAttribute(String attribute)
      {
         return readerInterceptorContext.getProperty(attribute);
      }

      @Override
      public void setAttribute(String name, Object value)
      {
         readerInterceptorContext.setProperty(name, value);
      }

      @Override
      public void removeAttribute(String name)
      {
         readerInterceptorContext.removeProperty(name);
      }

      @Override
      public Object proceed() throws IOException, WebApplicationException
      {
         return readerInterceptorContext.proceed();
      }
   }

   public static class ReaderInterceptorFacade implements ReaderInterceptor
   {
      protected final MessageBodyReaderInterceptor interceptor;

      public ReaderInterceptorFacade(MessageBodyReaderInterceptor interceptor)
      {
         this.interceptor = interceptor;
      }

      public MessageBodyReaderInterceptor getInterceptor()
      {
         return interceptor;
      }

      @Override
      public Object aroundReadFrom(ReaderInterceptorContext readerInterceptorContext) throws IOException, WebApplicationException
      {
         MessageBodyReaderContextFacade facade = new MessageBodyReaderContextFacade(readerInterceptorContext);
         return interceptor.read(facade);
      }
   }


   public void registerLegacy(Class<? extends MessageBodyReaderInterceptor> decl)
   {
      register(new LegacyPerMethodInterceptorFactory(decl, precedence)
      {
         @Override
         public Match postMatch(Class declaring, AccessibleObject target)
         {
            Object obj = getLegacyMatch(declaring, target);
            if (obj == null) return null;
            MessageBodyReaderInterceptor interceptor = (MessageBodyReaderInterceptor)obj;
            return new Match(new ReaderInterceptorFacade(interceptor), order);
         }

      });
   }

   public void registerLegacy(MessageBodyReaderInterceptor interceptor)
   {
      register(new LegacySingletonInterceptorFactory(interceptor.getClass(), interceptor, precedence)
      {
         @Override
         public Match postMatch(Class declaring, AccessibleObject target)
         {
            Object obj = getLegacyMatch(declaring, target);
            if (obj == null) return null;
            MessageBodyReaderInterceptor interceptor = (MessageBodyReaderInterceptor)obj;
            return new Match(new ReaderInterceptorFacade(interceptor), order);
         }
      });

   }
}
