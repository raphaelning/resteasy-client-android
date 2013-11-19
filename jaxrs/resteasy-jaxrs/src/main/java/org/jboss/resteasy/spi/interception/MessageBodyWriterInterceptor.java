package org.jboss.resteasy.spi.interception;

import javax.ws.rs.WebApplicationException;
import java.io.IOException;

/**
 * Wraps around invocations of MessageBodyWriter.writeTo()
 *
 * @deprecated
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Deprecated
public interface MessageBodyWriterInterceptor
{
   void write(MessageBodyWriterContext context) throws IOException, WebApplicationException;

}
