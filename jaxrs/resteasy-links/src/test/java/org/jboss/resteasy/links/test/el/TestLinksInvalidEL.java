package org.jboss.resteasy.links.test.el;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.plugins.server.resourcefactory.POJOResourceFactory;
import org.jboss.resteasy.test.EmbeddedContainer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.jboss.resteasy.test.TestPortProvider.generateBaseUrl;

@RunWith(Parameterized.class)
public class TestLinksInvalidEL
{

	private static Dispatcher dispatcher;

	@BeforeClass
	public static void beforeClass() throws Exception
	{
		dispatcher = EmbeddedContainer.start().getDispatcher();
	}

	@AfterClass
	public static void afterClass() throws Exception
	{
		EmbeddedContainer.stop();
	}

	@Parameters
	public static List<Class<?>[]> getParameters(){
		List<Class<?>[]> classes = new ArrayList<Class<?>[]>();
		classes.add(new Class<?>[]{BookStoreInvalidEL.class});
		return classes;
	}

	private Class<?> resourceType;
	private String url;
	private BookStoreService client;
	private HttpClient httpClient;
	
	public TestLinksInvalidEL(Class<?> resourceType){
		this.resourceType = resourceType;
	}
	
	@Before
	public void before(){
		POJOResourceFactory noDefaults = new POJOResourceFactory(resourceType);
		dispatcher.getRegistry().addResourceFactory(noDefaults);
		httpClient = new DefaultHttpClient();
		ApacheHttpClient4Executor executor = new ApacheHttpClient4Executor(httpClient);
		url = generateBaseUrl();
		client = ProxyFactory.create(BookStoreService.class, url,
					executor);
	}

	@After
	public void after(){
		// TJWS does not support chunk encodings well so I need to kill kept
		// alive connections
		httpClient.getConnectionManager().closeIdleConnections(0, TimeUnit.MILLISECONDS);
		dispatcher.getRegistry().removeRegistrations(resourceType);
	}
	
	@Test
	public void testELWorksWithoutPackageXML() throws Exception
	{
		try{
			client.getBookXML("foo");
			Assert.fail("This should have caused a 500");
		}catch(ClientResponseFailure x){
			System.err.println("Failure is "+x.getResponse().getEntity(String.class));
			Assert.assertEquals(500, x.getResponse().getStatus());
		}
	}
	@Test
	public void testELWorksWithoutPackageJSON() throws Exception
	{
		try{
			client.getBookJSON("foo");
			Assert.fail("This should have caused a 500");
		}catch(ClientResponseFailure x){
			System.err.println("Failure is "+x.getResponse().getEntity(String.class));
			Assert.assertEquals(500, x.getResponse().getStatus());
		}
	}
}