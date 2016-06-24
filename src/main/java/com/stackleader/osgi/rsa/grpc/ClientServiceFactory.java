package com.stackleader.osgi.rsa.grpc;

import com.google.common.primitives.Ints;
import static com.stackleader.osgi.rsa.grpc.GrpcProviderConstants.STUB_CLASS_NAME;
import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.grpc.stub.AbstractStub;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class ClientServiceFactory implements ServiceFactory<Object> {

    private static final Logger LOG = LoggerFactory.getLogger(ClientServiceFactory.class);
    private final Hashtable<String, Object> serviceProperties;
    private final EndpointDescription endpoint;
    private String host = "localhost";
    private int port;

    ClientServiceFactory(EndpointDescription endpoint, Hashtable<String, Object> serviceProperties) {
        this.endpoint = endpoint;
        this.serviceProperties = serviceProperties;
        setPortFromProperties();
        setHostFromProperties();
    }

    @Override
    public Object getService(Bundle requestingBundle, ServiceRegistration<Object> registration) {
        List<String> interfaceNames = endpoint.getInterfaces();
        final BundleContext consumerContext = requestingBundle.getBundleContext();
        final ClassLoader consumerLoader = requestingBundle.adapt(BundleWiring.class).getClassLoader();
        try {
            LOG.debug("getService() from serviceFactory for {}", interfaceNames);
            final List<Class<?>> interfaces = new ArrayList<Class<?>>();
            for (String ifaceName : interfaceNames) {
                interfaces.add(consumerLoader.loadClass(ifaceName));
            }
            return AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    try {
                        Class<?>[] ifAr = interfaces.toArray(new Class[]{});
                        //create proxy
                        LOG.debug("making connection to server at {}:{}", host, port);
                        ManagedChannel channel = OkHttpChannelBuilder.forAddress(host, port)
                                .usePlaintext(true)
                                .build();
                        String grcpClassname = (String) serviceProperties.get(STUB_CLASS_NAME);
                        Class grpcClass = Class.forName(grcpClassname, true, consumerLoader);
                        Method[] methods = grpcClass.getMethods();
                        for (int i = 0; i < methods.length; i++) {
                           LOG.info("public method: " + methods[i]);
                        }
                        Method method = grpcClass.getMethod("newBlockingStub", io.grpc.Channel.class);
                        AbstractStub stub = (AbstractStub) method.invoke(null, channel);
                        ClientService clientService = new ClientService(stub);
                        return Proxy.newProxyInstance(consumerLoader, ifAr, clientService);
                    } catch (Throwable ex) {
                        LOG.error(ex.getMessage(), ex);
                    }
                    return null;
                }
            });

        } catch (Exception ex) {
            LOG.warn("Problem creating a proxy for {}", interfaceNames, ex);
        }
        return null;
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
        // TODO look into closing importRegistration
    }

    private void setHostFromProperties() {
        if (serviceProperties.containsKey(GrpcProviderConstants.HOST_CONFIG_KEY)) {
            Object hostProperty = serviceProperties.get(GrpcProviderConstants.HOST_CONFIG_KEY);
            if (hostProperty instanceof String) {
                host = (String) hostProperty;
            }
        }
    }

    private void setPortFromProperties() {
        if (serviceProperties.containsKey(GrpcProviderConstants.PORT_CONFIG_KEY)) {
            Object portProperty = serviceProperties.get(GrpcProviderConstants.PORT_CONFIG_KEY);
            if (portProperty instanceof String) {
                Integer configuredPort = Ints.tryParse((String) portProperty);
                if (configuredPort != null) {
                    port = configuredPort;
                }
            } else if (portProperty instanceof Integer) {
                port = (Integer) portProperty;
            }

        }
    }
}
