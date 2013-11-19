package org.jboss.resteasy.plugins.providers.jaxb;

import org.jboss.resteasy.core.interception.DecoratorMatcher;
import org.jboss.resteasy.plugins.providers.AbstractEntityProvider;
import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.NoContent;
import org.jboss.resteasy.util.TypeConverter;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * A AbstractJAXBProvider.
 *
 * @author <a href="ryan@damnhandy.com">Ryan J. McDonough</a>
 * @version $Revision:$
 * @param <T>
 */
public abstract class AbstractJAXBProvider<T> extends AbstractEntityProvider<T>
{
   @Context
   protected Providers providers;

   private boolean expandEntityReferences = true;
   
   public AbstractJAXBProvider()
   {
      ResteasyConfiguration context = ResteasyProviderFactory.getContextData(ResteasyConfiguration.class);
      if (context != null)
      {
         String s = context.getParameter("resteasy.document.expand.entity.references");
         if (s != null)
         {
            setExpandEntityReferences(Boolean.parseBoolean(s));
         }
      }
   }
   
   public JAXBContext findJAXBContext(Class<?> type, Annotation[] annotations, MediaType mediaType, boolean reader)
           throws JAXBException
   {
      ContextResolver<JAXBContextFinder> resolver = providers.getContextResolver(JAXBContextFinder.class, mediaType);
      JAXBContextFinder finder = resolver.getContext(type);
      if (finder == null)
      {
         if (reader) throw new JAXBUnmarshalException("Could not find JAXBContextFinder for media type: " + mediaType);
         else throw new JAXBMarshalException("Could not find JAXBContextFinder for media type: " + mediaType);
      }
      return finder.findCachedContext(type, mediaType, annotations);
   }

   public static Marshaller decorateMarshaller(Class type, Annotation[] annotations, MediaType mediaType, Marshaller marshaller) throws JAXBException
   {
      DecoratorMatcher processor = new DecoratorMatcher();
      return processor.decorate(Marshaller.class, marshaller, type, annotations, mediaType);
   }

   public static Unmarshaller decorateUnmarshaller(Class type, Annotation[] annotations, MediaType mediaType, Unmarshaller marshaller) throws JAXBException
   {
      DecoratorMatcher processor = new DecoratorMatcher();
      return processor.decorate(Unmarshaller.class, marshaller, type, annotations, mediaType);
   }

   /**
    *
    */
   public T readFrom(Class<T> type,
                     Type genericType,
                     Annotation[] annotations,
                     MediaType mediaType,
                     MultivaluedMap<String, String> httpHeaders,
                     InputStream entityStream) throws IOException
   {
      try
      {
         NoContent.contentLengthCheck(httpHeaders);
         JAXBContext jaxb = findJAXBContext(type, annotations, mediaType, true);
         Unmarshaller unmarshaller = jaxb.createUnmarshaller();
         unmarshaller = decorateUnmarshaller(type, annotations, mediaType, unmarshaller);
       
         if (suppressExpandEntityExpansion())
         {
            return processWithoutEntityExpansion(unmarshaller, entityStream);
         }
         
         return (T) unmarshaller.unmarshal(new StreamSource(entityStream));
      }
      catch (JAXBException e)
      {
         throw new JAXBUnmarshalException(e);
      }
   }

   /**
    *
    */
   public void writeTo(T t,
                       Class<?> type,
                       Type genericType,
                       Annotation[] annotations,
                       MediaType mediaType,
                       MultivaluedMap<String, Object> httpHeaders,
                       OutputStream outputStream) throws IOException
   {
      try
      {
         Marshaller marshaller = getMarshaller(type, annotations, mediaType);
         marshaller = decorateMarshaller(type, annotations, mediaType, marshaller);
         marshaller.marshal(t, outputStream);
      }
      catch (JAXBException e)
      {
         throw new JAXBMarshalException(e);
      }
   }

   /**
    * FIXME Comment this
    *
    * @param type
    * @param mediaType
    * @param httpHeaders
    * @return
    */
   protected Marshaller getMarshaller(Class<?> type,
                                      Annotation[] annotations,
                                      MediaType mediaType)
   {
      try
      {
         JAXBContext jaxb = findJAXBContext(type, annotations, mediaType, false);
         Marshaller marshaller = jaxb.createMarshaller();
         setCharset(mediaType, marshaller);
         // Pretty Print the XML response.
         Object formatted = mediaType.getParameters().get("formatted");
         if (formatted != null)
         {
            Boolean value = TypeConverter.getBooleanValue(formatted.toString());
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, value);
         }
         return marshaller;
      }
      catch (JAXBException e)
      {
         throw new JAXBMarshalException(e);
      }
   }

   public static void setCharset(MediaType mediaType, Marshaller marshaller)
           throws PropertyException
   {
      String charset = getCharset(mediaType);
      // specify the character encoding if it is set on the media type
      if (charset != null)
      {
         marshaller.setProperty(Marshaller.JAXB_ENCODING, charset);
      }
   }

   /**
    * FIXME Comment this
    *
    * @param type
    * @param genericType
    * @param annotations
    * @return
    */
   protected abstract boolean isReadWritable(Class<?> type,
                                             Type genericType,
                                             Annotation[] annotations, MediaType mediaType);

   /**
    *
    */
   public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return isReadWritable(type, genericType, annotations, mediaType);
   }

   /**
    *
    */
   public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return isReadWritable(type, genericType, annotations, mediaType);
   }

   /**
    * FIXME Comment this
    *
    * @param mediaType
    * @return
    */
   public static String getCharset(final MediaType mediaType)
   {
      if (mediaType != null)
      {
         return mediaType.getParameters().get("charset");
      }
      return null;
   }

   public boolean isExpandEntityReferences()
   {
      return expandEntityReferences;
   }

   public void setExpandEntityReferences(boolean expandEntityReferences)
   {
      this.expandEntityReferences = expandEntityReferences;
   }
   
   protected boolean suppressExpandEntityExpansion()
   {
      return !isExpandEntityReferences();
   }
   
   protected T processWithoutEntityExpansion(Unmarshaller unmarshaller, InputStream entityStream) throws JAXBException
   {
      unmarshaller = new ExternalEntityUnmarshaller(unmarshaller);
      return (T) unmarshaller.unmarshal(entityStream);
   }
}
