package com.stackleader.osgi.rsa.grpc;

import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;

/**
 *
 * @author dcnorris
 */
public class ExportReferenceImpl implements ExportReference {

    ServiceReference<?> serviceReference;
    EndpointDescription endpointDescription;

    public ExportReferenceImpl(ServiceReference<?> serviceReference, EndpointDescription endpointDescription) {
        this.serviceReference = serviceReference;
        this.endpointDescription = endpointDescription;
    }

    @Override
    public ServiceReference<?> getExportedService() {
        return serviceReference;
    }

    @Override
    public EndpointDescription getExportedEndpoint() {
        return endpointDescription;
    }

    synchronized void close() {
        this.endpointDescription = null;
        this.serviceReference = null;
    }
    
    
}
