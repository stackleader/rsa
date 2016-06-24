### StackLeader RSA implementation
This repository contains an implementation of the OSGi Remote Service Admin specification. It is currently a work in progress, and borrows several implementation details from the Apache Aries RSA 
implementation [link](https://github.com/apache/aries-rsa) and the Amdatu implementation [link](https://bitbucket.org/amdatu/amdatu-remoteservices). Credit is owed to the authors of those implementations for their work. 

Currently the implementation includes some work on supporting gRPC services; however, what is likely is we will follow more closely to the Aries implementation 
and create an abstraction similar to their "DistributionProviders" where multiple transport implementations can be added/swapped out (e.g. protobuf, thrift, avro, json, ect). 

Credit is also owed to the Eclipse communications framework gRPC implementation [link](https://github.com/ECF/grpc-RemoteServicesProvider)authors for some of the details of how to create
a service proxy for gRPC service.  

This code is and will always remain fully open source, and licensed under the Apache 2 license (link)[http://www.apache.org/licenses/LICENSE-2.0].
