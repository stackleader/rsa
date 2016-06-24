package com.stackleader.osgi.rsa.grpc;

import static com.stackleader.osgi.rsa.grpc.GrpcProviderConstants.CONFIGURATION_TYPE;
import static com.stackleader.osgi.rsa.grpc.GrpcProviderConstants.GRPC_INTENT;
import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.REMOTE_CONFIGS_SUPPORTED;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.REMOTE_INTENTS_SUPPORTED;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

/**
 *
 * @author dcnorris
 */
public class Activator implements BundleActivator {

    private ServiceRegistration<?> serviceRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(REMOTE_INTENTS_SUPPORTED, GRPC_INTENT);
        props.put(REMOTE_CONFIGS_SUPPORTED, CONFIGURATION_TYPE);
        RemoteServiceAdminFactory adminFactory = new RemoteServiceAdminFactory();
        serviceRegistration = context.registerService(RemoteServiceAdmin.class.getName(), adminFactory, props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        serviceRegistration.unregister();
    }

}
