package com.stackleader.osgi.rsa.grpc;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

/**
 *
 * @author dcnorris
 */
public class GrpcProviderConstants {

    private GrpcProviderConstants() {
    }

    public static final String SERVICE_PID = "com.stackleader.osgi.rsa.grpc";
    public static final String CONFIGURATION_TYPE = SERVICE_PID;
    public static final String HOST_CONFIG_KEY = SERVICE_PID + ".host";
    public static final String PORT_CONFIG_KEY = SERVICE_PID + ".port";
    // Class name of GRPC stub class i.e. class extending io.grpc.stub.AbstractStub<T>
    public static final String STUB_CLASS_NAME = SERVICE_PID + ".stub.classname";
    public static final String[] SUPPORTED_CONFIGURATION_TYPES = new String[]{CONFIGURATION_TYPE};
    public static final String GRPC_INTENT = "grpc";
    public static final Set<String> SUPPORTED_INTENTS = ImmutableSet.of(GRPC_INTENT);

}
