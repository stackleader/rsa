package com.stackleader.osgi.rsa.grpc;

import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportReference;

/**
 *
 * @author dcnorris
 */
public class ImportReferenceImpl implements ImportReference {

    ServiceReference<?> serviceReference;
    EndpointDescription endpointDescription;

    public ImportReferenceImpl(ServiceReference<?> serviceReference, EndpointDescription endpointDescription) {
        this.serviceReference = serviceReference;
        this.endpointDescription = endpointDescription;
    }

    public void close() {
        this.endpointDescription = null;
        this.serviceReference = null;
    }

    @Override
    public ServiceReference<?> getImportedService() {
        return serviceReference;
    }

    @Override
    public EndpointDescription getImportedEndpoint() {
        return endpointDescription;
    }

}
