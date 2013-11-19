/**
 *
 */
package org.jboss.resteasy.plugins.providers;

import org.jboss.resteasy.util.NoContent;

import javax.activation.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author <a href="mailto:ryan@damnhandy.com">Ryan J. McDonough</a>
 * @version $Revision:$
 */
@Provider
@Consumes("*/*")
@Produces("*/*")
public class DataSourceProvider extends AbstractEntityProvider<DataSource>
{

   protected static class SequencedDataSource implements DataSource
   {
      private byte[] byteBuffer;
      private int byteBufferOffset;
      private int byteBufferLength;
      private File tempFile;
      private String type;

      public SequencedDataSource(byte[] byteBuffer, int byteBufferOffset,
                                 int byteBufferLength, File tempFile, String type)
      {
         super();
         this.byteBuffer = byteBuffer;
         this.byteBufferOffset = byteBufferOffset;
         this.byteBufferLength = byteBufferLength;
         this.tempFile = tempFile;
         this.type = type;
      }

      public String getContentType()
      {
         return type;
      }

      public InputStream getInputStream() throws IOException
      {
         InputStream bis = new ByteArrayInputStream(byteBuffer, byteBufferOffset, byteBufferLength);
         if (tempFile == null)
            return bis;
         InputStream fis = new FileInputStream(tempFile);
         return new SequenceInputStream(bis, fis);
      }

      public String getName()
      {
         return "";
      }

      public OutputStream getOutputStream() throws IOException
      {
         throw new IOException("No output stream allowed");
      }

   }


   /**
    * @param in
    * @param mediaType
    * @return
    * @throws IOException
    */
   public static DataSource readDataSource(final InputStream in, final MediaType mediaType) throws IOException
   {
      byte[] memoryBuffer = new byte[4096];
      int readCount = in.read(memoryBuffer, 0, memoryBuffer.length);

      File tempFile = null;
      if (in.available() > 0)
      {
         tempFile = File.createTempFile("resteasy-provider-datasource", null);
         FileOutputStream fos = new FileOutputStream(tempFile);
         try
         {
            ProviderHelper.writeTo(in, fos);
         }
         finally
         {
            fos.close();
         }
      }

      if (readCount == -1)
         readCount = 0;

      return new SequencedDataSource(memoryBuffer, 0, readCount, tempFile, mediaType.toString());
   }

   /**
    * FIXME Comment this
    *
    * @param type
    * @param genericType
    * @param annotations
    * @return
    * @see javax.ws.rs.ext.MessageBodyReader
    */
   public boolean isReadable(Class<?> type,
                             Type genericType,
                             Annotation[] annotations, MediaType mediaType)
   {
      return DataSource.class.isAssignableFrom(type);
   }


   /**
    * FIXME Comment this
    *
    * @param type
    * @param genericType
    * @param annotations
    * @param mediaType
    * @param httpHeaders
    * @param entityStream
    * @return
    * @throws IOException
    * @throws WebApplicationException
    * @see @see javax.ws.rs.ext.MessageBodyReader#readFrom(java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType, javax.ws.rs.core.MultivaluedMap, java.io.InputStream)
    */
   public DataSource readFrom(Class<DataSource> type,
                              Type genericType,
                              Annotation[] annotations,
                              MediaType mediaType,
                              MultivaluedMap<String, String> httpHeaders,
                              InputStream entityStream) throws IOException
   {
      if (NoContent.isContentLengthZero(httpHeaders)) return readDataSource(new ByteArrayInputStream(new byte[0]), mediaType);
      return readDataSource(entityStream, mediaType);
   }


   /**
    * FIXME Comment this
    *
    * @param type
    * @param genericType
    * @param annotations
    * @return
    * @see @see javax.ws.rs.ext.MessageBodyWriter#isWriteable(java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[])
    */
   public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return DataSource.class.isAssignableFrom(type);
   }

   /**
    * FIXME Comment this
    *
    * @param dataSource
    * @param type
    * @param genericType
    * @param annotations
    * @param mediaType
    * @param httpHeaders
    * @param entityStream
    * @throws IOException
    * @throws WebApplicationException
    * @see @see javax.ws.rs.ext.MessageBodyWriter#writeTo(java.lang.Object, java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType, javax.ws.rs.core.MultivaluedMap, java.io.OutputStream)
    */
   public void writeTo(DataSource dataSource,
                       Class<?> type,
                       Type genericType,
                       Annotation[] annotations,
                       MediaType mediaType,
                       MultivaluedMap<String, Object> httpHeaders,
                       OutputStream entityStream) throws IOException
   {
      InputStream in = dataSource.getInputStream();
      try
      {
         ProviderHelper.writeTo(in, entityStream);
      }
      finally
      {
         in.close();
      }

   }

}
