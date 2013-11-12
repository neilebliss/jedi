package org.devnull.jedi;

import org.devnull.jedi.configs.JediConfig;
import org.devnull.jedi.mock.*;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.AssertJUnit.*;

public class RestClientTest
{
	protected static Logger log = null;

	@BeforeMethod
	public void setUp() throws Exception
	{
		Properties logProperties = new Properties();

		logProperties.put("log4j.rootLogger", "DEBUG, stdout");
		logProperties.put("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
		logProperties.put("log4j.appender.stdout.layout", "org.apache.log4j.EnhancedPatternLayout");
		logProperties.put("log4j.appender.stdout.layout.ConversionPattern", "%d [%F:%L] [%p] %C: %m%n");
		logProperties.put("log4j.appender.stdout.layout.ConversionPattern", "%d [%p] %C: %m%n");
		logProperties.put("log4j.appender.stdout.immediateFlush", "true");
		logProperties.put("log4j.category.org.apache.http.wire", "INFO, stdout");
		logProperties.put("log4j.additivity.org.apache.http.wire", false);

		BasicConfigurator.resetConfiguration();
		PropertyConfigurator.configure(logProperties);

		log = Logger.getLogger(RestClientTest.class);
	}

	@Test
	public void testConstructor() throws Exception
	{
		RestClient client = null;

		try
		{
			client = new RestClient(null);
			assertTrue(false);
		}
		catch (Exception e)
		{
			assertTrue(e.getMessage(), e.getMessage().equals("config argument is null"));
		}

		try
		{
			client = new RestClient(new JediConfig());
			assertNotNull(client);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			assertTrue(e.getMessage(), false);
		}
	}

	@Test
	public void testSetHostname() throws Exception
	{
		try
		{
			RestClient client = new RestClient(new JediConfig());
			client.setHostname("foobarbaz");
			assertTrue(client.getHostname(), "foobarbaz".equals(client.getHostname()));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			assertTrue(e.getMessage(), false);
		}
	}

	@Test
	public void testCall() throws Exception
	{
		RestClient client = null;

		/**
		 * test 1: expect NPE because hostname is not set
		 */

		try
		{
			log.debug("testing failure when no hostname is set");
			client = new RestClient(new JediConfig());
			DNSRecord r = client.call();
			assertTrue(false);
		}
		catch (NullPointerException npe)
		{
			assertTrue(true);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			assertTrue(e.getMessage(), false);
		}

		/**
		 * test 2: expect null record because there's no server to talk to on the default host/port
		 */
		try
		{
			log.info("testing against a server that is rejecting connections");
			client.setHostname("foo.bar.baz");
			DNSRecord r = client.call();
			assertNull(r);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			assertTrue(e.getMessage(), false);
		}

		/**
		 * test 3: set up a mock server that does not support authentication
		 */
		JediConfig config = new JediConfig();
		MockAPIServer mock = null;
		client = new RestClient(config);
		client.setHostname("foo.bar.baz");

		try
		{
			log.info("testing against an API server that doesn't support authentication");
			mock = new MockAPIServer(new HelloServlet(), false, null, null);
			DNSRecord r = client.call();
			assertNull(r);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			assertTrue(e.getMessage(), false);
		}
		finally
		{
			mock.shutdown();
		}

		/**
		 * test 4: set up a mock server that has wrong authentication
		 */

		try
		{
			log.info("testing against an API server with mismatched authentication");
			mock = new MockAPIServer(new HelloServlet(), true, "bar", "foo");
			DNSRecord r = client.call();
			assertNull(r);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			assertTrue(e.getMessage(), false);
		}
		finally
		{
			mock.shutdown();
		}

		/**
		 * test 5: set up a mock server that has correct auth but gives a non-200 reply
		 */

		try
		{
			log.info("testing against an API server that returns 404");
			mock = new MockAPIServer(new BadReplyServlet(), true, "foo", "bar");
			DNSRecord r = client.call();
			assertNull(r);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			assertTrue(e.getMessage(), false);
		}
		finally
		{
			mock.shutdown();
		}

		/**
		 * test 6: set up a mock server that gives a 200 reply but an empty response body
		 */

		try
		{
			log.info("testing against an API sever that gives an empty response");
			mock = new MockAPIServer(new EmptyReplyServlet(), true, "foo", "bar");
			DNSRecord r = client.call();
			assertNull(r);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			assertTrue(e.getMessage(), false);
		}
		finally
		{
			mock.shutdown();
		}

		/**
		 * test 7: set up a mock server that gives a reply that is too long
		 */

		try
		{
			log.info("testing against an API server that gives a too long reply");
			mock = new MockAPIServer(new TooLongReplyServlet(), true, "foo", "bar");
			DNSRecord r = client.call();
			assertNull(r);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			assertTrue(e.getMessage(), false);
		}
		finally
		{
			mock.shutdown();
		}

		/**
		 * test 8: set up a mock server that gives a reply that does not parse correctly
		 */

		try
		{
			log.info("testing against a non-json reply");
			mock = new MockAPIServer(new HelloServlet(), true, "foo", "bar");
			DNSRecord r = client.call();
			assertNull(r);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			assertTrue(e.getMessage(), false);
		}
		finally
		{
			mock.shutdown();
		}

		/**
		 * test 9: set up a mock server that gives a reply that does parse correctly, expect the correct
		 * 	DNSRecord with a relatively current timestamp.
		 */
		try
		{
			log.info("testing against a good reply");
			mock = new MockAPIServer(new GoodReplyServlet(), true, "foo", "bar");
			DNSRecord r = client.call();
			assertNotNull(r);
			assertTrue(r.toString(), r.getTTL() == 100);
			assertNotNull(r.getRecords());
			List<IPRecord> ips = r.getRecords();
			assertTrue(r.toString(), ips.size() == 2);
			assertTrue(r.toString(), ips.get(0).getType().equals("A"));
			assertTrue(r.toString(), ips.get(0).getAddress().equals("1.1.1.1"));
			assertTrue(r.toString(), ips.get(1).getType().equals("AAAA"));
			assertTrue(r.toString(), ips.get(1).getAddress().equals("2001:fefe"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			assertTrue(e.getMessage(), false);
		}
		finally
		{
			mock.shutdown();
		}
	}
}