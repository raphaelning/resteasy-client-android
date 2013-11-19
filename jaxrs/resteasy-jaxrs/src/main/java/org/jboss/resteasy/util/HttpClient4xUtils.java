package org.jboss.resteasy.util;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.util.EntityUtils;
import org.jboss.resteasy.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * HttpClient4xUtils provides utility methods useful for changes
 * necessitated by switching from HttpClient 3.x to HttpClient 4.x.
 *
 * Modified for Android by Raphael Ning.
 * 
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1 $
 */
public class HttpClient4xUtils
{
   private final static Logger log = Logger.getLogger(HttpClient4xUtils.class);

   static public void consumeEntity(HttpResponse response)
   {
      try
      {
         EntityUtils.consume(response.getEntity());
      } catch (IOException e)
      {
         log.info("unable to close entity stream", e);
      }
   }
   
   static public String updateQuery(String uriString, String query)
   {
      try
      {
         URI uri = new URI(uriString);
         return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), query, uri.getFragment()).toString();
      } catch (URISyntaxException e)
      {
         throw new RuntimeException(e);
      }
   }
}
