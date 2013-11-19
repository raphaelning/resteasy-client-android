package org.jboss.resteasy.test.providers.multipart;

import junit.framework.Assert;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;
import org.jboss.resteasy.test.BaseResourceTest;
import org.jboss.resteasy.test.TestPortProvider;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

/**
 * @author Attila Kiraly
 */
public class InputPartDefaultContentTypeEncodingOverwriteTest extends
		BaseResourceTest {
	protected static final String TEXT_PLAIN_WITH_CHARSET_UTF_8 = "text/plain; charset=utf-8";

	@Path("/mime")
	public static class MyService {

		@POST
		@Consumes(MediaType.MULTIPART_FORM_DATA)
		@Produces(MediaType.TEXT_PLAIN)
		public String sendDefaultContentType(MultipartInput input) {
			return input.getParts().get(0).getMediaType().toString();
		}
	}

	@Provider
	@ServerInterceptor
	public static class ContentTypeSetterPreProcessorInterceptor implements
			PreProcessInterceptor {

		public ServerResponse preProcess(HttpRequest request,
				ResourceMethodInvoker method) throws Failure, WebApplicationException {
			request.setAttribute(InputPart.DEFAULT_CONTENT_TYPE_PROPERTY,
					TEXT_PLAIN_WITH_CHARSET_UTF_8);
			return null;
		}

	}

	@Before
	public void setUp() throws Exception {
		dispatcher.getRegistry().addPerRequestResource(MyService.class);
		dispatcher.getProviderFactory().registerProvider(
				ContentTypeSetterPreProcessorInterceptor.class, false);
	}

	private static final String TEST_URI = TestPortProvider.generateURL("");

	@Test
	public void testContentType() throws Exception {
		String message = "--boo\r\n"
				+ "Content-Disposition: form-data; name=\"foo\"\r\n"
				+ "Content-Transfer-Encoding: 8bit\r\n\r\n" + "bar\r\n"
				+ "--boo--\r\n";
		
		ClientRequest request = new ClientRequest(TEST_URI + "/mime");
		request.body("multipart/form-data; boundary=boo", message.getBytes());
		ClientResponse<String> response = request.post(String.class);
		Assert.assertEquals("Status code is wrong.", 20, response.getStatus() / 10);
		Assert.assertEquals("Response text is wrong", 
		                    MediaType.valueOf(TEXT_PLAIN_WITH_CHARSET_UTF_8),
		                    MediaType.valueOf(response.getEntity()));
	}
}
