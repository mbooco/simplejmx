package com.j256.simplejmx.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import javax.management.JMException;
import javax.management.ReflectionException;

import org.junit.Test;

import com.j256.simplejmx.client.JmxClient;
import com.j256.simplejmx.common.JmxAttribute;
import com.j256.simplejmx.common.JmxFolderName;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.common.JmxSelfNaming;
import com.j256.simplejmx.common.ObjectNameUtil;

public class JmxServerTest {

	private static final int DEFAULT_PORT = 5256;
	private static final String DOMAIN_NAME = "j256";
	private static final String OBJECT_NAME = "testObject";
	private static final int FOO_VALUE = 1459243;
	private static final String FOLDER_FIELD_NAME = "00";
	private static final String FOLDER_VALUE_NAME = "FolderName";
	private static final String FOLDER_NAME = FOLDER_FIELD_NAME + "=" + FOLDER_VALUE_NAME;

	@Test
	public void testJmxServer() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		try {
			server.start();
		} finally {
			server.stop();
		}
	}

	@Test
	public void testJmxServerStartStopStart() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		try {
			server.start();
			server.stop();
			server.start();
		} finally {
			server.stop();
		}
	}

	@Test(expected = JMException.class)
	public void testJmxServerDoubleInstance() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		try {
			server.start();
			new JmxServer(DEFAULT_PORT).start();
		} finally {
			server.stop();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJmxServerWildPort() throws Exception {
		JmxServer server = new JmxServer(-10);
		try {
			server.start();
		} finally {
			server.stop();
		}
	}

	@Test(expected = JMException.class)
	public void testDoubleClose() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		try {
			server.start();
		} finally {
			Field field = server.getClass().getDeclaredField("rmiRegistry");
			field.setAccessible(true);
			Registry rmiRegistry = (Registry) field.get(server);
			// close the rmi registry through trickery!
			UnicastRemoteObject.unexportObject(rmiRegistry, true);
			try {
				server.stopThrow();
			} finally {
				server.stop();
			}
		}
	}

	@Test
	public void testJmxServerInt() throws Exception {
		JmxServer server = new JmxServer();
		try {
			server.setPort(DEFAULT_PORT);
			server.start();
		} finally {
			server.stop();
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testJmxServerStartNoPort() throws Exception {
		JmxServer server = new JmxServer();
		try {
			server.start();
			fail("Should not have gotten here");
		} finally {
			server.stop();
		}
	}

	@Test
	public void testRegister() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		TestObject obj = new TestObject();
		try {
			server.start();
			JmxClient client = new JmxClient(DEFAULT_PORT);

			try {
				client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo");
				fail("should not get here");
			} catch (Exception e) {
				// ignored
			}

			server.register(obj);
			assertEquals(FOO_VALUE, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));

			int newValue = FOO_VALUE + 2;
			client.setAttribute(DOMAIN_NAME, OBJECT_NAME, "foo", newValue);
			assertEquals(newValue, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));

			client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "resetFoo");
			assertEquals(0, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));

			newValue = FOO_VALUE + 12;
			client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "resetFoo", (Integer) newValue);
			assertEquals(newValue, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));

			newValue = FOO_VALUE + 32;
			client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "resetFoo", Integer.toString(newValue));
			assertEquals(newValue, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));

			server.unregister(obj);

			try {
				client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo");
				fail("should not get here");
			} catch (Exception e) {
				// ignored
			}
		} finally {
			server.unregister(obj);
			server.stop();
		}
	}

	@Test(expected = JMException.class)
	public void testDoubleRegister() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		TestObject obj = new TestObject();
		try {
			server.start();
			server.register(obj);
			server.register(obj);
		} finally {
			server.unregister(obj);
			server.stop();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRegisterNoJmxResource() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		try {
			server.start();
			// this class has no JmxResource annotation
			server.register(this);
		} finally {
			server.unregister(this);
			server.stop();
		}
	}

	@Test(expected = JMException.class)
	public void testShortAttributeMethodName() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		ShortAttributeMethodName obj = new ShortAttributeMethodName();
		try {
			server.start();
			server.register(obj);
		} finally {
			server.unregister(obj);
			server.stop();
		}
	}

	@Test(expected = JMException.class)
	public void testJustGetAttributeMethodName() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		JustGet obj = new JustGet();
		try {
			server.start();
			server.register(obj);
		} finally {
			server.unregister(obj);
			server.stop();
		}
	}

	@Test
	public void testRegisterFolders() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		TestObjectFolders obj = new TestObjectFolders();
		try {
			server.start();
			server.register(obj);
			JmxClient client = new JmxClient(DEFAULT_PORT);
			assertEquals(FOO_VALUE, client.getAttribute(
					ObjectNameUtil.makeObjectName(DOMAIN_NAME, OBJECT_NAME, new String[] { FOLDER_NAME }), "foo"));
			assertEquals(
					FOO_VALUE,
					client.getAttribute(
							ObjectNameUtil.makeObjectName(DOMAIN_NAME, OBJECT_NAME, new String[] { FOLDER_FIELD_NAME
									+ "=" + FOLDER_VALUE_NAME }), "foo"));
		} finally {
			server.unregister(obj);
			server.stop();
		}
	}

	@Test
	public void testSelfNaming() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		SelfNaming obj = new SelfNaming();
		try {
			server.start();
			server.register(obj);
			JmxClient client = new JmxClient(DEFAULT_PORT);
			assertEquals(FOO_VALUE, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));
		} finally {
			server.unregister(obj);
			server.stop();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidDomain() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		InvalidDomain obj = new InvalidDomain();
		try {
			server.start();
			server.register(obj);
		} finally {
			server.unregister(obj);
			server.stop();
		}
	}

	@Test
	public void testNoDescriptions() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		NoDescriptions obj = new NoDescriptions();
		try {
			server.start();
			server.register(obj);
			JmxClient client = new JmxClient(DEFAULT_PORT);
			assertEquals(FOO_VALUE, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));
		} finally {
			server.unregister(obj);
			server.stop();
		}
	}

	@Test
	public void testHasDescription() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		HasDescriptions obj = new HasDescriptions();
		try {
			server.start();
			server.register(obj);
			JmxClient client = new JmxClient(DEFAULT_PORT);
			assertEquals(FOO_VALUE, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));
		} finally {
			server.unregister(obj);
			server.stop();
		}
	}

	@Test
	public void testOperationParameters() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		OperationParameters obj = new OperationParameters();
		try {
			server.start();
			server.register(obj);
			JmxClient client = new JmxClient(DEFAULT_PORT);
			String someArg = "pfoewjfpeowjfewf ewopjfwefew";
			assertEquals(someArg, client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "doSomething", someArg));
		} finally {
			server.unregister(obj);
			server.stop();
		}
	}

	@Test(expected = ReflectionException.class)
	public void testThrowingGet() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		MisterThrow obj = new MisterThrow();
		try {
			server.start();
			server.register(obj);
			JmxClient client = new JmxClient(DEFAULT_PORT);
			client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo");
		} finally {
			server.unregister(obj);
			server.stop();
		}
	}

	@Test(expected = ReflectionException.class)
	public void testThrowingSet() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		MisterThrow obj = new MisterThrow();
		try {
			server.start();
			server.register(obj);
			JmxClient client = new JmxClient(DEFAULT_PORT);
			client.setAttribute(DOMAIN_NAME, OBJECT_NAME, "foo", 0);
		} finally {
			server.unregister(obj);
			server.stop();
		}
	}

	@Test(expected = ReflectionException.class)
	public void testThrowingOperation() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		MisterThrow obj = new MisterThrow();
		try {
			server.start();
			server.register(obj);
			JmxClient client = new JmxClient(DEFAULT_PORT);
			client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "someCall");
		} finally {
			server.unregister(obj);
			server.stop();
		}
	}

	/* ============================================================= */

	@JmxResource(domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class TestObject {

		private int foo = FOO_VALUE;

		@JmxAttribute
		public int getFoo() {
			return foo;
		}

		@JmxAttribute
		public void setFoo(int foo) {
			this.foo = foo;
		}

		@JmxOperation
		public void resetFoo() {
			this.foo = 0;
		}

		@JmxOperation
		public void resetFoo(int newValue) {
			this.foo = newValue;
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, objectName = OBJECT_NAME, folderNames = { FOLDER_NAME })
	protected static class TestObjectFolders {

		@JmxAttribute
		public int getFoo() {
			return FOO_VALUE;
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class ShortAttributeMethodName {
		@JmxAttribute
		public int x() {
			return 0;
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class JustGet {
		@JmxAttribute
		public int get() {
			return 0;
		}
	}

	@JmxResource(domainName = "not the domain name", objectName = "not the object name")
	protected static class SelfNaming implements JmxSelfNaming {
		@JmxAttribute
		public int getFoo() {
			return FOO_VALUE;
		}
		public String getJmxDomainName() {
			return DOMAIN_NAME;
		}
		public String getJmxObjectName() {
			return OBJECT_NAME;
		}
		public JmxFolderName[] getJmxFolderNames() {
			return null;
		}
	}

	@JmxResource(domainName = "", objectName = OBJECT_NAME)
	protected static class InvalidDomain {
		@JmxAttribute
		public int getFoo() {
			return FOO_VALUE;
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class NoDescriptions {
		@JmxAttribute
		public int getFoo() {
			return FOO_VALUE;
		}
		@JmxOperation
		public void doSomething() {
		}
	}

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class HasDescriptions {
		@JmxAttribute(description = "Foo value")
		public int getFoo() {
			return FOO_VALUE;
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class OperationParameters {
		@JmxOperation(description = "A value", parameterNames = { "first" }, parameterDescriptions = { "First argument" })
		public String doSomething(String first) {
			return first;
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class MisterThrow {
		@JmxAttribute
		public int getFoo() {
			throw new IllegalStateException("because I can");
		}
		@JmxAttribute
		public void setFoo(int value) {
			throw new IllegalStateException("because I can");
		}
		@JmxOperation
		public void someCall() {
			throw new IllegalStateException("because I can");
		}
	}
}