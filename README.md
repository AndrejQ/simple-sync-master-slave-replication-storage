## Synchronous replication with a single master
This is super simple **no key** and **single value** in memory storage.
The **value** change on the master is replicated to its slaves.

### How to start

#### Start zookeeper and app instances
```shell
docker-compose up
```
> Note that zookeeper must start first

### Api

Connect to curl-container:
```shell
docker run -it --rm \
  --network my-network \
  --name my-curls \
  curlimages/curl /bin/sh
``` 
Then try **get** and **set** api:
```shell
curl -X GET app1:8000/get
curl -X PUT app1:8000/set/123
curl -X PUT app2:8000/set/456
curl -X GET app2:8000/get
```
