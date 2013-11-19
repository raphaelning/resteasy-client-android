package org.jboss.resteasy.plugins.providers.multipart;

import javax.ws.rs.core.MediaType;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface MultipartConstants
{
   /**
    * "multipart/mixed"
    */
   public final static String MULTIPART_MIXED = "multipart/mixed";
   /**
    * "multipart/mixed"
    */
   public final static MediaType MULTIPART_MIXED_TYPE = new MediaType("multipart", "mixed");

   /**
    * "multipart/related"
    */
   public final static String MULTIPART_RELATED = "multipart/related";
   /**
    * "multipart/related"
    */
   public final static MediaType MULTIPART_RELATED_TYPE = new MediaType("multipart", "related");

   /**
    * Default fallback of the HTTP 1.1 protocol.
    * <p/>
    * "text/plain; charset=ISO-8859-1"
    */
   public final static String TEXT_PLAIN_WITH_CHARSET_ISO_8859_1 = "text/plain; charset=ISO-8859-1";
   /**
    * Default fallback of the HTTP 1.1 protocol.
    * <p/>
    * "text/plain; charset=ISO-8859-1"
    */
   public final static MediaType TEXT_PLAIN_WITH_CHARSET_ISO_8859_1_TYPE = MediaType.valueOf(TEXT_PLAIN_WITH_CHARSET_ISO_8859_1);

   /**
    * Default fallback of MIME messages
    * <p/>
    * "text/plain; charset=us-ascii"
    */
   public final static String TEXT_PLAIN_WITH_CHARSET_US_ASCII = "text/plain; charset=us-ascii";
   /**
    * Default fallback of MIME messages
    * <p/>
    * "text/plain; charset=us-ascii"
    */
   public final static MediaType TEXT_PLAIN_WITH_CHARSET_US_ASCII_TYPE = MediaType.valueOf(TEXT_PLAIN_WITH_CHARSET_US_ASCII);

   /**
    * "application/xop+xml"
    */
   public final static String APPLICATION_XOP_XML = "application/xop+xml";
   /**
    * "application/xop+xml"
    */
   public final static MediaType APPLICATION_XOP_XML_TYPE = new MediaType("application", "xop+xml");
}
