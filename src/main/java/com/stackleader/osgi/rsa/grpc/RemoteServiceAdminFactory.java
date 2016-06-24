package com.stackleader.osgi.rsa.grpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class RemoteServiceAdminFactory implements ServiceFactory<RemoteServiceAdmin> {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteServiceAdminFactory.class);
    private final Map<Bundle, RemoteServiceAdminImpl> instances;

    public RemoteServiceAdminFactory() {
        instances = new ConcurrentHashMap<>();
    }

    @Override
    public RemoteServiceAdmin getService(Bundle bundle, ServiceRegistration<RemoteServiceAdmin> registration) {
        LOG.info("new RemoteServiceAdmin ServiceInstance created for Bundle {}", bundle.getSymbolicName());
        RemoteServiceAdminImpl remoteServiceAdminImpl = new RemoteServiceAdminImpl(bundle.getBundleContext());
        instances.put(bundle, remoteServiceAdminImpl);
        return remoteServiceAdminImpl;
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<RemoteServiceAdmin> registration, RemoteServiceAdmin service) {
        LOG.debug("RemoteServiceAdmin ServiceInstance removed for Bundle {}", bundle.getSymbolicName());
        RemoteServiceAdminImpl instance = instances.remove(bundle);
        try {
            instance.setClosed(true);
        } catch (Exception ex) {
            LOG.debug(ex.getMessage(), ex);
        }
    }

}
