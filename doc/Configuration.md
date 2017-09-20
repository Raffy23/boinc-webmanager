Configuration
==============

Sample **application.conf**:   
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