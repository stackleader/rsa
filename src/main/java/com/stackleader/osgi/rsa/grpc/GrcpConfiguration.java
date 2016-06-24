package com.stackleader.osgi.rsa.grpc;

/**
 *
 * @author dcnorris
 */
public interface GrcpConfiguration {

    String getHhost();

    int getPort();

    /**
     * Defines the maximum number of concurrent calls permitted for each incoming connection.
     * Defaults to Integer.MAX_VALUE
     */
    int getMaxConcurrentCallsPerConnection();

    /**
     * Defines the maximum size (in bytes) for inbound header/trailer.
     * defaults to 8192 bytes
     */
    int getmaxHeaderListSize();

    /**
     * Defines the maximum message size allowed to be received on the server.
     * defaults to 100 MiB represented in bytes i.e. 100 * 1024 * 1024
     */
    int getMaxMessageSize();

}
