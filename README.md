# JRoaming

## service-discovery.yml
```
application: client-server
service-signature: service-signature-1
service-parallelism: 4
standalone-server: true
bind-addresses:
- locality: remote
  connection-address: 123.123.121.131:8491
- locality: inter-process

services:
- service-signature: service-signature-2
  locality: inter-process
- service-signature: service-signature-3
  locality: remote
  connection-address: 123.123.121.131:8491
- service-signature: service-signature-4
  locality: in-process
```