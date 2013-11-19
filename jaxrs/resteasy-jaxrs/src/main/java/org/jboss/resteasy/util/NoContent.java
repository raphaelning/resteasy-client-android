package org.jboss.resteasy.util;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NoContentException;
import java.io.InputStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class NoContent
{
   public static InputStream noContentCheck( MultivaluedMap httpHeaders, InputStream is) throws NoContentException
   {
      contentLengthCheck(httpHeaders);
      NoContentInputStreamDelegate delegate = new NoContentInputStreamDelegate(is);
      return delegate;

   }

   public static boolean isContentLengthZero(MultivaluedMap httpHeaders)
   {
      if (httpHeaders == null) return false;
      String contentLength = (String)httpHeaders.getFirst(HttpHeaders.CONTENT_LENGTH);
      if (contentLength != null)
      {
         int length = Integer.parseInt(contentLength);
         if (length == 0) return true;
      }
      return false;
   }

   public static void contentLengthCheck(MultivaluedMap httpHeaders) throws NoContentException
   {
      if (isContentLengthZero(httpHeaders)) throw new NoContentException("No content.  Content-Length is 0");
   }
}
