BackBlaze Integration
=============================

**CloudFS** provides a Rest interface to BackBlaze storage. It allows for structuring directory trees along the lines of `author`, `owner` (aka: organization) and nested contexts.
Invisible to the caller, **CloudFS** handles:
- Maintaining active authentication with **BackBlaze**
- Subsequent per-operation authN/Z
+ Network and "server-busy" recovery
+ Configurable automated retries
- Current config will retry 4 times, waiting exponentially longer between retries
- Sends email to admin if retries exhasuted
- **CloudFS** caches uploads thus users are buffered from interruptions between **Zovida** and  **B2**
- **CloudFS** optimizes uploads by using a pool of web-clients when connecting to B2. The number of clients in the pool, as well as an upper threshold of allowed clients, are configurable. Current configuration tested with 4 users simultaneously uploading 100-150 files each. If any individual file(s) in an upload were interrupted, none of the other files or requests are _blocked_. Only those individual files are retried
- Large file upload. Files over 5Gigs require special handling on **B2**.
+ Additional featiures:
- Ability to change bucket access : _public_ or _private_
- Zovida may grant an end user temporary access to files in _private_ buckets. The LMS specifies when access expires. This makes it possible to expose protected backblaze links to authenticated users' session.

----

To build:

    mvn clean compile package -Dmaven.test.skip=true

To run from command line:

    mvn camel:run
    
 Or

    java $JAVA_OPTS -jar target/b2intgr-0.0.1.jar
    
    
    
    
    
    heroku create mardev --remote mardev
    heroku create myapp-staging --remote staging
    heroku create myapp-production --remote production
    
    
    git remote add b2intgr-heroku https://git.heroku.com/mardev.git
    git push b2intgr-heroku b2intgr:master
    
    git push staging master
    git push mardev master
    
    
    heroku open --app mardev
    
    
    ```
    heroku addons:create graphenedb:dev-free
    
    ```
    
    To build:
    
    mvn clean compile package
    
    To run:
    
    - From command line:
    
    mvn camel:run
    
    

    


To build:

    mvn clean compile package

To run:

- From command line:

    mvn camel:run


The deployed app is intended to run on Heroku VM, in a shared dyno with the NodeJS server. b2intgr provides a file-manipulation interface to the Node app.

- When run via deployment ...:

	- The Procfile is invoked and executes: `bin/start_servers.sh`
	- This BASH script, in turn, starts b2intgr by executing the FAT jar:
		`java $JAVA_OPTS -jar target/b2intgr-0.0.1.jar`
