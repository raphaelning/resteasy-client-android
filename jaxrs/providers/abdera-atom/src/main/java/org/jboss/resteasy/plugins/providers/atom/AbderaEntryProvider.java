package org.jboss.resteasy.plugins.providers.atom;

import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.writer.Writer;

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
@Provider
@Consumes(MediaType.APPLICATION_ATOM_XML)
@Produces(MediaType.APPLICATION_ATOM_XML)
public class AbderaEntryProvider extends AbstractAbdera implements MessageBodyReader<Entry>, MessageBodyWriter<Entry>
{

   public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return type.equals(Entry.class);
   }

   public Entry readFrom(Class<Entry> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException
   {
      Parser parser = abdera.getParser();
      Document<Entry> doc = parser.parse(entityStream);
      return doc.getRoot();
   }

   public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return Entry.class.isAssignableFrom(type);
   }

   public long getSize(Entry entry, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return -1;
   }

   public void writeTo(Entry entry, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException
   {
      Writer writer = abdera.getWriter();
      writer.writeTo(entry, entityStream);
   }
}