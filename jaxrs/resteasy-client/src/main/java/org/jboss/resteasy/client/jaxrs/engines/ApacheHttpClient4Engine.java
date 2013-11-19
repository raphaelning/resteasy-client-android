package org.jboss.resteasy.client.jaxrs.engines;

import org.apache.commons.io.output.DeferredFileOutputStream;
import ch.boye.httpclientandroidlib.conn.ClientConnectionManager;
import ch.boye.httpclientandroidlib.entity.FileEntity;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.client.params.HttpClientParams;
import ch.boye.httpclientandroidlib.conn.params.ConnRoutePNames;
import ch.boye.httpclientandroidlib.entity.ByteArrayEntity;
import ch.boye.httpclientandroidlib.message.BasicHeader;
import ch.boye.httpclientandroidlib.protocol.HTTP;
import ch.boye.httpclientandroidlib.protocol.HttpContext;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.core.SelfExpandingBufferredInputStream;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.util.CaseInsensitiveMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MultivaluedMap;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

/**
 *
 * Modified for Android by Raphael Ning.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApacheHttpClient4Engine implements ClientHttpEngine
{
   private final static Logger logger = Logger.getLogger(ApacheHttpClient4Engine.class);

   /**
    * Used to build temp file prefix.
    */
   private static String processId = null;

   static
   {
      processId = String.format("%d", new Random().nextInt(32768));
   }

   protected HttpClient httpClient;
   protected boolean createdHttpClient;
   protected HttpContext httpContext;
   protected boolean closed;
   protected SSLContext sslContext;
   protected HostnameVerifier hostnameVerifier;
   /**
    * For uploading File's over JAX-RS framework, this property, together with {@link #fileUploadMemoryUnit},
    * defines the maximum File size allowed in memory. If fileSize exceeds this size, it will be stored to
    * {@link #fileUploadTempFileDir}. <br>
    * <br>
    * Defaults to 1 MB
    */
   protected int fileUploadInMemoryThresholdLimit = 1;

   /**
    * Enumeration to represent memory units.
    */
   public static enum MemoryUnit
   {
      /**
       * Bytes
       */
      BY,
      /**
       * Killo Bytes
       */
      KB,

      /**
       * Mega Bytes
       */
      MB,

      /**
       * Giga Bytes
       */
      GB
   }

   /**
    * The unit for {@link #fileUploadInMemoryThresholdLimit}. <br>
    * <br>
    * Defaults to MB.
    *
    * @see MemoryUnit
    */
   protected MemoryUnit fileUploadMemoryUnit = MemoryUnit.MB;

   /**
    * Temp directory to write output request stream to. Any file to be uploaded has to be written out to the
    * output request stream to be sent to the service and when the File is too huge the output request stream is
    * written out to the disk rather than to memory. <br>
    * <br>
    * Defaults to JVM temp directory.
    */
   protected File fileUploadTempFileDir = new File(System.getProperty("java.io.tmpdir"));


   public ApacheHttpClient4Engine()
   {
      this.httpClient = new DefaultHttpClient();
      this.createdHttpClient = true;
   }

   public ApacheHttpClient4Engine(HttpClient httpClient)
   {
      this.httpClient = httpClient;
   }

   public ApacheHttpClient4Engine(HttpClient httpClient, boolean closeHttpClient)
   {
      this.httpClient = httpClient;
      this.createdHttpClient = closeHttpClient;
   }


   public ApacheHttpClient4Engine(HttpClient httpClient, HttpContext httpContext)
   {
      this.httpClient = httpClient;
      this.httpContext = httpContext;
   }

   /**
    * Based on memory unit
    * @return
    */
   public int getFileUploadInMemoryThresholdLimit()
   {
      return fileUploadInMemoryThresholdLimit;
   }

   public void setFileUploadInMemoryThresholdLimit(int fileUploadInMemoryThresholdLimit)
   {
      this.fileUploadInMemoryThresholdLimit = fileUploadInMemoryThresholdLimit;
   }

   public MemoryUnit getFileUploadMemoryUnit()
   {
      return fileUploadMemoryUnit;
   }

   public void setFileUploadMemoryUnit(MemoryUnit fileUploadMemoryUnit)
   {
      this.fileUploadMemoryUnit = fileUploadMemoryUnit;
   }

   public File getFileUploadTempFileDir()
   {
      return fileUploadTempFileDir;
   }

   public void setFileUploadTempFileDir(File fileUploadTempFileDir)
   {
      this.fileUploadTempFileDir = fileUploadTempFileDir;
   }

   public HttpClient getHttpClient()
   {
      return httpClient;
   }

   public HttpContext getHttpContext()
   {
      return httpContext;
   }

   public void setHttpContext(HttpContext httpContext)
   {
      this.httpContext = httpContext;
   }

   @Override
   public SSLContext getSslContext()
   {
      return sslContext;
   }

   public void setSslContext(SSLContext sslContext)
   {
      this.sslContext = sslContext;
   }

   @Override
   public HostnameVerifier getHostnameVerifier()
   {
      return hostnameVerifier;
   }

   public void setHostnameVerifier(HostnameVerifier hostnameVerifier)
   {
      this.hostnameVerifier = hostnameVerifier;
   }

   public HttpHost getDefaultProxy()
   {
	   return (HttpHost) httpClient.getParams().getParameter(ConnRoutePNames.DEFAULT_PROXY);
   }
   
   public void setDefaultProxy(HttpHost defaultProxy)
   {
	   httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, defaultProxy);
   }
   public static CaseInsensitiveMap<String> extractHeaders(
           HttpResponse response)
   {
      final CaseInsensitiveMap<String> headers = new CaseInsensitiveMap<String>();

      for (Header header : response.getAllHeaders())
      {
         headers.add(header.getName(), header.getValue());
      }
      return headers;
   }

   @SuppressWarnings("unchecked")
   public ClientResponse invoke(ClientInvocation request)
   {
      String uri = request.getUri().toString();
      final HttpRequestBase httpMethod = createHttpMethod(uri, request.getMethod());
      final HttpResponse res;
      try
      {
         loadHttpMethod(request, httpMethod);

         res = httpClient.execute(httpMethod, httpContext);
      }
      catch (Exception e)
      {
         throw new ProcessingException("Unable to invoke request", e);
      }
      finally
      {
         cleanUpAfterExecute(httpMethod);
      }

      ClientResponse response = new ClientResponse(request.getClientConfiguration())
      {
         InputStream stream;
         InputStream hc4Stream;

         @Override
         protected void setInputStream(InputStream is)
         {
            stream = is;
         }

         public InputStream getInputStream()
         {
            if (stream == null)
            {
               HttpEntity entity = res.getEntity();
               if (entity == null) return null;
               try
               {
                  hc4Stream = entity.getContent();
                  stream = new SelfExpandingBufferredInputStream(hc4Stream);
               }
               catch (IOException e)
               {
                  throw new RuntimeException(e);
               }
            }
            return stream;
         }

         public void releaseConnection() throws IOException
         {
            // Apache Client 4 is stupid,  You have to get the InputStream and close it if there is an entity
            // otherwise the connection is never released.  There is, of course, no close() method on response
            // to make this easier.
            try {
               // Another stupid thing...TCK is testing a specific exception from stream.close()
               // so, we let it propagate up.
               if (stream != null)
               {
                  stream.close();
               }
               else
               {
                  InputStream is = getInputStream();
                  if (is != null)
                  {
                     is.close();
                  }
               }
            }
            finally {
               // just in case the input stream was entirely replaced and not wrapped, we need
               // to close the apache client input stream.
               if (hc4Stream != null)
               {
                  try {
                     hc4Stream.close();
                  }
                  catch (IOException ignored) {

                  }
               }
               else
               {
                  try
                  {
                     HttpEntity entity = res.getEntity();
                     if (entity != null) entity.getContent().close();
                  }
                  catch (IOException ignored)
                  {
                  }

               }

            }
          }
      };
      response.setProperties(request.getMutableProperties());
      response.setStatus(res.getStatusLine().getStatusCode());
      response.setHeaders(extractHeaders(res));
      response.setClientConfiguration(request.getClientConfiguration());
      return response;
   }

   protected HttpRequestBase createHttpMethod(String url, String restVerb)
   {
      if ("GET".equals(restVerb))
      {
         return new HttpGet(url);
      }
      else if ("POST".equals(restVerb))
      {
         return new HttpPost(url);
      }
      else
      {
         final String verb = restVerb;
         return new HttpPost(url)
         {
            @Override
            public String getMethod()
            {
               return verb;
            }
         };
      }
   }

   protected void loadHttpMethod(final ClientInvocation request, HttpRequestBase httpMethod) throws Exception
   {
      if (httpMethod instanceof HttpGet && false) // todo  && request.followRedirects())
      {
         HttpClientParams.setRedirecting(httpMethod.getParams(), true);
      }
      else
      {
         HttpClientParams.setRedirecting(httpMethod.getParams(), false);
      }

      if (request.getEntity() != null)
      {
         if (httpMethod instanceof HttpGet) throw new ProcessingException("A GET request cannot have a body.");

         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         request.getDelegatingOutputStream().setDelegate(baos);
         try
         {
            HttpEntity entity = buildEntity(request);
            HttpPost post = (HttpPost) httpMethod;
            commitHeaders(request, httpMethod);
            post.setEntity(entity);
         }
         catch (IOException e)
         {
            throw new RuntimeException(e);
         }
      }
      else // no body
      {
         commitHeaders(request, httpMethod);
      }
   }

   protected void commitHeaders(ClientInvocation request, HttpRequestBase httpMethod)
   {
      MultivaluedMap<String, String> headers = request.getHeaders().asMap();
      for (Map.Entry<String, List<String>> header : headers.entrySet())
      {
         List<String> values = header.getValue();
         for (String value : values)
         {
//               System.out.println(String.format("setting %s = %s", header.getKey(), value));
            httpMethod.addHeader(header.getKey(), value);
         }
      }
   }

   public void close()
   {
      if (closed)
         return;

      if (createdHttpClient && httpClient != null)
      {
         ClientConnectionManager manager = httpClient.getConnectionManager();
         if (manager != null)
         {
            manager.shutdown();
         }
      }
      closed = true;
   }

   public boolean isClosed()
   {
      return closed;
   }

   public void finalize() throws Throwable
   {
      close();
      super.finalize();
   }

   /**
    * If passed httpMethod is of type HttpPost then obtain its entity. If the entity has an enclosing File then
    * delete it by invoking this method after the request has completed. The entity will have an enclosing File
    * only if it was too huge to fit into memory.
    *
    * @param httpMethod - the httpMethod to clean up.
    */
   protected void cleanUpAfterExecute(final HttpRequestBase httpMethod)
   {
      if (httpMethod != null && httpMethod instanceof HttpPost)
      {
         HttpPost postMethod = (HttpPost) httpMethod;
         HttpEntity entity = postMethod.getEntity();
         if (entity != null && entity instanceof FileExposingFileEntity)
         {
            File tempRequestFile = ((FileExposingFileEntity) entity).getFile();
            try
            {
               boolean isDeleted = tempRequestFile.delete();
               if (!isDeleted)
               {
                  handleFileNotDeletedError(tempRequestFile, null);
               }
            }
            catch (Exception ex)
            {
               handleFileNotDeletedError(tempRequestFile, ex);
            }
         }
      }
   }

   /**
    * Build the HttpEntity to be sent to the Service as part of (POST) request. Creates a off-memory
    * {@link FileExposingFileEntity} or a regular in-memory {@link ByteArrayEntity} depending on if the request
    * OutputStream fit into memory when built by calling.
    *
    * @param request -
    * @return - the built HttpEntity
    * @throws IOException -
    */
   protected HttpEntity buildEntity(final ClientInvocation request) throws IOException
   {
      HttpEntity entityToBuild = null;
      DeferredFileOutputStream memoryManagedOutStream = writeRequestBodyToOutputStream(request);

      if (memoryManagedOutStream.isInMemory())
      {
         ByteArrayEntity entityToBuildByteArray = new ByteArrayEntity(memoryManagedOutStream.getData());
         entityToBuildByteArray.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, request.getHeaders().getMediaType().toString()));
         entityToBuild = entityToBuildByteArray;
      }
      else
      {
         File requestBodyFile = memoryManagedOutStream.getFile();
         requestBodyFile.deleteOnExit();
         entityToBuild = new FileExposingFileEntity(memoryManagedOutStream.getFile(), request.getHeaders().getMediaType().toString());
      }

      return entityToBuild;
   }

   /**
    * Creates the request OutputStream, to be sent to the end Service invoked, as a
    * <a href="http://commons.apache.org/io/api-release/org/apache/commons/io/output/DeferredFileOutputStream.html"
    * >DeferredFileOutputStream</a>.
    *
    *
    * @param request -
    * @return - DeferredFileOutputStream with the ClientRequest written out per HTTP specification.
    * @throws IOException -
    */
   private DeferredFileOutputStream writeRequestBodyToOutputStream(final ClientInvocation request) throws IOException
   {
      DeferredFileOutputStream memoryManagedOutStream =
              new DeferredFileOutputStream(this.fileUploadInMemoryThresholdLimit * getMemoryUnitMultiplier(),
                      getTempfilePrefix(), ".tmp", this.fileUploadTempFileDir);
      request.getDelegatingOutputStream().setDelegate(memoryManagedOutStream);
      request.writeRequestBody(request.getEntityStream());
      memoryManagedOutStream.close();
      return memoryManagedOutStream;
   }

   /**
    * Use context information, which will include node name, to avoid conflicts in case of multiple VMS using same
    * temp directory location.
    *
    * @return -
    */
   protected String getTempfilePrefix()
   {
      return processId;
   }
   /**
    * @return - the constant to multiply {@link #fileUploadInMemoryThresholdLimit} with based on
    *         {@link #fileUploadMemoryUnit} enumeration value.
    */
   private int getMemoryUnitMultiplier()
   {
      switch (this.fileUploadMemoryUnit)
      {
         case BY:
            return 1;
         case KB:
            return 1024;
         case MB:
            return 1024 * 1024;
         case GB:
            return 1024 * 1024 * 1024;
      }
      return 1;
   }



   /**
    * Log that the file did not get deleted but prevent the request from failing by eating the exception. The file
    * has been registered to delete on exit, so it will get deleted eventually.
    *
    * @param tempRequestFile -
    * @param ex - a null may be passed in which case this param gets ignored.
    */
   private void handleFileNotDeletedError(File tempRequestFile, Exception ex)
   {
      logger.warn("Could not delete file' " + tempRequestFile.getAbsolutePath() + "' for request: ", ex);
   }




   /**
    * We use {@link ch.boye.httpclientandroidlib.entity.FileEntity} as the {@link HttpEntity} implementation when the request OutputStream has been
    * saved to a File on disk (because it was too large to fit into memory see however, we have to delete
    * the File supporting the <code>FileEntity</code>, otherwise the disk will soon run out of space - remember
    * that there can be very huge files, in GB range, processed on a regular basis - and FileEntity exposes its
    * content File as a protected field. For the enclosing parent class ( {@link ApacheHttpClient4Engine} ) to be
    * able to get a handle to this content File and delete it, this class expose the content File.<br>
    * This class is private scoped to prevent access to this content File outside of the parent class.
    *
    * @author <a href="mailto:stikoo@digitalriver.com">Sandeep Tikoo</a>
    */
   private static class FileExposingFileEntity extends FileEntity
   {
      /**
       * @param pFile -
       * @param pContentType -
       */
      public FileExposingFileEntity(File pFile, String pContentType)
      {
         super(pFile, pContentType);
      }

      /**
       * @return - the content File enclosed by this FileEntity.
       */
      File getFile()
      {
         return this.file;
      }
   }

}