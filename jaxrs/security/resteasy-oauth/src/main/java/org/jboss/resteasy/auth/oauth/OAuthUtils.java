package org.jboss.resteasy.auth.oauth;

import net.oauth.OAuth;
import net.oauth.OAuth.Parameter;
import net.oauth.OAuth.Problems;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import org.jboss.resteasy.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OAuthUtils {
	
	/**
	 * HTTP Authorization header
	 */
	public static final String AUTHORIZATION_HEADER = "Authorization";

	/**
	 * HTTP WWW-Authenticate header
	 */
	public static final String AUTHENTICATE_HEADER = "WWW-Authenticate";
	
	/**
	 * OAuth Verifier parameter
	 */
	public static final String OAUTH_VERIFIER_PARAM = "oauth_verifier";

	/**
	 * OAuth Callback Confirmed parameter
	 */
	public static final String OAUTH_CALLBACK_CONFIRMED_PARAM = "oauth_callback_confirmed";
	
	/**
	 * Name of the OAuthValidator Servlet Context Attribute name.
	 */
	private static final String ATTR_OAUTH_VALIDATOR = OAuthValidator.class.getName();

	/**
	 * Name of the OAuthProvider Servlet Context Attribute name.
	 */
	private static final String ATTR_OAUTH_PROVIDER = OAuthProvider.class.getName();

	private final static Logger logger = Logger.getLogger(OAuthUtils.class);

	/**
	 * Encodes the given value for use in an OAuth parameter
	 */
	public static String encodeForOAuth(String value){
		try {
			return URLUtils.encodePart(value, "UTF-8", URLUtils.UNRESERVED);
		} catch (UnsupportedEncodingException e) {
			// this encoding is specified in the JDK
			throw new RuntimeException("UTF8 encoding should be supported", e);
		}
	}
	
	/**
	 * Sends a list of OAuth parameters in the body of the given Http Servlet Response
	 * @param params a list of <name, value> parameters
	 */
	public static void sendValues(HttpServletResponse resp, String... params) throws IOException {
		PrintWriter writer = resp.getWriter();
		if((params.length % 2) != 0)
			throw new IllegalArgumentException("Arguments should be name=value*");
		for(int i=0;i<params.length;i+=2){
			if(i > 0)
				writer.append('&');
			writer.append(encodeForOAuth(params[i]));
			writer.append('=');
			writer.append(encodeForOAuth(params[i+1]));
		}
	}

	/**
	 * Reads an OAuthMessage from an HTTP Servlet Request. Uses the Authorization header, GET and POST parameters.
	 */
	public static OAuthMessage readMessage(HttpServletRequest req) {
		String authorizationHeader = req.getHeader(AUTHORIZATION_HEADER);
		Set<OAuth.Parameter> parameters = new HashSet<OAuth.Parameter>();
		
		// first read the Authorization header
		if(authorizationHeader != null){
			for(Parameter param : OAuthMessage.decodeAuthorization(authorizationHeader)){
				if(!"realm".equalsIgnoreCase(param.getValue()))
					parameters.add(param);
			}
		}
		// Read all parameters from either POST or the query String
		@SuppressWarnings("unchecked")
		List<String> parameterNames = Collections.<String>list(req.getParameterNames());
		for(String parameterName : parameterNames){
			for(String value : req.getParameterValues(parameterName)){
				logger.debug("Adding parameter "+parameterName+" => "+value);
				parameters.add(new OAuth.Parameter(parameterName, value));
			}
		}
		
		return new OAuthMessage(req.getMethod(), req.getRequestURL().toString(), parameters);

	}

	/**
	 * Sends an error to the OAuth Consumer
	 */
	public static void makeErrorResponse(HttpServletResponse resp, String message, int httpCode, OAuthProvider provider) throws IOException{
		logger.debug("Error ["+httpCode+"]: "+message);
		resp.getWriter().append(message);
		resp.setStatus(httpCode);
		String headerValue = "OAuth";
		if (provider.getRealm() != null && provider.getRealm().length() > 0) {
		    headerValue += (" realm=\"" + provider.getRealm() + "\"");
		}
		resp.setHeader(AUTHENTICATE_HEADER, headerValue);
	}

	/**
	 * Parse an OAuth timestamp
	 */
	public static long parseTimestamp(String timestampString) throws OAuthException {
		try{
			long timestamp = Long.parseLong(timestampString);
			if(timestamp > 0)
				return timestamp;
		}catch(NumberFormatException x){
			// fallback
		}
		throw new OAuthException(HttpURLConnection.HTTP_UNAUTHORIZED, "Invalid timestamp "+timestampString);
	}

	/**
	 * Finds the HTTP status code from the given exception
	 */
	public static int getHttpCode(OAuthProblemException x){
		Integer httpCode = Problems.TO_HTTP_CODE.get(x.getProblem());
		if(httpCode != null)
			return httpCode;
		return HttpURLConnection.HTTP_INTERNAL_ERROR;

	}
	
	/**
	 * Loads the OAuthProvider as specified in the Servlet Context parameters, and caches it in the Servlet Context attributes for reuse.
	 */
	public static OAuthProvider getOAuthProvider(ServletContext context) throws ServletException {
		OAuthProvider provider = (OAuthProvider) context.getAttribute(ATTR_OAUTH_PROVIDER);
		if(provider != null)
			return provider;
		
		String providerClassName = context.getInitParameter(OAuthServlet.PARAM_PROVIDER_CLASS);
		if(providerClassName == null)
			throw new ServletException(OAuthServlet.PARAM_PROVIDER_CLASS+" parameter required");
		try {
			logger.info("Loading OAuthProvider: "+ providerClassName);
			Class<?> providerClass = Class.forName(providerClassName);
			if(!OAuthProvider.class.isAssignableFrom(providerClass))
				throw new ServletException(OAuthServlet.PARAM_PROVIDER_CLASS+" class "+providerClassName+" must be an instance of OAuthProvider");
			provider = new OAuthProviderChecker((OAuthProvider) providerClass.newInstance());
			context.setAttribute(ATTR_OAUTH_PROVIDER, provider);
			return provider;
		} catch (ClassNotFoundException e) {
			throw new ServletException(OAuthServlet.PARAM_PROVIDER_CLASS+" class "+providerClassName+" not found");
		} catch (Exception e) {
			throw new ServletException(OAuthServlet.PARAM_PROVIDER_CLASS+" class "+providerClassName+" could not be instanciated", e);
		}
	}

	/**
	 * Creates an OAuthValidator, and caches it in the Servlet Context attributes for reuse.
	 */
	public static OAuthValidator getValidator(ServletContext context,
			OAuthProvider provider) {
		OAuthValidator validator = (OAuthValidator) context.getAttribute(ATTR_OAUTH_VALIDATOR);
		if(validator != null)
			return validator;
		
		validator = new OAuthValidator(provider);
		context.setAttribute(ATTR_OAUTH_VALIDATOR, validator);
		return validator;
	}
	
	public static void validateRequestWithAccessToken(
	        HttpServletRequest request,
            OAuthMessage message,
            OAuthToken accessToken,
            OAuthValidator validator,
            org.jboss.resteasy.auth.oauth.OAuthConsumer consumer) throws Exception {
	    
        OAuthConsumer _consumer = new OAuthConsumer(null, consumer.getKey(), accessToken.getConsumer().getSecret(), null);
        OAuthAccessor accessor = new OAuthAccessor(_consumer);
        accessor.accessToken = accessToken.getToken();
        accessor.tokenSecret = accessToken.getSecret();
        
        // validate the message
        validator.validateMessage(message, accessor, accessToken);
        if (!OAuthUtils.validateUriScopes(request.getRequestURL().toString(), accessToken.getScopes())) {
            throw new OAuthException(HttpURLConnection.HTTP_BAD_REQUEST, "Wrong URI Scope");
        }
	}
	
	/**
	 * Validates if a given request is a valid 2-leg oAuth request
	 */
	public static void validateRequestWithoutAccessToken(
	        HttpServletRequest request,
	        OAuthMessage message,
	        OAuthValidator validator,
	        org.jboss.resteasy.auth.oauth.OAuthConsumer consumer) throws Exception 
	{
	    
	    String[] scopes = consumer.getScopes();
        if (scopes == null || !validateUriScopes(request.getRequestURL().toString(), scopes)) {
            throw new OAuthException(HttpURLConnection.HTTP_BAD_REQUEST, "Wrong URI Scope");
        }
        // build some info for verification
        OAuthConsumer _consumer = new OAuthConsumer(null, consumer.getKey(), consumer.getSecret(), null);
        OAuthAccessor accessor = new OAuthAccessor(_consumer);
        // validate the message
        validator.validateMessage(message, accessor, null);
	}
	
	/**
	 * Validates if a current request URI matches URI provided by the consumer at the
	 * registration time or during the request token validation request 
	 */
	public static boolean validateUriScopes(String requestURI, String[] scopes) {
        if (scopes == null) {
            return true;
        }
        for (String scope : scopes) {
            if (requestURI.startsWith(scope)) {
                return true;
            }
        }
        return false; 
    }
}
