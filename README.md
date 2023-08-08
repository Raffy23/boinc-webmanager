Boinc Webmanager
=====================

The Boinc Webmanager provides a simple Web UI for multiple Boinc Clients, similar to any other 
Account Management Website like BAM. In contrast to these Websites the Server needs direct access
to the BOINC core client and is able to view Tasks and has directly control over the client. The
Boinc Webmanager is compatible with most **7.x.x** BOINC Versions

## Features
* Display multiple Clients in a simple Dashboard View
* Display Projects / Workunits and other data similar to the BOINC Manager
* Limited control over Workunits and Projects (suspend, delete, ...)
* Auto Discovery for BOINC Clients in local network

## Building
The Boinc Webmanager currently needs scala **2.13.1** and sbt **1.3.7**. 
All other Dependencies are managed by SBT. To build the Project follow theses steps: 

 - Execute following commands to Build the Javascript Client: `clientJS/fullOptJS`
 - Copy `boinc-webmanager--opt.js` and `boinc-webmanager--jsdepts.min.js` into the `jvm/src/main/resources/web-root/` Folder
 - *(Optional) gzip the `*.js` Files to lower the amount of bytes which are transferred*
 - Run sbt in Project folder and execute `serverJVM/universal:packageBin`, this wil build a Fat-Jar in the following Folder: `jvm/target/scala-1.12/`

*Note: this project does use the [native-packager](https://github.com/sbt/sbt-native-packager) and 
therefore also linux/windows packages and a docker image can be build*

## Installation
1. Extract the .zip or .tar.gz File to any Directory which is writable for the Application
2. Create a `application.conf` file with the Settings *(See the next section for more details)*
3. Create the SSL Certificate with the Java Keytool
4. Run it with `java -jar Boinc-Webmanager-assembly-XXXX.jar`

After these Steps the Web UI should be reachable under `https://127.0.0.1:8080` or the specified address
in the configuration file.

## Configuration
A sample configuration is available [here](doc/configuration/application.conf). Additionally the boinc
core client may also need additional configuration as shown below: 

### BOINC Core Client Configuration
The Boinc Webmanager can only connect to a Core Client which does allow Remote Connections.
To allow such connections the `cc_config.xml` File of the Core Client must be modified buy setting
the property `allow_remote_gui_rpc`to `1`:

*Sample cc_config.xml File:*
```xml
<cc_config>
  <options>
    <allow_remote_gui_rpc>1</allow_remote_gui_rpc>
  </options>
</cc_config>    
```

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
The web server can also be used with a custom client, which only has to support JSON or messagepack
encoding.
The Server API is documented [here](doc/Server-API.md)

## Issues
* Page broken on Mobile Devices, minimal width is ~1120px
* Self-Signed Certificate causes Unsafe Error in Chrome / Firefox / ...
