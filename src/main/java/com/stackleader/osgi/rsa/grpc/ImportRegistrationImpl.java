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

import java.util.concurrent.atomic.AtomicBoolean;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportRegistrationImpl implements ImportRegistration {

    private static final Logger LOG = LoggerFactory.getLogger(ImportRegistrationImpl.class);
    private final AtomicBoolean isClosed;
    private final ImportReferenceImpl importReferenceImpl;
    private Throwable exception;
    private final RemoteServiceAdminImpl rsa;

    public ImportRegistrationImpl(ImportReferenceImpl importReferenceImpl, RemoteServiceAdminImpl rsa) {
        isClosed = new AtomicBoolean(false);
        this.importReferenceImpl = importReferenceImpl;
        this.rsa = rsa;
    }

    @Override
    public ImportReference getImportReference() {
        if (isClosed.get()) {
            return null;
        }
        if (exception != null) {
            //TODO maybe just log and return null?
            throw new IllegalStateException("Endpoint registration is failed.");
        }
        return importReferenceImpl;
    }

    @Override
    public boolean update(EndpointDescription endpoint) {
        //TODO implement this
        return true;
    }

    @Override
    public void close() {
        if (!isClosed.get()) {
            rsa.removeImportRegistration(importReferenceImpl.getImportedEndpoint());
        }
    }

    @Override
    public Throwable getException() {
        if (isClosed.get()) {
            return null;
        }
        return exception;
    }

}
