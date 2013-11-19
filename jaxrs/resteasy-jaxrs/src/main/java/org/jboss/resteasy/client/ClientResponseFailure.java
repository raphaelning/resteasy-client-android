package org.jboss.resteasy.client;

import org.jboss.resteasy.client.core.BaseClientResponse;

/**
 * @deprecated
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Deprecated
public class ClientResponseFailure extends RuntimeException
{
   private static final long serialVersionUID = 7491381058971118249L;
   private ClientResponse response;

   public ClientResponseFailure(ClientResponse response)
   {
      super("Failed with status: " + response.getStatus());
      this.response = BaseClientResponse.copyFromError(response);
      // release connection just in case it doesn't get garbage collected or manually released
      response.releaseConnection();
   }

   public ClientResponseFailure(String s, ClientResponse response)
   {
      super(s);
      this.response = BaseClientResponse.copyFromError(response);
      // release the connection because we don't trust users to catch and clean up
      response.releaseConnection();
   }

   public ClientResponseFailure(String s, Throwable throwable, ClientResponse response)
   {
      super(s, throwable);
      this.response = BaseClientResponse.copyFromError(response);
      // release the connection because we don't trust users to catch and clean up
      response.releaseConnection();
   }

   public ClientResponseFailure(Throwable throwable, ClientResponse response)
   {
      super(throwable);
      this.response = BaseClientResponse.copyFromError(response);
      // release the connection because we don't trust users to catch and clean up
      response.releaseConnection();
   }

   public ClientResponse getResponse()
   {
      return response;
   }
}
