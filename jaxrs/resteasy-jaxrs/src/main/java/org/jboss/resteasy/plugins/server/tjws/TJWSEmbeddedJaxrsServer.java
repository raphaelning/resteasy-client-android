package org.jboss.resteasy.plugins.server.tjws;

import Acme.Serve.Serve;
import org.jboss.resteasy.plugins.server.embedded.EmbeddedJaxrsServer;
import org.jboss.resteasy.plugins.server.embedded.SecurityDomain;
import org.jboss.resteasy.spi.ResteasyDeployment;

import java.util.Hashtable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TJWSEmbeddedJaxrsServer extends TJWSServletServer implements EmbeddedJaxrsServer
{
   protected ResteasyDeployment deployment = new ResteasyDeployment();
   protected TJWSServletDispatcher servlet = new TJWSServletDispatcher();

   protected String rootResourcePath = "";
   protected Hashtable<String,String> initParameters;
   protected Hashtable<String,String> contextParameters;

   public void setRootResourcePath(String rootResourcePath)
   {
      this.rootResourcePath = rootResourcePath;
   }

   public TJWSEmbeddedJaxrsServer()
   {
   }

   public ResteasyDeployment getDeployment()
   {
      return deployment;
   }

   public void setDeployment(ResteasyDeployment deployment)
   {
      this.deployment = deployment;
   }

   public void setInitParameters(Hashtable<String,String> initParameters)
   {
      this.initParameters = initParameters;
   }
   
   public void setContextParameters(Hashtable<String,String> contextParameters)
   {
      this.contextParameters = contextParameters;
   }

   @Override
   public void start()
   {
      server.setAttribute(ResteasyDeployment.class.getName(), deployment);
      addServlet(rootResourcePath, servlet, initParameters, contextParameters);
      servlet.setContextPath(rootResourcePath);
      super.start();
   }

   public void setSecurityDomain(SecurityDomain sc)
   {
      servlet.setSecurityDomain(sc);
   }

   public String getProperty(String key)
   {
      return props.getProperty(key);
   }

   public String getPort()
   {
      return getProperty(Serve.ARG_PORT);
   }
}
