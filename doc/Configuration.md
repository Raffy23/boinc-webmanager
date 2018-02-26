Configuration
==============
This file contains some sample Configuration files with additional comments how the
Boinc Webmanager and the Core Client must be configured to function properly

# Boinc Webmanager
Sample **application.conf**:   
````hocon

# Default Boinc port
boinc-default-port: 31416

# True if the WebServer should run in development mode
# development: false 

# If started as service or where stdin does not block
# set this to true, otherwise server shuts down on a 
# keypress 
service-mode: false

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

# For Host grouping, leave this empty if you don't want groups
host-groups {
 "Group Red": ["PC1", "PC2"]
 "Group Blue": ["PC4"]
}
````

# BOINC Core Client Configuration
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