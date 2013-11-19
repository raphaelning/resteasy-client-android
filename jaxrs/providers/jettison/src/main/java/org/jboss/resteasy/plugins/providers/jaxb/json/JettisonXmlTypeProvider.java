package org.jboss.resteasy.plugins.providers.jaxb.json;

import org.jboss.resteasy.plugins.providers.jaxb.JAXBXmlTypeProvider;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Provider
@Produces("application/*+json")
@Consumes("application/*+json")
public class JettisonXmlTypeProvider extends JAXBXmlTypeProvider
{
   @Override
   protected boolean suppressExpandEntityExpansion()
   {
      return false;
   }
}
