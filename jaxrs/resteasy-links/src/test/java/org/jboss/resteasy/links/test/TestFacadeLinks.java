package org.jboss.resteasy.links.test;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.links.RESTServiceDiscovery;
import org.jboss.resteasy.links.RESTServiceDiscovery.AtomLink;
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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.jboss.resteasy.test.TestPortProvider.generateBaseUrl;

@RunWith(Parameterized.class)
public class TestFacadeLinks
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
		return Arrays.asList(new Class<?>[]{BookStore.class}, new Class<?>[]{BookStoreMinimal.class});
	}

	private Class<?> resourceType;
	private String url;
	private BookStoreService client;
	private HttpClient httpClient;
	
	public TestFacadeLinks(Class<?> resourceType){
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
	public void testLinks() throws Exception
	{
		ScrollableCollection comments = client.getScrollableCommentsXML("foo");
		checkCommentsLinks(url, comments);
		comments = client.getScrollableCommentsJSON("foo");
		checkCommentsLinks(url, comments);
	}

	private void checkCommentsLinks(String url, ScrollableCollection comments) {
		Assert.assertNotNull(comments);
		RESTServiceDiscovery links = comments.getRest();
		Assert.assertNotNull(links);
		Assert.assertEquals(3, links.size());
		// list
		AtomLink atomLink = links.getLinkForRel("list");
		Assert.assertNotNull(atomLink);
		Assert.assertEquals(url+"/book/foo/comments", atomLink.getHref());
		// add
		atomLink = links.getLinkForRel("add");
		Assert.assertNotNull(atomLink);
		Assert.assertEquals(url+"/book/foo/comments", atomLink.getHref());
		// comment collection
		atomLink = links.getLinkForRel("collection");
		Assert.assertNotNull(atomLink);
		Assert.assertEquals(url+"/book/foo/comment-collection", atomLink.getHref());
	}

	private void checkCommentLinks(String url, Comment comment) {
		Assert.assertNotNull(comment);
		Assert.assertEquals(0, comment.getId());
		RESTServiceDiscovery links = comment.getRest();
		Assert.assertNotNull(links);
		Assert.assertEquals(6, links.size());
		// self
		AtomLink atomLink = links.getLinkForRel("self");
		Assert.assertNotNull(atomLink);
		Assert.assertEquals(url+"/book/foo/comment/0", atomLink.getHref());
		// update
		atomLink = links.getLinkForRel("update");
		Assert.assertNotNull(atomLink);
		Assert.assertEquals(url+"/book/foo/comment/0", atomLink.getHref());
		// remove
		atomLink = links.getLinkForRel("remove");
		Assert.assertNotNull(atomLink);
		Assert.assertEquals(url+"/book/foo/comment/0", atomLink.getHref());
		// list
		atomLink = links.getLinkForRel("list");
		Assert.assertNotNull(atomLink);
		Assert.assertEquals(url+"/book/foo/comments", atomLink.getHref());
		// add
		atomLink = links.getLinkForRel("add");
		Assert.assertNotNull(atomLink);
		Assert.assertEquals(url+"/book/foo/comments", atomLink.getHref());
		// collection
		atomLink = links.getLinkForRel("collection");
		Assert.assertNotNull(atomLink);
		Assert.assertEquals(url+"/book/foo/comment-collection", atomLink.getHref());
	}
}