Hardware
========
This is the Documentation about the Hardware Extension used by the Boinc-Webmanager to retrieve 
hardware information about the clients. The Communication is done via a custom script or binary
which must be provided to the Manager in order to use this extension.

##Configuration
Add the following lines to the `application.conf` to be able to use the extension: 
````hocon
hardware {
  # Must be set to true otherwise the route /api/hardware is not available
  enabled: true
  
  # Should be a List from boinc.hosts List
  hosts: ["PC1"]
  
  # Path to Binary 
  # For more information look at the Documentation (/doc/extension/Hardware.md)
  binary: "/hw-monitor"
  
  # Timeout in ms
  cache-timeout: 1200000
}
````


##Hardware Monitor (Binary)
To be able to use the Hardware Extension, the Binary which can read
the Data must be provided. The Server does only know how to parse the
output of following utilities: 
   * *lm-sensors*
   * *cpufreq* or *cpupower* (depends on distribution)

The binary is called in the following way: `hw-monitor <client>` where 
client parameter is the name of the client in the `application.conf` file.

The Standard Output (stdout) of the binary must be formatted as XML:
````xml
<data>
    <cpu-freq>
            TODO: Insert sample output of cpupower frequency-info -f -m 
    </cpu-freq>
    <sensors>
            TODO: Insert sample output of sensors 
    </sensors>
</data>
````
where `<cpu-freq>` and `<sensors>` do contain the output of the utilities.

##Server - API
The Hardware Extensions is found at `/api/hardware`.
* **GET** `/` - Returns a List of clients which can provide Hardware Information
* **GET** `/:client/cpufrequency` - Reports the CPu Frequency in GHz
* **GET** `/:client/sensors` - Returns a Map of all Sensors which are reported