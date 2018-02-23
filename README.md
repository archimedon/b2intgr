BackBlaze Integration Project
=============================


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
