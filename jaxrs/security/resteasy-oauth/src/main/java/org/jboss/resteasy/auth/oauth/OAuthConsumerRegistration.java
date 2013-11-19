package org.jboss.resteasy.auth.oauth;

/**
 * Registration of OAuth consumers
 * 
 */
public interface OAuthConsumerRegistration {
		
	/**
     * Creates a new OAuth Consumer
     * @param consumerKey the Consumer key.
     * @return consumer secret.
     * @throws OAuthException thrown if Consumer can not be registered.
     */
    public OAuthConsumer registerConsumer(String consumerKey, 
            String displayName, String connectURI) throws OAuthException;

    
    /**
     * Registers Consumer Scopes
     * @param consumerKey the Consumer key.
     * @return scopes the consumer scopes
     * @throws OAuthException thrown if scopes can not be registered.
     */
    public void registerConsumerScopes(String consumerKey, 
            String[] scopes) throws OAuthException;
    
    /**
     * Registers Consumer Permissions
     * @param consumerKey the Consumer key.
     * @return scopes the consumer permissions
     * @throws OAuthException thrown if permissions can not be registered.
     */
    public void registerConsumerPermissions(String consumerKey, 
                String[] permissions) throws OAuthException;
        
}
