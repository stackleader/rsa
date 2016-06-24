package com.stackleader.osgi.rsa.grpc;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
//An Export Registration associates a service to a local endpoint. 
//The Export Registration can be used to delete the endpoint associated with an this registration. It is created with the
//RemoteServiceAdmin.exportService(ServiceReference,Map) method. When this Export Registration
//has been closed, all methods must return null.
public class ExportRegistrationImpl implements ExportRegistration {

    private static final Logger LOG = LoggerFactory.getLogger(ExportRegistrationImpl.class);
    private final AtomicBoolean isClosed;
    private Throwable exception;
    private final ExportReferenceImpl exportReferenceImpl;
    private final GrpcEndpoint grpcEndpoint;
    private final RemoteServiceAdminImpl rsa;

    public ExportRegistrationImpl(ExportReferenceImpl exportReferenceImpl, RemoteServiceAdminImpl rsa, GrpcEndpoint grpcEndpoint) {
        this.exportReferenceImpl = exportReferenceImpl;
        this.grpcEndpoint = grpcEndpoint;
        this.rsa = rsa;
        isClosed = new AtomicBoolean(false);
    }

    //Return the Export Reference for the exported service.
    //Throws IllegalStateException– When this registration was not properly initialized. See getException().
    @Override
    public ExportReference getExportReference() {
        if (isClosed.get()) {
            return null;
        }
        if (exception != null) {
            throw new IllegalStateException("Endpoint registration is failed. ");
        }
        return exportReferenceImpl;
    }

    @Override
    public EndpointDescription update(Map<String, ?> properties) {
        if (isClosed.get()) {
            return null;
        }
        //TODO implement this method
        return exportReferenceImpl.getExportedEndpoint();
    }

//    Delete the local endpoint and disconnect any remote distribution providers. After this method returns,
//    all methods must return null. This method has no effect when this registration has already
//    been closed or is being closed.
//    @Override
    public void close() {
        try {
            grpcEndpoint.close();
            rsa.remoteExportRegistration(exportReferenceImpl);
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    //Return the exception for any error during the export process. If the Remote Service Admin for some
    //reasons is unable to properly initialize this registration, then it must return an exception from this
    //method. If no error occurred, this method must return null. The error must be set before this Export
    //Registration is returned. Asynchronously occurring errors must be reported to the log.
    @Override
    public Throwable getException() {
        if (isClosed.get()) {
            return null;
        }
        return exception;
    }

}
