package com.jrestless.aws.gateway.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

public class GatewayBinaryResponseFilterIntTest {

	@Test
	public void testBinaryZippedWhenZippingRequested() throws Exception {
		runJerseyTest(createJerseyTest(true), (jersey) -> {
			Response response = jersey.target()
					.path("/binary")
					.request()
					.header(HttpHeaders.ACCEPT_ENCODING, "gzip")
					.get();
			assertEquals(200, response.getStatus());
			assertNotNull(response.getHeaderString(HttpHeaders.CONTENT_ENCODING));
			assertNotNull(response.getHeaderString(GatewayBinaryResponseFilter.HEADER_BINARY_RESPONSE));
			InputStream unzipStream = new GZIPInputStream(response.readEntity(InputStream.class));
			assertEquals("binary", new String(toBytes(unzipStream)));
		});
	}

	@Test
	public void testBinaryNotZippedWhenZippingNotRequested() throws Exception {
		runJerseyTest(createJerseyTest(true), (jersey) -> {
			Response response = jersey.target()
					.path("/binary")
					.request()
					.get();
			assertEquals(200, response.getStatus());
			assertNull(response.getHeaderString(HttpHeaders.CONTENT_ENCODING));
			assertNotNull(response.getHeaderString(GatewayBinaryResponseFilter.HEADER_BINARY_RESPONSE));
			assertEquals("binary", response.readEntity(String.class));
		});
	}

	@Test
	public void testNonBinaryNotZippedWhenZippingRequestedAndBinaryCompressionOnly() throws Exception {
		runJerseyTest(createJerseyTest(true), (jersey) -> {
			Response response = jersey.target()
					.path("/non-binary")
					.request()
					.header(HttpHeaders.ACCEPT_ENCODING, "gzip")
					.get();
			assertEquals(200, response.getStatus());
			assertNull(response.getHeaderString(HttpHeaders.CONTENT_ENCODING));
			assertNull(response.getHeaderString(GatewayBinaryResponseFilter.HEADER_BINARY_RESPONSE));
			assertEquals("non-binary", response.readEntity(String.class));
		});
	}

	@Test
	public void testNonBinaryNotZippedWhenNoZippingRequestedAndBinaryCompressionOnly() throws Exception {
		runJerseyTest(createJerseyTest(true), (jersey) -> {
			Response response = jersey.target()
					.path("/non-binary")
					.request()
					.get();
			assertEquals(200, response.getStatus());
			assertNull(response.getHeaderString(HttpHeaders.CONTENT_ENCODING));
			assertNull(response.getHeaderString(GatewayBinaryResponseFilter.HEADER_BINARY_RESPONSE));
			assertEquals("non-binary", response.readEntity(String.class));
		});
	}

	@Test
	public void testNonBinaryZippedWhenZippingRequestedAndNotBinaryCompressionOnly() throws Exception {
		runJerseyTest(createJerseyTest(false), (jersey) -> {
			Response response = jersey.target()
					.path("/non-binary")
					.request()
					.header(HttpHeaders.ACCEPT_ENCODING, "gzip")
					.get();
			assertEquals(200, response.getStatus());
			assertNotNull(response.getHeaderString(HttpHeaders.CONTENT_ENCODING));
			assertNotNull(response.getHeaderString(GatewayBinaryResponseFilter.HEADER_BINARY_RESPONSE));
			InputStream unzipStream = new GZIPInputStream(response.readEntity(InputStream.class));
			assertEquals("non-binary", new String(toBytes(unzipStream)));
		});
	}

	@Test
	public void testNonBinaryNotZippedWhenNoZippingRequestedAndNotBinaryCompressionOnly() throws Exception {
		runJerseyTest(createJerseyTest(false), (jersey) -> {
			Response response = jersey.target()
					.path("/non-binary")
					.request()
					.get();
			assertEquals(200, response.getStatus());
			assertNull(response.getHeaderString(HttpHeaders.CONTENT_ENCODING));
			assertNull(response.getHeaderString(GatewayBinaryResponseFilter.HEADER_BINARY_RESPONSE));
			assertEquals("non-binary", response.readEntity(String.class));
		});
	}

	private void runJerseyTest(JerseyTest jerseyTest, ThrowingConsumer<JerseyTest> test) throws Exception {
		try {
			try {
				jerseyTest.setUp();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			test.accept(jerseyTest);
		} finally {
			try {
				jerseyTest.tearDown();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}


	private JerseyTest createJerseyTest(Boolean binaryCompressionOnly) {
		return new JerseyTest() {
			@Override
			protected Application configure() {
				ResourceConfig config = new ResourceConfig();
				config.register(TestResource.class);
				config.register(EncodingFilter.class);
				config.register(GZipEncoder.class);
				config.register(GatewayBinaryResponseFilter.class);
				if (binaryCompressionOnly != null) {
					config.property(GatewayBinaryResponseFilter.BINARY_COMPRESSION_ONLY_PROPERTY,
							binaryCompressionOnly);
				}
				return config;
			}
		};
	}

	private byte[] toBytes(InputStream is) throws IOException {
	    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	    int nRead;
	    byte[] data = new byte[1024];
	    while ((nRead = is.read(data, 0, data.length)) != -1) {
	        buffer.write(data, 0, nRead);
	    }
	    buffer.flush();
	    return buffer.toByteArray();
	}

	@Path("/")
	public static class TestResource {
		@GET
		@Path("/binary")
		public Response getBinary() {
			return Response.ok("binary".getBytes()).build();
		}
		@GET
		@Path("/non-binary")
		public Response getNonBinary() {
			return Response.ok("non-binary").build();
		}
	}

	@FunctionalInterface
	private static interface ThrowingConsumer<T> {
		void accept(T in) throws Exception;
	}
}
