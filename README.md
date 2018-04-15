BackBlaze Integration
=============================

**CloudFS** provides a Rest interface to BackBlaze storage. It allows for structuring directory trees along the lines of `author`, `owner` (aka: organization) and nested contexts.
Invisible to the caller, **CloudFS** handles:
- Maintaining active authentication with **BackBlaze**
- Subsequent per-operation authN/Z
+ Network and "server-busy" recovery and failover handling
+ Configurable automated retries
  - Current configurtion will retry 4 times, waiting exponentially longer between retries
  - Sends an email to admin if retries are exhasuted
- **CloudFS** caches uploads thus users are buffered from interruptions between **Zovida** and **B2**
- **CloudFS** optimizes uploads by using a pool of web-clients when connecting to B2. The number of clients in the pool, as well as an upper bound of allowed clients, are configurable. Current configuration tested with 4 users simultaneously uploading 100-150 files each. If any individual file(s) in an upload were interrupted, none of the other files or requests are _blocked_. Only those individual files are retried
- Large file upload. Files over 5Gigs require special handling on **B2**.
+ Additional features:
  - Ability to change bucket access : _public_ or _private_
  - Zovida may grant an end user temporary access to files in _private_ buckets. The LMS specifies when access expires. This makes it possible to expose protected backblaze links to authenticated users' session.

----


### System Requirements

- Java 8 or higher
- Maven 3.5+
- Neo4J Should already be setup


## Build & Run

#### 1. Configure or set environment variables

    
```bash
GRAPHENEDB_BOLT_PASSWORD=reggae
GRAPHENEDB_BOLT_USER=neo4j
GRAPHENEDB_BOLT_URL=bolt://localhost:7476
```

Set properties in the config.json file. `$VARIABLES` may be used directly in the config file. They will be interpolated from corresponding **environment** variables. That is, for BASH:
```bash
   export VARN=value
```
The config ..:

```json
    {
    "poolSize": 5,
    "maxPoolSize": $VARN,
    "neo4jConf": {
        "urlString": "$GRAPHENEDB_BOLT_URL",
        "username": "$GRAPHENEDB_BOLT_USER",
        "..."
    },
```


Check and modify the config files:

- `src/main/resources/config.json` - ([Main config](src/main/resources/config.json))
- `src/test/resources/config.json` - ([Test config](src/test/resources/config.json))

#### 2a. Scripted build and run:

```
    # Start:
        b2intgr/bin/cloudfs.sh start
    
    # Stop:
        b2intgr/bin/cloudfs.sh stop
        
    # Test:
        b2intgr/bin/cloudfs.sh test
        
    # Clean:
        b2intgr/bin/cloudfs.sh clean
```

#### 2b. Build with maven
```bash

    # Build and run
        mvn clean compile -Dmaven.test.skip=true camel:run
        # Ctrl-C to stop

    # Compile a runnable (fat) JAR
        mvn clean compile package -Dmaven.test.skip=true 
        
        # Run
        java $JAVA_TOOL_OPTIONS -jar target/b2intgr-[ver].jar
        
        # Ctrl-C to stop

    # Test
        mvn test 

    # Clean
        mvn clean         
```

