package com.stackleader.osgi.rsa.grpc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import static com.stackleader.osgi.rsa.grpc.GrpcProviderConstants.CONFIGURATION_TYPE;
import static com.stackleader.osgi.rsa.grpc.GrpcProviderConstants.GRPC_INTENT;
import static com.stackleader.osgi.rsa.grpc.GrpcProviderConstants.SUPPORTED_INTENTS;
import static com.stackleader.osgi.rsa.grpc.OsgiUtils.getPublicServiceReferenceProps;
import static com.stackleader.osgi.rsa.grpc.OsgiUtils.stringPlusToListFormat;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.osgi.framework.BundleContext;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.Constants.SERVICE_ID;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointPermission;
import static org.osgi.service.remoteserviceadmin.EndpointPermission.IMPORT;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_FRAMEWORK_UUID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_SERVICE_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTENTS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTENTS_EXTRA;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTERFACES;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED_CONFIGS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_INTENTS;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author David Norris
 */
public class RemoteServiceAdminImpl implements RemoteServiceAdmin {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteServiceAdminImpl.class);
    private final BundleContext bundleContext;
    private boolean closed;
    private final EventProducer eventProducer;
    private Map<ExportReference, Set<ExportRegistrationImpl>> exportedServices;
    private final Map<EndpointDescription, Set<ImportRegistrationImpl>> importedServices;

    public RemoteServiceAdminImpl(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        closed = false;
        eventProducer = new EventProducer(bundleContext);
        exportedServices = Maps.newConcurrentMap();
        importedServices = Maps.newConcurrentMap();
    }

    //an Endpoint can be shared between multiple Export Registrations
//    The Remote Service Admin service must ensure that the cor-
//    responding Endpoint remains available as long as there is at least one open Export Registration for
//    that Endpoint.
    @Override
    public Collection<ExportRegistration> exportService(final ServiceReference<?> reference, final Map<String, ?> additionalProperties) {
        Map<String, Object> properties = getPublicServiceReferenceProps(reference);
        Optional.ofNullable(additionalProperties).ifPresent(propsToAdd -> {
            propsToAdd.keySet().stream()
                    .filter(key -> !key.startsWith(".")) // ignore private properties
                    .filter(key -> !SERVICE_ID.equalsIgnoreCase(key)) //should not be overwritten
                    .filter(key -> !OBJECTCLASS.equalsIgnoreCase(key)) //should not be overwritten
                    .forEach(key -> {
                        properties.put(key, propsToAdd.get(key));
                    });
        });

        Optional<EndpointDescription> endpointDescription = createEndpointDescription(properties);
        if (endpointDescription.isPresent()) {
            checkPermission(new EndpointPermission("*", EndpointPermission.EXPORT));
            return AccessController.doPrivileged(new PrivilegedAction<List<ExportRegistration>>() {
                public List<ExportRegistration> run() {
                    ExportReferenceImpl exportReference = new ExportReferenceImpl(reference, endpointDescription.get());
                    if (!exportedServices.containsKey(exportReference)) {
                        exportedServices.put(exportReference, Sets.newConcurrentHashSet());
                    }
                    GrpcEndpoint grpcEndpoint = new GrpcEndpoint(bundleContext,exportReference);
                    ExportRegistrationImpl exportRegistration = new ExportRegistrationImpl(exportReference, RemoteServiceAdminImpl.this, grpcEndpoint);
                    exportedServices.get(exportReference).add(exportRegistration);
                    LOG.info("Added exported endpoint: {}", grpcEndpoint);
                    ImmutableList<ExportRegistration> exportRegs = ImmutableList.of(exportRegistration);
                    eventProducer.publishNotification(exportRegs);
                    return exportRegs;
                }
            });

        }
        return Collections.<ExportRegistration>emptyList();
    }

    @Override
    public ImportRegistration importService(EndpointDescription endpoint) {
        if (endpoint == null) {
            LOG.warn("No valid endpoint specified. Ignoring...");
            return null;
        }
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new EndpointPermission(endpoint, OsgiUtils.getUUID(bundleContext), IMPORT));
        }
        if (!endpoint.getConfigurationTypes().contains(CONFIGURATION_TYPE)) {
            LOG.info("No supported configuration type found. Not importing endpoint: {}", endpoint);
            return null;
        }
        if (!hasAvailableInterfaces(endpoint)) {
            LOG.info("No available interfaces found. Not importing endpoint: {}", endpoint);
            return null;
        }

        //TODO validate required properties
        return AccessController.doPrivileged(new PrivilegedAction<ImportRegistration>() {
            @Override
            public ImportRegistration run() {
                ImportRegistrationImpl importedEndpoint = null;
                Set<ImportRegistrationImpl> importedEndpoints = importedServices.get(endpoint);
                if (importedEndpoints == null) {
                    importedEndpoints = new HashSet<>();
                    importedServices.put(endpoint, importedEndpoints);
                }
                String[] objectClass = createImportedServiceObjectClass(endpoint);
                Hashtable<String, Object> serviceProperties = createImportedServiceProperties(endpoint);
                ClientServiceFactory clientServiceFactory = new ClientServiceFactory(endpoint, serviceProperties);
                ServiceRegistration<?> serviceReference = bundleContext.registerService(objectClass, clientServiceFactory, serviceProperties);
                ImportReferenceImpl importReference = new ImportReferenceImpl(serviceReference.getReference(), endpoint);
                importedEndpoint = new ImportRegistrationImpl(importReference, RemoteServiceAdminImpl.this);
                importedEndpoints.add(importedEndpoint);
                LOG.info("Added imported endpoint: {}", importedEndpoint);
                eventProducer.publishNotification(importedEndpoint);
                return importedEndpoint;
            }
        });
    }

    private static Hashtable<String, Object> createImportedServiceProperties(EndpointDescription endpointDescription) {
        Hashtable<String, Object> serviceProperties = new Hashtable<>();
        serviceProperties.put(SERVICE_IMPORTED, endpointDescription.getId());
        for (String key : endpointDescription.getProperties().keySet()) {
            serviceProperties.put(key, endpointDescription.getProperties().get(key));
        }
        return serviceProperties;
    }

    /**
     * Create an objectClass value from an Endpoint Description.
     *
     * @param description
     * @return the objectClass
     */
    private static String[] createImportedServiceObjectClass(EndpointDescription description) {
        List<String> ifaceList = description.getInterfaces();
        return ifaceList.toArray(new String[ifaceList.size()]);
    }

    private static boolean hasAvailableInterfaces(EndpointDescription endpoint) {
        List<String> interfaces = endpoint.getInterfaces();
        if (interfaces == null || interfaces.isEmpty()) {
            return false;
        }
//        try {
//            for (String iface : interfaces) {
//                RemoteServiceAdminImpl.class.getClassLoader().loadClass(iface);
//            }
//        } catch (ClassNotFoundException e) {
//            return false;
//        }
        return true;
    }

    @Override
    public Collection<ImportReference> getImportedEndpoints() {
        return Collections.<ImportReference>emptyList();
    }

    @Override
    public Collection<ExportReference> getExportedServices() {
        return exportedServices.keySet();
    }

    private Optional<EndpointDescription> createEndpointDescription(Map<String, Object> properties) {
        String endpointId = UUID.randomUUID().toString(); //TODO consider using stub class name
        List<String> serviceExportedConfigs = stringPlusToListFormat(properties.get(RemoteConstants.SERVICE_EXPORTED_CONFIGS));
        Set<String> exportedIntents = getExportedIntents(properties);

        if (!containsSupportedConfigurationType(serviceExportedConfigs, properties)) {
            return Optional.empty();
        }

        if (!allIntentsAreSupported(exportedIntents)) {
            return Optional.empty();
        }

        List<String> exportedInterfaces = getExportedInterfaces(properties);
        properties.put(ENDPOINT_ID, endpointId);
        properties.put(OBJECTCLASS, exportedInterfaces.toArray(new String[exportedInterfaces.size()]));
        properties.put(SERVICE_IMPORTED_CONFIGS, new String[]{CONFIGURATION_TYPE});
        properties.put(SERVICE_INTENTS, exportedIntents);
        properties.put(ENDPOINT_SERVICE_ID, properties.get(SERVICE_ID));
        properties.put(ENDPOINT_FRAMEWORK_UUID, OsgiUtils.getUUID(bundleContext));

        return Optional.of(new EndpointDescription(properties));
    }

    private static boolean containsSupportedConfigurationType(List<String> serviceExportedConfigs, Map<String, Object> properties) {
        if (!serviceExportedConfigs.isEmpty()) {
            if (!serviceExportedConfigs.contains(CONFIGURATION_TYPE)) {
                LOG.info("Can not export service (no supported configuration type specified): {}", properties);
                return false;
            }
        }
        return true;
    }

    private static boolean allIntentsAreSupported(Set<String> exportedIntents) {
        if (!exportedIntents.stream().allMatch(intent -> SUPPORTED_INTENTS.contains(intent))) {
            LOG.info("Can not export service, contains unsupported intents", exportedIntents);
            return false;
        }
        return true;
    }

    // may need to merge with service.exported.intents.extra
    private static Set<String> getExportedIntents(Map<String, Object> properties) {
        Set<String> exportedIntents = Sets.newHashSet(GRPC_INTENT); // add default intent
        Object serviceExportedIntents = properties.get(SERVICE_EXPORTED_INTENTS);
        Object exportedIntentsExtra = properties.get(SERVICE_EXPORTED_INTENTS_EXTRA);
        if (serviceExportedIntents != null) {
            stringPlusToListFormat(serviceExportedIntents).stream()
                    .forEach(exportedIntent -> exportedIntents.add(exportedIntent));
        }
        if (exportedIntentsExtra != null) {
            stringPlusToListFormat(exportedIntentsExtra).stream()
                    .forEach((exportedIntent) -> exportedIntents.add(exportedIntent));
        }
        return exportedIntents;
    }

    //service.exported.interfaces must be set
    //The list members must all be contained in the types listed in the objectClass service property from the Service Reference
    private List<String> getExportedInterfaces(Map<String, ?> exportProperties) {
        List<String> providedInterfaces = stringPlusToListFormat(exportProperties.get(OBJECTCLASS));
        if (providedInterfaces.isEmpty()) {
            throw new IllegalArgumentException("Can not export service (no provided interfaces)");
        }
        List<String> exportedInterfaces = stringPlusToListFormat(exportProperties.get(SERVICE_EXPORTED_INTERFACES));
        if (exportedInterfaces.isEmpty()) {
            throw new IllegalArgumentException("Can not export service (no exported interfaces)");
        }
        if (exportedInterfaces.size() == 1 && exportedInterfaces.get(0).equals("*")) {
            return providedInterfaces;
        }
        if (!providedInterfaces.containsAll(exportedInterfaces)) {
            LOG.error("Exported interfaces not all implemented by service {}, implemented interfaces {}", exportedInterfaces, providedInterfaces);
            throw new IllegalArgumentException("Exported interfaces not all implemented by service");
        }
        return exportedInterfaces;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    private void checkPermission(EndpointPermission permission) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(permission);
        }
    }

    //TODO not sure about this implementation
    void removeImportRegistration(EndpointDescription endpointDescription) {
        importedServices.remove(endpointDescription);
    }

    void remoteExportRegistration(ExportReferenceImpl exportReferenceImpl) {
        exportedServices.remove(exportReferenceImpl);
    }

}
