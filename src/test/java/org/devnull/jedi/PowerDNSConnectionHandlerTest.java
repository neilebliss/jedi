package org.devnull.jedi;

import com.google.common.cache.CacheBuilder;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.devnull.jedi.mock.*;
import org.devnull.jedi.configs.*;

import static org.testng.AssertJUnit.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import com.google.common.cache.Cache;

import org.devnull.statsd_client.*;

public class PowerDNSConnectionHandlerTest extends JsonBase
{
	private static Logger log = Logger.getLogger(PowerDNSConnectionHandlerTest.class);
	private static final StatsObject so = StatsObject.getInstance();

	@BeforeMethod
	public void setUp() throws Exception
	{
		Properties logProperties = new Properties();

		logProperties.put("log4j.rootLogger", "DEBUG, stdout");
		logProperties.put("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
		logProperties.put("log4j.appender.stdout.layout", "org.apache.log4j.EnhancedPatternLayout");
		logProperties.put("log4j.appender.stdout.layout.ConversionPattern", "%d [%F:%L] [%p] %C: %m%n");
		logProperties.put("log4j.appender.stdout.immediateFlush", "true");

		BasicConfigurator.resetConfiguration();
		PropertyConfigurator.configure(logProperties);
	}

	private Socket socket = null;
	private MockAPIServer mock = null;
	protected ThreadPoolExecutor apiPool = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1));
	protected ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1));
	protected Cache<String, DNSRecord> cache = CacheBuilder.newBuilder().maximumSize(1).build();
	protected JediConfig config = new JediConfig();

	@Test
	public void testRun() throws Exception
	{
		//
		// not using ExecutorService here because I want to be able to fetch info about the TPEs
		//
		// ExecutorService apiPool = Executors.newFixedThreadPool(1);
		//

		/**
		 * test 1: if socket is closed, connection handler should join immediately
		 */

		config.cache_timeout = 1;
		Thread t = null;

		try
		{
			socket = new Socket();
			socket.close();
			t = new Thread(new PowerDNSConnectionHandler(socket, config, apiPool, cache));
			t.start();
			t.join(100);
			assertTrue(!t.isAlive());
		}
		catch (Exception e)
		{
			assertTrue(e.getMessage(), false);
			e.printStackTrace();
		}
		finally
		{
			t = null;
			socket = null;
		}

		/**
		 * test 2: invalid request format results in a closed socket and immediate join
		 */

		try
		{
			socket = new Socket();
			t = new Thread()
			{
				public void run()
				{
					ServerSocket server = null;

					try
					{
						log.debug("starting server on port 5353");
						server = new ServerSocket();
						server.bind(new InetSocketAddress("localhost", 5353));
						log.debug("listening for a connection");
						Socket accepted = server.accept();
						log.debug("got a connection, starting handler");
						Thread p = new Thread(new PowerDNSConnectionHandler(accepted, config, apiPool, cache));
						p.start();
						log.debug("waiting to join handler");
						p.join();
						log.debug("handler joined");
					}
					catch (Exception e)
					{
						assertTrue(e.getMessage(), false);
						e.printStackTrace();
					}
					finally
					{
						try
						{
							if (server != null)
							{
								log.debug("closing server on port 5353");
								server.close();
							}
						}
						catch (IOException e)
						{
						}
					}
				}
			};
			t.start();
			Thread.sleep(200);
			log.debug("connecting as a client");
			socket.connect(new InetSocketAddress("localhost", 5353));
			log.debug("connected, writing foo to it");
			socket.getOutputStream().write("foo\n".getBytes());
			log.debug("wrote foo, giving it a second to join");
			t.join(1000);
			assertTrue(!t.isAlive());
		}
		catch (Exception e)
		{
			assertTrue(e.getMessage(), false);
			e.printStackTrace();
		}
		finally
		{
			t = null;
			socket = null;
		}

		/**
		 * test: invalid request (method is not initialize or lookup) results in closed socket, immediate join
		 */

		try
		{
			socket = new Socket();
			t = new Thread()
			{
				public void run()
				{
					ServerSocket server = null;

					try
					{
						log.debug("starting server on port 5353");
						server = new ServerSocket();
						server.bind(new InetSocketAddress("localhost", 5353));
						log.debug("listening for a connection");
						Socket accepted = server.accept();
						log.debug("got a connection, starting handler");
						Thread p = new Thread(new PowerDNSConnectionHandler(accepted, config, apiPool, cache));
						p.start();
						log.debug("waiting to join handler");
						p.join();
						log.debug("handler joined");
					}
					catch (Exception e)
					{
						assertTrue(e.getMessage(), false);
						e.printStackTrace();
					}
					finally
					{
						try
						{
							if (server != null)
							{
								log.debug("closing server on port 5353");
								server.close();
							}
						}
						catch (IOException e)
						{
						}
					}
				}
			};
			t.start();
			Thread.sleep(200);
			log.debug("connecting as a client");
			socket.connect(new InetSocketAddress("localhost", 5353));
			log.debug("connected, writing foo to it");
			socket.getOutputStream().write("{\"method\":\"foo\"}\n".getBytes());
			log.debug("wrote foo, giving it a second to join");
			t.join(1000);
			assertTrue(!t.isAlive());
		}
		catch (Exception e)
		{
			assertTrue(e.getMessage(), false);
			e.printStackTrace();
		}
		finally
		{
			t = null;
			socket = null;
		}

		/**
		 * test: if request method is initialize, that it writes result: true to socket
		 */

		try
		{
			socket = new Socket();
			t = new Thread()
			{
				public void run()
				{
					ServerSocket server = null;

					try
					{
						log.debug("starting server on port 5353");
						server = new ServerSocket();
						server.bind(new InetSocketAddress("localhost", 5353));
						log.debug("listening for a connection");
						Socket accepted = server.accept();
						log.debug("got a connection, starting handler");
						Thread p = new Thread(new PowerDNSConnectionHandler(accepted, config, apiPool, cache));
						p.start();
						log.debug("waiting to join handler");
						p.join();
						log.debug("handler joined");
					}
					catch (Exception e)
					{
						assertTrue(e.getMessage(), false);
						e.printStackTrace();
					}
					finally
					{
						try
						{
							if (server != null)
							{
								log.debug("closing server on port 5353");
								server.close();
							}
						}
						catch (IOException e)
						{
						}
					}
				}
			};
			t.start();
			Thread.sleep(200);
			log.debug("connecting as a client");
			socket.connect(new InetSocketAddress("localhost", 5353));
			log.debug("connected, sending initialization request");
			socket.getOutputStream().write("{\"method\":\"initialize\"}\n".getBytes());
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String line = reader.readLine().trim();
			log.debug("read line from server: " + line);
			assertTrue(line, "{\"result\":true}".equals(line));
			socket.close();
			log.debug("wrote foo, giving it a second to join");
			t.join(1000);
			assertTrue(!t.isAlive());
		}
		catch (Exception e)
		{
			assertTrue(e.getMessage(), false);
			e.printStackTrace();
		}
		finally
		{
			t = null;
			socket = null;
		}

		/**
		 * Have a group of tests all at once:
		 *
		 * In order to do this, many of the tests are passive.  We check the contents of the global/singleton
		 * StatsObject contents to make sure things went as planned.
		 *
		 * create Mock API server
		 * test: make sure it fetches replies from the API server correctly
		 * test: make sure replies from API server are served out of cache immediately after
		 * test: ability to send and receive query/response many times from socket with cache and cache expires,
		 * and make sure there are no negative side-effects from the way we handle socket reads/writes/state.
		 * test: make sure if something is cached and is too old it is not served
		 *
		 */
		mock = new MockAPIServer(new GoodReplyServlet(), true, "foo", "bar");
		so.clear();

		try
		{
			socket = new Socket();
			t = new Thread()
			{
				public void run()
				{
					ServerSocket server = null;

					try
					{
						log.debug("starting server on port 5353");
						server = new ServerSocket();
						server.bind(new InetSocketAddress("localhost", 5353));
						log.debug("listening for a connection");
						Socket accepted = server.accept();
						log.debug("got a connection, starting handler");
						Thread p = new Thread(new PowerDNSConnectionHandler(accepted, config, apiPool, cache));
						p.start();
						log.debug("waiting to join handler");
						p.join();
						log.debug("handler joined");
					}
					catch (Exception e)
					{
						assertTrue(e.getMessage(), false);
						e.printStackTrace();
					}
					finally
					{
						try
						{
							if (server != null)
							{
								log.debug("closing server on port 5353");
								server.close();
							}
						}
						catch (IOException e)
						{
						}
					}
				}
			};
			t.start();
			Thread.sleep(100);
			log.debug("connecting as a client");
			socket.connect(new InetSocketAddress("localhost", 5353));
			log.debug("connected, sending lookup request");

			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			for (int i = 0; i < 10; i++)
			{
				socket.getOutputStream().write("{\"method\":\"lookup\", \"parameters\":{\"qtype\":\"ANY\",\"qname\":\"foo.bar.baz\"}}\n".getBytes());
				String line = reader.readLine().trim();
				assertTrue(line, "{\"result\":[{\"qname\":\"foo.bar.baz\",\"qtype\":\"A\",\"content\":\"1.1.1.1\",\"ttl\":100,\"priority\":0,\"auth\":1},{\"qname\":\"foo.bar.baz\",\"qtype\":\"AAAA\",\"content\":\"2001:fefe\",\"ttl\":100,\"priority\":0,\"auth\":1}]}".equals(line));
			}

			//
			// sleep 2 seconds to give entries in the cache time to expire
			//
			Thread.sleep(2000);

			for (int i = 0; i < 10; i++)
			{
				socket.getOutputStream().write("{\"method\":\"lookup\", \"parameters\":{\"qtype\":\"ANY\",\"qname\":\"foo.bar.baz\"}}\n".getBytes());
				String line = reader.readLine().trim();
				log.debug("read line from server: " + line);
				assertTrue(line, "{\"result\":[{\"qname\":\"foo.bar.baz\",\"qtype\":\"A\",\"content\":\"1.1.1.1\",\"ttl\":100,\"priority\":0,\"auth\":1},{\"qname\":\"foo.bar.baz\",\"qtype\":\"AAAA\",\"content\":\"2001:fefe\",\"ttl\":100,\"priority\":0,\"auth\":1}]}".equals(line));
			}

			Thread.sleep(100);

			for (int i = 0; i < 10; i++)
			{
				socket.getOutputStream().write("{\"method\":\"initialize\"}\n".getBytes());
				String line = reader.readLine().trim();
				assertTrue(line, "{\"result\":true}".equals(line));
			}

			socket.close();
			t.join(1000);
			assertTrue(!t.isAlive());
		}
		catch (Exception e)
		{
			assertTrue(e.getMessage(), false);
			e.printStackTrace();
		}
		finally
		{
			t = null;
			socket = null;
			mock.shutdown();
		}

		Map<String, Long> soMap = new TreeMap<String, Long>(so.getMapAndClear());
		String soMapString = mapper.writeValueAsString(soMap);

		assertTrue(soMapString, soMap.get("RestClient.calls") == 2);
		assertTrue(soMapString, soMap.get("RestClient.created") == 1);
		assertTrue(soMapString, soMap.get("RestClient.fetches_attempted") == 2);
		assertTrue(soMapString, soMap.get("RestClient.return_codes.200") == 2);
		assertTrue(soMapString, soMap.get("RestClient.valid_responses") == 2);
		assertTrue(soMapString, soMap.get("PDNSCH.API_requests_submitted") == 2);
		assertTrue(soMapString, soMap.get("PDNSCH.answers_served_from_cache") == 18);
		assertTrue(soMapString, soMap.get("PDNSCH.cache_expirations") == 1);
		assertTrue(soMapString, soMap.get("PDNSCH.cache_hits") == 19);
		assertTrue(soMapString, soMap.get("PDNSCH.cache_inserts") == 2);
		assertTrue(soMapString, soMap.get("PDNSCH.cache_lookups") == 20);
		assertTrue(soMapString, soMap.get("PDNSCH.cache_misses") == 1);
		assertTrue(soMapString, soMap.get("PDNSCH.lines_read_from_socket") == 30);
		assertTrue(soMapString, soMap.get("PDNSCH.initialization_requests") == 10);
		assertTrue(soMapString, soMap.get("PDNSCH.lookup_requests") == 20);
		assertTrue(soMapString, soMap.get("PDNSCH.empty_replies_sent") == 10);
		assertTrue(soMapString, soMap.get("PDNSCH.positive_replies_sent") == 20);
		assertTrue(soMapString, soMap.get("PDNSCH.successful_futures") == 2);
		assertTrue(soMapString, soMap.get("PDNSCH.valid_requests_received") == 30);

		/**
		 * test: make sure a timeout fetching reply from API server results in a null reply
		 * side effects and no failures.
		 */
		so.clear();
		cache.invalidateAll();
		mock = new MockAPIServer(new NeverReplyServlet(), true, "foo", "bar");

		try
		{
			socket = new Socket();
			t = new Thread()
			{
				public void run()
				{
					ServerSocket server = null;

					try
					{
						log.debug("starting server on port 5353");
						server = new ServerSocket();
						server.bind(new InetSocketAddress("localhost", 5353));
						log.debug("listening for a connection");
						Socket accepted = server.accept();
						log.debug("got a connection, starting handler");
						Thread p = new Thread(new PowerDNSConnectionHandler(accepted, config, apiPool, cache));
						p.start();
						log.debug("waiting to join handler");
						p.join();
						log.debug("handler joined");
					}
					catch (Exception e)
					{
						assertTrue(e.getMessage(), false);
						e.printStackTrace();
					}
					finally
					{
						try
						{
							if (server != null)
							{
								log.debug("closing server on port 5353");
								server.close();
							}
						}
						catch (IOException e)
						{
						}
					}
				}
			};

			t.start();
			Thread.sleep(200);
			log.debug("connecting as a client");
			socket.connect(new InetSocketAddress("localhost", 5353));
			log.debug("connected, sending lookup request");

			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			socket.getOutputStream().write("{\"method\":\"lookup\", \"parameters\":{\"qtype\":\"ANY\",\"qname\":\"foo.bar.baz\"}}\n".getBytes());
			String line = reader.readLine();
			assertTrue(line, "{\"result\":false}".equals(line));

			socket.close();
			t.join(1000);
			assertTrue(!t.isAlive());
		}
		catch (Exception e)
		{
			assertTrue(e.getMessage(), false);
			e.printStackTrace();
		}
		finally
		{
			t = null;
			socket = null;
			mock.shutdown();
		}

		soMap = new TreeMap<String, Long>(so.getMapAndClear());
		soMapString = mapper.writeValueAsString(soMap);

		assertTrue(soMapString, soMap.get("RestClient.calls") == 1);
		assertTrue(soMapString, soMap.get("RestClient.created") == 1);
		assertTrue(soMapString, soMap.get("RestClient.fetches_attempted") == 1);
		assertTrue(soMapString, soMap.get("RestClient.null_returns.request_timeout") == 1);
		assertTrue(soMapString, soMap.get("PDNSCH.API_requests_submitted") == 1);
		assertTrue(soMapString, soMap.get("PDNSCH.cache_lookups") == 1);
		assertTrue(soMapString, soMap.get("PDNSCH.futures_exceptions.TimeoutException") == 1);
		assertTrue(soMapString, soMap.get("PDNSCH.lines_read_from_socket") == 1);
		assertTrue(soMapString, soMap.get("PDNSCH.lookup_requests") == 1);
		assertTrue(soMapString, soMap.get("PDNSCH.negative_replies_sent") == 1);
		assertTrue(soMapString, soMap.get("PDNSCH.valid_requests_received") == 1);
	}
}