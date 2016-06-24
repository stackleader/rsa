/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.stackleader.osgi.rsa.grpc;

import java.util.List;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//sourced from aries rsa implementation -- credit owed to original authors 
//TODO consider replacement with a stackleader implementation... 
public class EventProducer {

    private static final Logger LOG = LoggerFactory.getLogger(EventProducer.class);
    private final BundleContext bundleContext;

    public EventProducer(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    protected void publishNotification(List<ExportRegistration> erl) {
        erl.forEach(this::publishNotification);
    }

    protected void publishNotification(ExportRegistration er) {
        if (er.getException() == null) {
            notify(RemoteServiceAdminEvent.EXPORT_REGISTRATION, er.getExportReference(), null);
        } else {
            notify(RemoteServiceAdminEvent.EXPORT_ERROR, (ExportReference) null, er.getException());
        }
    }

    protected void publishNotification(ImportRegistration ir) {
        if (ir.getException() == null) {
            notify(RemoteServiceAdminEvent.IMPORT_REGISTRATION, ir.getImportReference(), null);
        } else {
            notify(RemoteServiceAdminEvent.IMPORT_ERROR, (ImportReference) null, ir.getException());
        }
    }

    public void notifyRemoval(ExportRegistration er) {
        notify(RemoteServiceAdminEvent.EXPORT_UNREGISTRATION, er.getExportReference(), null);
    }

    public void notifyRemoval(ImportRegistration ir) {
        notify(RemoteServiceAdminEvent.IMPORT_UNREGISTRATION, ir.getImportReference(), null);
    }

    private void notify(int type, ExportReference er, Throwable ex) {
        try {
            RemoteServiceAdminEvent event = new RemoteServiceAdminEvent(type, bundleContext.getBundle(), er, ex);
            notifyListeners(event);
        } catch (IllegalStateException ise) {
            LOG.debug("can't send notifications since bundle context is no longer valid");
        }
    }

    private void notify(int type, ImportReference ir, Throwable ex) {
        try {
            RemoteServiceAdminEvent event = new RemoteServiceAdminEvent(type, bundleContext.getBundle(), ir, ex);
            notifyListeners(event);
        } catch (IllegalStateException ise) {
            LOG.debug("can't send notifications since bundle context is no longer valid");
        }
    }

     @SuppressWarnings({
     "rawtypes", "unchecked"
    })
    private void notifyListeners(RemoteServiceAdminEvent rsae) {
        try {
            ServiceReference[] listenerRefs = bundleContext.getServiceReferences(
                    RemoteServiceAdminListener.class.getName(), null);
            if (listenerRefs != null) {
                for (ServiceReference sref : listenerRefs) {
                    RemoteServiceAdminListener rsal = (RemoteServiceAdminListener)bundleContext.getService(sref);
                    if (rsal != null) {
                        try {
                            Bundle bundle = sref.getBundle();
                            if (bundle != null) {
                                LOG.debug("notify RemoteServiceAdminListener {} of bundle {}",
                                        rsal, bundle.getSymbolicName());
                                rsal.remoteAdminEvent(rsae);
                            }
                        } finally {
                            bundleContext.ungetService(sref);
                        }
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
