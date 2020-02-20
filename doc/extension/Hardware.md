Hardware
========
This is the Documentation about the Hardware Extension used by the Boinc-Webmanager to retrieve 
hardware information about the clients. The Communication is done via a custom script or binary
which must be provided to the Manager in order to use this extension.

## Configuration
See [here](../configuration/conf/hardware.conf) for a detailed configuration of the client.

## Hardware Monitor (Binary)
To be able to use the Hardware Extension, the Binary which can read
the Data must be provided. The Server does only know how to parse the
output of following utilities: 
   * *lm-sensors*
   * *cpufreq* or *cpupower* (depends on distribution)

The binary is called in the following way: `hw-monitor <client>` where 
client parameter is the name of the client in the `application.conf` file.

The Standard Output (stdout) of the binary or script must be formatted as XML:
````xml
<data>
    <cpu-freq>
analyzing CPU 0:
  current CPU frequency: 3.30 GHz (asserted by call to kernel)
    </cpu-freq>
    <sensors>
coretemp-isa-0000
Adapter: ISA adapter
Physical id 0:  +45.0°C  (high = +85.0°C, crit = +105.0°C)
Core 0:         +43.0°C  (high = +85.0°C, crit = +105.0°C)
Core 1:         +40.0°C  (high = +85.0°C, crit = +105.0°C)
Core 2:         +38.0°C  (high = +85.0°C, crit = +105.0°C)
Core 3:         +45.0°C  (high = +85.0°C, crit = +105.0°C)

nct6776-isa-0290
Adapter: ISA adapter
Vcore:        +0.83 V  (min =  +0.00 V, max =  +1.74 V)
in1:          +0.17 V  (min =  +0.00 V, max =  +0.00 V)  ALARM
AVCC:         +3.30 V  (min =  +2.98 V, max =  +3.63 V)
+3.30V:       +3.28 V  (min =  +2.98 V, max =  +3.63 V)
in4:          +0.52 V  (min =  +0.00 V, max =  +0.00 V)  ALARM
+5.00V:       +5.02 V  (min =  +4.75 V, max =  +5.26 V)
+12.00V:     +12.04 V  (min = +11.40 V, max = +12.62 V)
3VSB:         +3.42 V  (min =  +2.98 V, max =  +3.63 V)
Vbat:         +3.30 V  (min =  +2.70 V, max =  +3.63 V)
CPU Fan:        0 RPM  (min =    0 RPM)
Chassi Fan:     0 RPM  (min =    0 RPM)
M/B Temp:     +35.0°C  (high = +60.0°C, hyst = +50.0°C)  sensor = thermistor
CPU Temp:     +55.0°C  (high = +80.0°C, hyst = +75.0°C)  sensor = thermistor
    </sensors>
</data>
````
where `<cpu-freq>` and `<sensors>` do contain the output of the utilities as shown in the sample above.

### cpufreq
The binary must call the cpufreq binary as follows: `cpupower frequency-info -f -m` 

### Sensors
To be able to view the values of sensors in the table, the sensors utility has to be
configured first. Only values which are named like the table header will show up, the 
rest of the values will be shown in the more values dialog.

## Example script as Hardware Monitor
If this script is made executable it can be called directly by the Server otherwise bash has to 
be called with this script a parameter. Parameters can be modified with the parameters array
```bash
#!/bin/bash

echo "<data>"
echo "   <cpu-freq>"
cpupower frequency-info -f -m
echo "   </cpu-freq>"
echo "   <sensors>"
sensors
echo "   </sensors>"
echo "</data>"

```



## Server - API
The Hardware Extensions is found at `/api/hardware`.
* **GET** `/` - Returns a List of clients which can provide Hardware Information
* **GET** `/:client/cpufrequency` - Reports the CPu Frequency in GHz
* **GET** `/:client/sensors` - Returns a Map of all Sensors which are reported