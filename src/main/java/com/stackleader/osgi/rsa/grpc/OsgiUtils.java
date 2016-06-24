package com.stackleader.osgi.rsa.grpc;

import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author dcnorris
 */
public class OsgiUtils {

    public static List<String> stringPlusToListFormat(Object stringPlus) {
        if (stringPlus == null) {
            return Collections.EMPTY_LIST;
        }
        if (stringPlus instanceof String) {
            String toSplit = (String) stringPlus;
            return Splitter.on(',').trimResults().splitToList(toSplit);
        }

        if (stringPlus instanceof String[]) {
            return Arrays.asList((String[]) stringPlus);
        }

        if (stringPlus instanceof Collection) {
            return FluentIterable.from((Collection) stringPlus)
                    .filter(String.class)
                    .toList();

        }
        return Collections.EMPTY_LIST;
    }

    public static Map<String, Object> getPublicServiceReferenceProps(ServiceReference<?> serviceReference) {
        return Arrays.asList(serviceReference.getPropertyKeys()).stream()
                .filter(key -> !key.startsWith(".")) //filter if private property (i.e. starts with .)
                .collect(Collectors.toMap(key -> key, key -> serviceReference.getProperty(key)));
    }

    // credit to dosgi OsgiUtils authors -- https://svn.apache.org/repos/asf/cxf/dosgi/trunk/dsw/cxf-dsw/src/main/java/org/apache/cxf/dosgi/dsw/util/OsgiUtils.java
    public static String getUUID(BundleContext bundleContext) {
        synchronized ("org.osgi.framework.uuid") {
            String uuid = bundleContext.getProperty("org.osgi.framework.uuid");
            if (uuid == null) {
                uuid = UUID.randomUUID().toString();
                System.setProperty("org.osgi.framework.uuid", uuid);
            }
            return uuid;
        }
    }

    public static InetAddress getLocalHostLANAddress() throws UnknownHostException {
        try {
            List<InetAddress> candidateAddrs = Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                    .flatMap(iface -> Collections.list(iface.getInetAddresses()).stream())
                    .filter(inetAddr -> !inetAddr.isLoopbackAddress())
                    .collect(Collectors.toList());
            if (candidateAddrs.stream().anyMatch(addr -> addr.isSiteLocalAddress())) {
                return candidateAddrs.stream().filter(addr -> addr.isSiteLocalAddress()).findFirst().get();
            } else {
                return candidateAddrs.stream().findFirst().orElse(InetAddress.getLocalHost());
            }

        } catch (SocketException | UnknownHostException ex) {
            throw new UnknownHostException();
        }
    }

}
