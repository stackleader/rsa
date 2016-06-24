package com.stackleader.osgi.rsa.grpc;

import com.google.common.primitives.Ints;
import io.grpc.BindableService;
import io.grpc.Server;
import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_HEADER_LIST_SIZE;
import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;
import io.grpc.netty.NettyServerBuilder;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class GrpcEndpoint implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcEndpoint.class);
    private final BundleContext bundleContext;
    private Server server;
    private int port;
    private String host;
    private ServiceReference<?> exportedService;

    public GrpcEndpoint(BundleContext bundleContext, ExportReferenceImpl exportReference) {
        this.bundleContext=bundleContext;
        EndpointDescription exportedEndpoint = exportReference.getExportedEndpoint();
        Map<String, Object> properties = exportedEndpoint.getProperties();
        initFromConfiguration(properties);
        exportedService = exportReference.getExportedService();
        Object service = bundleContext.getService(exportedService);
         if (service instanceof BindableService) {
            start();
        } else {
            LOG.error("service must inherit from BindableService"); //TODO not sure about this
        }
    }

    private void start() {
        try {
            server = NettyServerBuilder
                    .forAddress(new InetSocketAddress(host, port))
                    .maxConcurrentCallsPerConnection(Integer.MAX_VALUE)
                    .maxHeaderListSize(DEFAULT_MAX_HEADER_LIST_SIZE)
                    .maxMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                    .addService((BindableService) bundleContext.getService(exportedService))
                    .build()
                    .start();
            LOG.info("Server started, listening on {}", port);
            CompletableFuture.runAsync(() -> {
                try {
                    server.awaitTermination();
                } catch (InterruptedException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
            });
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void close() throws IOException {
        if (server != null) {
            server.shutdown();
        }
    }

    private void initFromConfiguration(Map<String, Object> properties) {
        int DEFAULT_PORT = 5000;
        port = DEFAULT_PORT;
        if (properties.containsKey(GrpcProviderConstants.PORT_CONFIG_KEY)) {
            Object portProperty = properties.get(GrpcProviderConstants.PORT_CONFIG_KEY);
            if (portProperty instanceof String) {
                Integer configuredPort = Ints.tryParse((String) portProperty);
                if (configuredPort != null) {
                    port = configuredPort;
                }
            } else if (portProperty instanceof Integer) {
                port = (Integer) portProperty;
            }

        }
        host = "localhost";
        try {
            host = OsgiUtils.getLocalHostLANAddress().getHostAddress();
        } catch (UnknownHostException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        if (properties.containsKey(GrpcProviderConstants.HOST_CONFIG_KEY)) {
            Object hostProperty = properties.get(GrpcProviderConstants.HOST_CONFIG_KEY);
            if (hostProperty instanceof String) {
                host = (String) hostProperty;
            }
        }
    }
}
