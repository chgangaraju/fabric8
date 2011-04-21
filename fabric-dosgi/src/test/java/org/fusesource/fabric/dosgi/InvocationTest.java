/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.fusesource.fabric.dosgi.io.ServerInvoker;
import org.fusesource.fabric.dosgi.tcp.ClientInvokerImpl;
import org.fusesource.fabric.dosgi.tcp.ServerInvokerImpl;
import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InvocationTest {

    @Test
    public void testInvoke() throws Exception {

        int port = getFreePort();

        DispatchQueue queue = Dispatch.createQueue();
        ServerInvokerImpl server = new ServerInvokerImpl("tcp://localhost:" + port, queue);
        server.start();
        ClientInvokerImpl client = new ClientInvokerImpl(queue);
        client.start();

        try {
            server.registerService("service-id", new ServerInvoker.ServiceFactory() {
                public Object get() {
                    return new HelloImpl();
                }
                public void unget() {
                }
            }, HelloImpl.class.getClassLoader());


            InvocationHandler handler = client.getProxy("tcp://localhost:" + port, "service-id", HelloImpl.class.getClassLoader());
            Hello hello  = (Hello) Proxy.newProxyInstance(HelloImpl.class.getClassLoader(), new Class[] { Hello.class }, handler);

            assertEquals("Hello Fabric!", hello.hello("Fabric"));
        }
        finally {
            server.stop();
            client.stop();
        }
    }

    @Test
    public void testUnderLoad() throws Exception {
        int port = getFreePort();

        DispatchQueue queue = Dispatch.createQueue();
        ServerInvokerImpl server = new ServerInvokerImpl("tcp://localhost:" + port, queue);
        server.start();
        ClientInvokerImpl client = new ClientInvokerImpl(queue);
        client.start();

        try {
            server.registerService("service-id", new ServerInvoker.ServiceFactory() {
                public Object get() {
                    return new HelloImpl();
                }
                public void unget() {
                }
            }, HelloImpl.class.getClassLoader());


            InvocationHandler handler = client.getProxy("tcp://localhost:" + port, "service-id", HelloImpl.class.getClassLoader());

            final Hello hello  = (Hello) Proxy.newProxyInstance(HelloImpl.class.getClassLoader(), new Class[] { Hello.class }, handler);

            final int nbThreads = 100;
            final int nbInvocationsPerThread = 1000;

            final AtomicInteger requests = new AtomicInteger(0);
            final AtomicInteger failures = new AtomicInteger(0);
            final long latencies[] = new long[nbThreads*nbInvocationsPerThread];

            Thread[] threads = new Thread[nbThreads];
            for (int t = 0; t < nbThreads; t++) {
                final int thread_idx = t;
                threads[t] = new Thread() {
                    public void run() {
                        for (int i = 0; i < nbInvocationsPerThread; i++) {
                            try {
                                requests.incrementAndGet();
                                String response;

                                long start = System.nanoTime();
                                response = hello.hello("Fabric");
                                long end = System.nanoTime();
                                latencies[(thread_idx*nbInvocationsPerThread)+i] = end-start;

                                assertEquals("Hello Fabric!", response);
                            } catch (Throwable t) {
                                latencies[(thread_idx*nbInvocationsPerThread)+i] = -1;
                                failures.incrementAndGet();
                                if (t instanceof UndeclaredThrowableException) {
                                    t = ((UndeclaredThrowableException) t).getUndeclaredThrowable();
                                }
                                System.err.println("Error: " + t.getClass().getName() + (t.getMessage() != null ? " (" + t.getMessage() + ")" : ""));
                            }
                        }
                    }
                };
                threads[t].start();
            }


            for (int t = 0; t < nbThreads; t++) {
                threads[t].join();
            }

            long latency_count = 0;
            long latency_sum = 0;
            for (int t = 0; t < latencies.length; t++) {
                if( latencies[t] != -1 ) {
                    latency_count += 1;
                    latency_sum += latencies[t];
                }
            }
            double latency_avg = (latency_sum * 1.0d)/latency_count;


            long MILLIS_IN_A_NANO = TimeUnit.MILLISECONDS.toNanos(1);


            System.err.println("Ratio: " + failures.get() + " / " + requests.get());
            System.err.println(String.format("Average request latency: %.2f ms", (latency_avg / MILLIS_IN_A_NANO)));
        }
        finally {
            server.stop();
            client.stop();
        }
    }


    public static interface Hello {
        String hello(String name);
    }

    public static class HelloImpl implements Hello {
        public String hello(String name) {
            return "Hello " + name + "!";
        }
    }

    static int getFreePort() throws IOException {
        ServerSocket sock = new ServerSocket();
        try {
            sock.bind(new InetSocketAddress(0));
            return sock.getLocalPort();
        } finally {
            sock.close();
        }
    }
}
