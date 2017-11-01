Webserver - API Documentation
=============================
This Document gives a short overview of the REST Service which is provided by the 
Webserver. This functions are used by the Webclient but can also be used by any other
Program. 

*Please note that a the Library [Prickle](https://github.com/benhutchison/prickle) which 
is used to serialize Scala Classes to JSON and back do produce Elements names like `#id`
and the Server might fail to parse the JSON if these Elements are not present!* 

### Authentication
To be able to use all REST functions the client has to login and retrieve a JWT-Token.
The Token is valid 1 hour and should be refreshed soon enough to avoid the Forbidden error-code

* **GET** `/auth`: Returns a nonce which should be used to hash the Password with
* **POST** `/auth`: Returns the JWT-Token, POST Parameters must be: `username, passwordHash, nonce`
* **GET** `/auth/refresh`: Returns a refreshed JWT-Token, this call expects a valid JWT-Token 
in the HTTP-Headers

After acquiring a Token make sure to set the HTTP-Header `X-Authorization` and send it with every
REST-Call otherwise the Server might not return the expected results 

### BOINC RPC
The URL Root of all BOINC REST-Class is `/api/boinc` and needs the `X-Authorization` Header populated 
with a JWT-Token. Most of the Calls do mirror the XML Interface of the BOINC RPC 
calls and provice the data as JSON. 

*Please not that the BOINC Client in the Background can only sequentially process 
Requests. This leads sometimes to the issue that some Requests are stalled a couple of
seconds. The Server will not open a new Connection to the Core Client!* 

* **GET** `/`: This call will return a List of Clients
* **GET** `/config`: Some Settings from the `application.conf` 
* **GET** `/project_list`: A list of Projects (url and name) which can be added to a Client
* **GET** `/health`: A Map for all clients which indicates if the client has Connection problems or not
* **GET** `/:client/:action`
  * Values for the `:action` Parameter:
  * `tasks`: A list of active Tasks
  * `all_tasks`: A list of all Tasks in the Client 
  * `hostinfo`: Information about the Host (BOINC)
  * `network`: True or False if the core client has Network *(Apparently Deprecated in Core Client)*
  * `projects`: A List of Projects
  * `state`: The State Object, which contains the whole State Snapshot of the Core Client
  * `filetransfer`: A List of File transfers
  * `disk`: A List of Projects and their usage of the Disk
  * `ccstate`: Current Control State of the Core Client (run-modes, ...)
  * `global_prefs_override`: Settings of the Core Client (Same Data which is displayed by the BOINC Manager under Settings)
  * `statistics`: Statistics Object which contains all Projects
  * `messages`: List of Log Message from the Core Client
  * `notices`: Notices from Projects and the Core Client

* **POST** `/:client/:action`
  * Values for the `:action` Parameter:
  * `project`: Adds a new Project to the Core Client. Post Parameters: `projecturl, user, password`
  * `projects`: Change the Project state of a Project
  * `run_mode`: Change the run-mode of the BOINC Client
  * `cpu`: Change the mode of the CPU 
  * `gpu`: Change the mode of the GPU
  * `network`: Change the mode of the Network usage
  * `global_prefs_override`: Sets the Settings of the Core Client _(Core Client will not automatically apply it!)_
  
* **POST** `/:client/tasks/:task`: Change the state of the given Task

* **PATCH** `/:client/global_prefs_override`: Let's the client reload the Settings file
  
### BOINC WebRPC
*This is currently not exported by the Webserver!*
