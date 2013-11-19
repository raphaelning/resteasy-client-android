package org.jboss.resteasy.jose.jws;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.jboss.resteasy.jose.Base64Url;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Providers;
import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JWSInput
{
   String wireString;
   String encodedHeader;
   String encodedContent;
   String encodedSignature;
   JWSHeader header;
   Providers providers;
   byte[] content;
   byte[] signature;

   private static ObjectMapper mapper = new ObjectMapper();

   static
   {
      mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
   }

   public JWSInput(String wire)
   {
      this(wire, null);
   }

   public JWSInput(String wire, Providers providers)
   {
      this.providers = providers;
      this.wireString = wire;
      String[] parts = wire.split("\\.");
      if (parts.length < 2 || parts.length > 3) throw new IllegalArgumentException("Parsing error");
      encodedHeader = parts[0];
      encodedContent = parts[1];
      try
      {
         content = Base64Url.decode(encodedContent);
         if (parts.length > 2)
         {
            encodedSignature = parts[2];
            signature = Base64Url.decode(encodedSignature);

         }
         byte[] headerBytes = Base64Url.decode(encodedHeader);
         header = mapper.readValue(headerBytes, JWSHeader.class);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   public String getWireString()
   {
      return wireString;
   }

   public String getEncodedHeader()
   {
      return encodedHeader;
   }

   public String getEncodedContent()
   {
      return encodedContent;
   }

   public String getEncodedSignature()
   {
      return encodedSignature;
   }

   public JWSHeader getHeader()
   {
      return header;
   }

   public byte[] getContent()
   {
      return content;
   }

   public byte[] getSignature()
   {
      return signature;
   }

   public <T> T readContent(Class<T> type)
   {
      MediaType mediaType = MediaType.WILDCARD_TYPE;
      if (header.getContentType() != null)
      {
         mediaType = MediaType.valueOf(header.getContentType());
      }
      return (T) readContent(type, null, null, mediaType);

   }

   public Object readContent(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      MessageBodyReader reader = providers.getMessageBodyReader(type, genericType, annotations, mediaType);
      if (reader == null) throw new RuntimeException("Unable to find reader for content type");

      try
      {
         ByteArrayInputStream bais = new ByteArrayInputStream(content);
         return reader.readFrom(type, genericType, annotations, mediaType, new MultivaluedHashMap<String, String>(), bais);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }


}
