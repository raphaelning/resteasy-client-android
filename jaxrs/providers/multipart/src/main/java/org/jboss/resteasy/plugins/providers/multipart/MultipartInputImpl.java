package org.jboss.resteasy.plugins.providers.multipart;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.MimeIOException;
import org.apache.james.mime4j.codec.Base64InputStream;
import org.apache.james.mime4j.codec.QuotedPrintableInputStream;
import org.apache.james.mime4j.descriptor.BodyDescriptor;
import org.apache.james.mime4j.field.ContentTypeField;
import org.apache.james.mime4j.message.BinaryBody;
import org.apache.james.mime4j.message.Body;
import org.apache.james.mime4j.message.BodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.Entity;
import org.apache.james.mime4j.message.Message;
import org.apache.james.mime4j.message.MessageBuilder;
import org.apache.james.mime4j.message.Multipart;
import org.apache.james.mime4j.message.TextBody;
import org.apache.james.mime4j.parser.Field;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.storage.DefaultStorageProvider;
import org.apache.james.mime4j.storage.StorageProvider;
import org.apache.james.mime4j.util.CharsetUtil;
import org.apache.james.mime4j.util.MimeUtil;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.CaseInsensitiveMap;
import org.jboss.resteasy.util.GenericType;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Providers;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MultipartInputImpl implements MultipartInput
{
   protected MediaType contentType;
   protected Providers workers;
   protected Message mimeMessage;
   protected List<InputPart> parts = new ArrayList<InputPart>();
   protected static final Annotation[] empty = {};
   protected MediaType defaultPartContentType = MultipartConstants.TEXT_PLAIN_WITH_CHARSET_US_ASCII_TYPE;
   protected String defaultPartCharset = null;

   // We hack MIME4j so that it always returns a BinaryBody so we don't have to deal with Readers and their charset conversions
   private static class BinaryOnlyMessageBuilder extends MessageBuilder
   {
      private Method expectMethod;
      private java.lang.reflect.Field bodyFactoryField;
      private java.lang.reflect.Field stackField;

      private void init()
      {
         try
         {
            expectMethod = MessageBuilder.class.getDeclaredMethod("expect", Class.class);
            expectMethod.setAccessible(true);
            bodyFactoryField = MessageBuilder.class.getDeclaredField("bodyFactory");
            bodyFactoryField.setAccessible(true);
            stackField = MessageBuilder.class.getDeclaredField("stack");
            stackField.setAccessible(true);
         }
         catch (Exception e)
         {
            throw new RuntimeException(e);
         }
      }

      private BinaryOnlyMessageBuilder(Entity entity)
      {
         super(entity);
         init();
      }

      private BinaryOnlyMessageBuilder(Entity entity, StorageProvider storageProvider)
      {
         super(entity, storageProvider);
         init();
      }

      @Override
      public void body(BodyDescriptor bd, InputStream is) throws MimeException, IOException
      {
         // the only thing different from the superclass is that we just return a BinaryBody no matter what
         try
         {
            expectMethod.invoke(this, Entity.class);
         }
         catch (Exception e)
         {
            throw new RuntimeException(e);
         }

         final String enc = bd.getTransferEncoding();

         final Body body;

         final InputStream decodedStream;
         if (MimeUtil.ENC_BASE64.equals(enc)) {
            decodedStream = new Base64InputStream(is);
         } else if (MimeUtil.ENC_QUOTED_PRINTABLE.equals(enc)) {
            decodedStream = new QuotedPrintableInputStream(is);
         } else {
            decodedStream = is;
         }

         try
         {
            BodyFactory factory = (BodyFactory)bodyFactoryField.get(this);
            body = factory.binaryBody(decodedStream);

            Stack<Object> st = (Stack<Object>)stackField.get(this);
            Entity entity = ((Entity) st.peek());
            entity.setBody(body);
         }
         catch (Exception e)
         {
            throw new RuntimeException(e);
         }

      }
   }

   private static class BinaryMessage extends Message
   {
      private BinaryMessage(InputStream is) throws IOException, MimeIOException
      {
         try {
            MimeStreamParser parser = new MimeStreamParser(null);
            parser.setContentHandler(new BinaryOnlyMessageBuilder(this, DefaultStorageProvider.getInstance()));
            parser.parse(is);
         } catch (MimeException e) {
            throw new MimeIOException(e);
         }

      }
   }

   public MultipartInputImpl(MediaType contentType, Providers workers)
   {
      this.contentType = contentType;
      this.workers = workers;
      HttpRequest httpRequest = ResteasyProviderFactory
              .getContextData(HttpRequest.class);
      if (httpRequest != null)
      {
         String defaultContentType = (String) httpRequest
                 .getAttribute(InputPart.DEFAULT_CONTENT_TYPE_PROPERTY);
         if (defaultContentType != null)
            this.defaultPartContentType = MediaType
                    .valueOf(defaultContentType);
         this.defaultPartCharset = (String) httpRequest.getAttribute(InputPart.DEFAULT_CHARSET_PROPERTY);
         if (defaultPartCharset != null)
         {
            this.defaultPartContentType = getMediaTypeWithDefaultCharset(this.defaultPartContentType);
         }
      }
   }

   public MultipartInputImpl(MediaType contentType, Providers workers,
                             MediaType defaultPartContentType, String defaultPartCharset)
   {
      this.contentType = contentType;
      this.workers = workers;
      if (defaultPartContentType != null) this.defaultPartContentType = defaultPartContentType;
      this.defaultPartCharset = defaultPartCharset;
      if (defaultPartCharset != null)
      {
         this.defaultPartContentType = getMediaTypeWithDefaultCharset(this.defaultPartContentType);
      }
   }

   public void parse(InputStream is) throws IOException
   {
      mimeMessage = new BinaryMessage(addHeaderToHeadlessStream(is));
      extractParts();
   }

   protected InputStream addHeaderToHeadlessStream(InputStream is)
           throws UnsupportedEncodingException
   {
      return new SequenceInputStream(createHeaderInputStream(), is);
   }

   protected InputStream createHeaderInputStream()
           throws UnsupportedEncodingException
   {
      String header = HttpHeaders.CONTENT_TYPE + ": " + contentType
              + "\r\n\r\n";
      return new ByteArrayInputStream(header.getBytes("utf-8"));
   }

   public String getPreamble()
   {
      return ((Multipart) mimeMessage.getBody()).getPreamble();
   }

   public List<InputPart> getParts()
   {
      return parts;
   }

   protected void extractParts() throws IOException
   {
      Multipart multipart = (Multipart) mimeMessage.getBody();
      for (BodyPart bodyPart : multipart.getBodyParts())
         parts.add(extractPart(bodyPart));
   }

   protected InputPart extractPart(BodyPart bodyPart) throws IOException
   {
      return new PartImpl(bodyPart);
   }

   public class PartImpl implements InputPart
   {

      private BodyPart bodyPart;
      private MediaType contentType;
      private MultivaluedMap<String, String> headers = new CaseInsensitiveMap<String>();
      private boolean contentTypeFromMessage;

      public PartImpl(BodyPart bodyPart)
      {
         this.bodyPart = bodyPart;
         for (Field field : bodyPart.getHeader())
         {
            headers.add(field.getName(), field.getBody());
            if (field instanceof ContentTypeField)
            {
               contentType = MediaType.valueOf(field.getBody());
               contentTypeFromMessage = true;
            }
         }
         if (contentType == null)
            contentType = defaultPartContentType;
         if (getCharset(contentType) == null)
         {
            if (defaultPartCharset != null)
            {
               contentType = getMediaTypeWithDefaultCharset(contentType);
            }
            else if (contentType.getType().equalsIgnoreCase("text"))
            {
               contentType = getMediaTypeWithCharset(contentType, "us-ascii");
            }
         }
      }

      @Override
      public void setMediaType(MediaType mediaType)
      {
         contentType = mediaType;
         contentTypeFromMessage = false;
         headers.putSingle("Content-Type", mediaType.toString());
      }

      public <T> T getBody(Class<T> type, Type genericType)
              throws IOException
      {
         MessageBodyReader<T> reader = workers.getMessageBodyReader(type,
                 genericType, empty, contentType);
         if (reader == null)
         {
            throw new RuntimeException("Unable to find a MessageBodyReader for media type: " + contentType + " and class type " + type.getName());
         }

         return reader.readFrom(type, genericType, empty, contentType, headers, getBody());

      }

      public <T> T getBody(GenericType<T> type) throws IOException
      {
         return getBody(type.getType(), type.getGenericType());
      }

      public InputStream getBody() throws IOException
      {
         Body body = bodyPart.getBody();
         InputStream result = null;
         if (body instanceof TextBody)
         {
            throw new UnsupportedOperationException();
            /*
            InputStreamReader reader = (InputStreamReader)((TextBody) body).getReader();
            StringBuilder inputBuilder = new StringBuilder();
            char[] buffer = new char[1024];
            while (true) {
               int readCount = reader.read(buffer);
               if (readCount < 0) {
                  break;
               }
               inputBuilder.append(buffer, 0, readCount);
            }
            String str = inputBuilder.toString();
            return new ByteArrayInputStream(str.getBytes(reader.getEncoding()));
            */
         }
         else if (body instanceof BinaryBody)
         {
            return ((BinaryBody)body).getInputStream();
         }
         return result;
      }

      public String getBodyAsString() throws IOException
      {
         return getBody(String.class, null);
      }

      public MultivaluedMap<String, String> getHeaders()
      {
         return headers;
      }

      public MediaType getMediaType()
      {
         return contentType;
      }

      public boolean isContentTypeFromMessage()
      {
         return contentTypeFromMessage;
      }
   }

   public static void main(String[] args) throws Exception
   {
      String input = "URLSTR: file:/Users/billburke/jboss/resteasy-jaxrs/resteasy-jaxrs/src/test/test-data/data.txt\r\n"
              + "--B98hgCmKsQ-B5AUFnm2FnDRCgHPDE3\r\n"
              + "Content-Disposition: form-data; name=\"part1\"\r\n"
              + "Content-Type: text/plain; charset=US-ASCII\r\n"
              + "Content-Transfer-Encoding: 8bit\r\n"
              + "\r\n"
              + "This is Value 1\r\n"
              + "--B98hgCmKsQ-B5AUFnm2FnDRCgHPDE3\r\n"
              + "Content-Disposition: form-data; name=\"part2\"\r\n"
              + "Content-Type: text/plain; charset=US-ASCII\r\n"
              + "Content-Transfer-Encoding: 8bit\r\n"
              + "\r\n"
              + "This is Value 2\r\n"
              + "--B98hgCmKsQ-B5AUFnm2FnDRCgHPDE3\r\n"
              + "Content-Disposition: form-data; name=\"data.txt\"; filename=\"data.txt\"\r\n"
              + "Content-Type: application/octet-stream; charset=ISO-8859-1\r\n"
              + "Content-Transfer-Encoding: binary\r\n"
              + "\r\n"
              + "hello world\r\n" + "--B98hgCmKsQ-B5AUFnm2FnDRCgHPDE3--";
      ByteArrayInputStream bais = new ByteArrayInputStream(input.getBytes());
      Map<String, String> parameters = new HashMap<String, String>();
      parameters.put("boundary", "B98hgCmKsQ-B5AUFnm2FnDRCgHPDE3");
      MediaType contentType = new MediaType("multipart", "form-data",
              parameters);
      MultipartInputImpl multipart = new MultipartInputImpl(contentType, null);
      multipart.parse(bais);

      System.out.println(multipart.getPreamble());
      System.out.println("**********");
      for (InputPart part : multipart.getParts())
      {
         System.out.println("--");
         System.out.println("\"" + part.getBodyAsString() + "\"");
      }
      System.out.println("done");

   }

   @Override
   public void close()
   {
      if (mimeMessage != null)
      {
         try
         {
            mimeMessage.dispose();
         }
         catch (Exception e)
         {

         }
      }
   }

   protected void finalize() throws Throwable
   {
      close();
   }

   protected String getCharset(MediaType mediaType)
   {
      for (Iterator<String> it = mediaType.getParameters().keySet().iterator(); it.hasNext(); )
      {
         String key = it.next();
         if ("charset".equalsIgnoreCase(key))
         {
            return mediaType.getParameters().get(key);
         }
      }
      return null;
   }
   
   private MediaType getMediaTypeWithDefaultCharset(MediaType mediaType)
   {
      String charset = defaultPartCharset;
      return getMediaTypeWithCharset(mediaType, charset);
   }

   private MediaType getMediaTypeWithCharset(MediaType mediaType, String charset)
   {
      Map<String, String> params = mediaType.getParameters();
      Map<String, String> newParams = new HashMap<String, String>();
      newParams.put("charset", charset);
      for (Iterator<String> it = params.keySet().iterator(); it.hasNext(); )
      {
         String key = it.next();
         if (!"charset".equalsIgnoreCase(key))
         {
            newParams.put(key, params.get(key));
         }
      }
      return new MediaType(mediaType.getType(), mediaType.getSubtype(), newParams);
   }

}
