Boinc Webmanager
=====================

The Boinc Webmanager provides a simple Web UI for multiple Boinc Clients, similar to any other 
Account Management Website like BAM. In contrast to these Websites the Server needs direct access
to the Boinc core client and is able to view Tasks and directly control the client

## Features
* Display multiple Clients in a simple Dashboard View
* Display Projects / Workunits and other data similar to the BOINC Manager
* Limited control over Workunits and Projects (suspend, delete, ...)
* Auto Discovery for BOINC Clients 

## Building
The Boinc Webmanager currently needs scala >= **2.12.2** and sbt >= **0.13.15** which must be installed 
before the Project can be build. All other Dependencies are managed by SBT. To build the Project
follow theses steps: 

 - Execute following commands to Build the Javascript Client: `managerJS/fullOptJS`
 - Copy `boinc-webmanager-opt.js` and `boinc-webmanager-jsdepts.min.js` into the `jvm/src/main/resources/web-root/` Folder
 - *(Optional) gzip the `*.js` Files to lower the amount of bytes which are transferred*
 - Run sbt in Project folder and execute `managerJVM/assembly`, this wil build a Fat-Jar in the following Folder: `jvm/target/scala-1.12/`

## Installation
1. Extract the .zip or .tar.gz File to any Directory which is writable for the Application
2. Create a `application.conf` file with the Settings *(See [Configuration](doc/Configuration.md) for more information)*
3. Create the SSL Certificate with the Java Keytool
4. Run it with `java -jar Boinc-Webmanager-assembly-XXXX.jar`

After these Steps you should be able to view the Client at the following URL `https://127.0.0.1:8080` 
you did not change anything from the sample configuration file below

### SSL-Certificate
The Webmanager does need a Certificate to provide a Secure Connection over https. The keystore is build
with the keytool from Java which should be present at any Java installation. Navigate to your Java Path
or use the `JAVA_HOME` Environment variable to launch the keytool over cmd or your terminal:

```bash
$ keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass <password> -validity 365 -keysize 4096
```

*Please not that the store password and the certificate password must be the same otherwise the Server
would hang*

## Server - API
A short overview of the Server API is documented [here](doc/Server-API.md)

## Issues
The Boinc Webmanager Client does not work in Internet Explorer or Microsoft Edge