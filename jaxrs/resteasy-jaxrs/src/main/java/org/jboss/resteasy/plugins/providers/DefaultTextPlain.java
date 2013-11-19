package org.jboss.resteasy.plugins.providers;

import org.jboss.resteasy.util.NoContent;
import org.jboss.resteasy.util.NoContentInputStreamDelegate;
import org.jboss.resteasy.util.TypeConverter;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SuppressWarnings("unchecked")
@Provider
@Produces("text/plain")
@Consumes("text/plain")
public class DefaultTextPlain implements MessageBodyReader, MessageBodyWriter
{
   public boolean isReadable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      // StringTextStar should pick up strings
      return !String.class.equals(type) && TypeConverter.isConvertable(type);
   }

   public Object readFrom(Class type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap httpHeaders, InputStream entityStream) throws IOException, WebApplicationException
   {
      InputStream delegate = NoContent.noContentCheck(httpHeaders, entityStream);
      String value = ProviderHelper.readString(delegate, mediaType);
      return TypeConverter.getType(type, value);
   }

   public boolean isWriteable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      // StringTextStar should pick up strings
      return !String.class.equals(type) && !type.isArray();
   }

   public long getSize(Object o, Class type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return o.toString().getBytes().length;
   }

   public void writeTo(Object o, Class type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException
   {
      String charset = mediaType.getParameters().get("charset");
      if (charset == null) entityStream.write(o.toString().getBytes());
      else entityStream.write(o.toString().getBytes(charset));
   }
}
