package org.jboss.resteasy.plugins.providers.jaxb.fastinfoset;

import org.jboss.resteasy.plugins.providers.jaxb.JAXBXmlSeeAlsoProvider;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Provider
@Consumes("application/*+fastinfoset")
@Produces("application/*+fastinfoset")
public class FastinfoSetXmlSeeAlsoProvider extends JAXBXmlSeeAlsoProvider
{
   @Override
   protected boolean suppressExpandEntityExpansion()
   {
      return false;
   }
}
