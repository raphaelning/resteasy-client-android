package org.jboss.resteasy.plugins.providers.jsonp;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AbstractJsonpProvider
{
   @Context
   javax.ws.rs.ext.Providers providers;

   public static Charset getCharset(final MediaType mediaType)
   {
      if (mediaType != null)
      {
         String charset = mediaType.getParameters().get("charset");
         if (charset != null) return Charset.forName(charset);
      }
      return Charset.defaultCharset();
   }

   protected JsonReader findReader(MediaType mediaType, InputStream is)
   {
      ContextResolver<JsonReaderFactory> resolver = providers.getContextResolver(JsonReaderFactory.class, mediaType);
      JsonReaderFactory factory = null;
      if (resolver != null)
      {
         factory = resolver.getContext(JsonReaderFactory.class);
      }
      if (factory == null)
      {
         factory = Json.createReaderFactory(null);
      }
      Charset charset = getCharset(mediaType);
      return factory.createReader(is, charset);
   }

   protected JsonWriter findWriter(MediaType mediaType, OutputStream os)
   {
      ContextResolver<JsonWriterFactory> resolver = providers.getContextResolver(JsonWriterFactory.class, mediaType);
      JsonWriterFactory factory = null;
      if (resolver != null)
      {
         factory = resolver.getContext(JsonWriterFactory.class);
      }
      if (factory == null)
      {
         factory = Json.createWriterFactory(null);
      }
      Charset charset = getCharset(mediaType);
      return factory.createWriter(os, charset);
   }
}
