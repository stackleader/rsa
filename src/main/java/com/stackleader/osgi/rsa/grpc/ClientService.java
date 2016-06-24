package com.stackleader.osgi.rsa.grpc;

import io.grpc.stub.AbstractStub;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * credit to ecf team https://github.com/ECF/grpc-RemoteServicesProvider for 
 * @author dcnorris
 */
public class ClientService implements InvocationHandler {

    private final AbstractStub stub;

    public ClientService(AbstractStub stub) {
        this.stub = stub;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Class[] rcMethodParameterTypes = method.getParameterTypes();
        Method invokeMethod = null;
        for (Method m : stub.getClass().getMethods()) {
            if (methodName.equals(m.getName())
                    && compareParameterTypes(rcMethodParameterTypes, m.getParameterTypes())) {
                invokeMethod = m;
            }
        }
        if (invokeMethod == null) {
            throw new IllegalStateException("Cannot find matching invokeMethod on grpc stub");
        }
        return invokeMethod.invoke(stub, args);
    }

    @SuppressWarnings("rawtypes")
    private boolean compareParameterTypes(Class[] first, Class[] second) {
        if (first.length != second.length) {
            return false;
        }
        for (int i = 0; i < first.length; i++) {
            if (!first[i].equals(second[i])) {
                return false;
            }
        }
        return true;
    }
}
