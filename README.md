### StackLeader RSA implementation
This repository contains an implementation of the OSGi Remote Service Admin specification. It is currently a work in progress, and borrows several implementation details from the Apache Aries RSA 
 [implementation](https://github.com/apache/aries-rsa) and the Amdatu [implementation](https://bitbucket.org/amdatu/amdatu-remoteservices). Credit is owed to the authors of those implementations for their work. 

Currently the implementation includes some work on supporting gRPC services; however, what is likely is we will follow more closely to the Aries implementation 
and create an abstraction similar to their "DistributionProviders" where multiple transport implementations can be added/swapped out (e.g. protobuf, thrift, avro, json, ect). 

Credit is also owed to the Eclipse communications framework gRPC [implementation](https://github.com/ECF/grpc-RemoteServicesProvider) authors for some of the details of how to create
a service proxy for gRPC service.  

This code is and will always remain fully open source, and licensed under the Apache 2 license [link](http://www.apache.org/licenses/LICENSE-2.0).


Implementation Details:
It turns out that gRPC is not the best fit for an RSA implementation for a number of reasons, but this entire RSA implementation started as an experiment to explore how feasible it was to implement
an RSA implementation that would be capable of supporting gRPC services, so it hasn't been abandoned yet. The ECF gRPC implementation was actually discovered after this implementation had begun, 
and its great to see others sharing some of our own interest/ideas in blending these two great technologies.  

I see two possible paths forward for this work:

1. It may be something we continue to develop into a more mature RSA implementation with multiple transport implementation options, our own topology manager, and distribution providers... ect.
2. We will abandon this implementation as a fun experiment and simply adopt the Aries implementation and contribute to their project.

