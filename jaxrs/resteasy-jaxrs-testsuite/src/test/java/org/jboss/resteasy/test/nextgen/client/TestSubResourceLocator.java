package org.jboss.resteasy.test.nextgen.client;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.test.BaseResourceTest;
import org.jboss.resteasy.test.TestPortProvider;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

public class TestSubResourceLocator extends BaseResourceTest
{
	public static interface Book
	{
		@GET
		@Path("/title")
		@Produces("text/plain")
		String getTitle();

		@Path("/ch/{number}")
		Chapter getChapter(@PathParam("number") int number);
	}

	@Path("/gulliverstravels")
	public static class BookImpl implements Book
	{
		public String getTitle()
		{
			return "Gulliver's Travels";
		}
		
		@Override
		public Chapter getChapter(int number)
		{
			return new ChapterImpl(number);
		}
	}

	public static interface Chapter
	{
		@GET
		@Path("title")
		@Produces("text/plain")
		String getTitle();

		@GET
		@Path("body")
		@Produces("text/plain")
		String getBody();
	}

	public static class ChapterImpl implements Chapter
	{
		private final int number;

		public ChapterImpl(int number)
		{
			this.number = number;
		}

		public String getTitle()
		{
			return "Chapter " + number;
		}

		public String getBody()
		{
			return "This is the content of chapter " + number + ".";
		}
	}

   static ResteasyClient client;

   @BeforeClass
   public static void setUp() throws Exception
   {
      addPerRequestResource(BookImpl.class);
      client = new ResteasyClientBuilder().build();
   }

   @AfterClass
   public static void shutdown() throws Exception
   {
      client.close();
   }

   @Test
	public void testSubresourceProxy() throws Exception
	{
      ResteasyWebTarget target = client.target(TestPortProvider.generateURL("/gulliverstravels"));
		Book book = target.proxy(Book.class);
		
		Assert.assertEquals("Gulliver's Travels", book.getTitle());
		
		Chapter ch1 = book.getChapter(1);
		Assert.assertEquals("Chapter 1", ch1.getTitle());
		
		Chapter ch2 = book.getChapter(2);
		Assert.assertEquals("Chapter 2", ch2.getTitle());
	}
}
