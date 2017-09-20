Boinc Webmanager
=====================

The Boinc Webmanager provides a simple Web UI for multiple Boinc Clients, similar to any other 
Account Management Website like BAM. In contrast to these Websites the Server needs direct access
to the Boinc core client and is able to view Tasks and directly control the client

## Features
* Display multiple Clients in a simple Dashboard View
* Show Projects / Workunits and Boinc Information from each Client
* Limited control over Workunits and Projects (suspend, delete, ...)

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
2. Create a `application.conf` file with the Settings *(See [Configuration](#configuration) Section for more information)*
3. Create the SSL Certificate with the Java Keytool
4. Run it with `java -jar Boinc-Webmanager-assembly-XXXX.jar`

After these Steps you should be able to view the Client at the following URL `https://127.0.0.1:8080` 
you did not change anything from the sample configuration file below

## Configuration
Sample application.conf:  
````hocon

# Default Boinc port
boinc-default-port: 31416

# True if the WebServer should run in development mode
# development: false 

# Basic Settings of the Server
server {
  # Adress and Port on which the Server listens
  address: "127.0.0.1"
  port: 8080
  
  # Username and Password for the Login
  username: "admin"
  password: "password"
  
  # Secret is used to sign and encrypt session tokens 
  secret: "<some-long-secret>"
  
  # The Path where the Files are served (Only needed in development mode)
  # webroot: "./web/"
  
  # Certificate settings:
  ssl {
     keystore: "./keystore.jks"
     password: "<keystore-and-certificate-password>"
   }
}

# How many concurrent Requests are send to the Core Client 
boinc.connection-pool: 3

# Set the Text Encoding of the Core Client
boinc.encoding: "iso-8859-1"

# Settings about the Boinc hosts
boinc.hosts {
  "PC1": {address: "localhost", port: ${boinc-default-port}, password: "password" }
  "PC2": {address: "127.0.0.1", port: 9001, password: "password" }
}

# Settings for the projects which can be added to the Clients: 
boinc.projects {
  # This File can be copied from a regular Boinc installation, which is used to lookup the projects
  xml-source: "./all_projects_list.xml"
  
  # If the Project is not in the List above other projects can be added here
  custom-projects: {
    "WuProp@Home": {
      url: "http://wuprop.boinc-af.org/"
      organization: ""
      general-area: ""
      description: """
        WUProp@home is a non-intensive project that uses Internet-connected computers to collect workunits
        properties of BOINC projects such as computation time, memory requirements, checkpointing interval
        or report limit. You can participate by downloading and running a free program on your computer.
        """
    }
  }
}

# Settings for the Auto Discovery Service
auto-discovery {
  enabled: true

  # IP Range & Port which should be scanned
  start-ip: "192.168.1.100"
  end-ip: "192.168.1.125"
  port: ${boinc-default-port}

  # Socket Timeout
  timeout: 500

  # Timeout in Minutes in which intervall the Network should be scanned
  scan-timeout: 30

  # A List of Passwords which should be used for a Connection to the Core Client
  password: [
    "password1", "password2", "password3"
  ]

}
````

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