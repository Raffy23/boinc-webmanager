Boinc Webmanager
=====================

The Boinc Webmanager provides a simple Web UI for multiple Boinc Clients, similar to any other 
Account Management Website like BAM. In contrast to these Websites the Server needs direct access
to the Boinc core client and is able to view Tasks and directly control the client

## Features
* Display multiple Clients in a simple Dashboard View
* Show Projects / Workunits and Boinc Information from each Client
* Limited control over Workunits and Projects (suspend, delete, ...)


## Installation
1. Clone the Project from Github with `git clone`
2. Download and Install `sbt` and `scala`
3. - Execute following commands to Build the Javascript Client: `managerJS/fullOptJS`
   - Copy `boinc-webmanager-opt.js` and `boinc-webmanager-jsdepts.min.js` into the `jvm/src/main/resources/web-root/` Folder
   - Run sbt in Project folder and execute `assembly`, this wil build a Fat-Jar in the following Folder: `jvm/target/scala-1.12/`
4. Copy the Fat-Jar to any location where the Application has read / write access
5. Create a `application.conf` file with the Settings (See Configuration Section for more information)
6. Create a SSL Certificate with `"%JAVA_HOME%\keytool" -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass <password> -validity 365 -keysize 4096`
7. Run it with `java -jar Boinc-Webmanager-assembly-XXXX.jar`


After these Steps you should be able to view the Client at the following URL http://127.0.0.1:8080 
you did not change anything from the sample configuration file below

## Configuration
Sample application.conf: 
````hocon

# Default Boinc port
boinc-default-port: 31416

# True if the WebServer should run in development mode
development: false 

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
  #webroot: "./web/"
  
  # Certificate settings:
  ssl {
     keystore: "./keystore.jks"
     password: "<keystore-and-certificate-password>"
   }
}

# Settings about the Boinc hosts
boinc.hosts {
  "PC1": {address: "localhost", port: ${boinc-default-port}, password: "password" }
  "PC2": {address: "127.0.0.1", port: 9001, password: "password" }
}

# Settings for the projects which can be added to the Clients: 
boinc.projects {
  # This File can be copied from a regular Boinc installation, which is used to lookup the projects
  xml-source: "./all_projects_list.xml",
  
  # If the Project is not in the List above other projects can be added here
  custom-projects: {
    "WuProp@Home": {
      url: "http://wuprop.boinc-af.org/",
      general-area: "Datacollection of Workunits",
    }
  }
}
````