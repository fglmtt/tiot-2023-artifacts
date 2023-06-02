# Experiments

## Requirements

### Entanglement-Aware Middleware for Cyber-Physical Systems

See [these](entanglement-aware-middleware) instructions.

### Labels

The rationale behind labeling is to allow for advanced scheduling policies. 
In Kubernetes, scheduling relies on labels (i.e., key-value pairs) to provide additional metadata to objects, such as Worker Nodes, thus attracting containerized applications to Worker Nodes with matching labels.
Ensure to have at least 1 Worker Nodes for each key zone of the edge-to-cloud continuum (i.e., Edge on-premises, MEC, Cloud).

Add the `dtm/zone: cloud` label to a Worker Node, say `worker-node-1`:
```
$ kubectl edit node worker-node-1
```

Then, edit the yaml file as follows:
```
apiVersion: v1
kind: Node
metadata:
  ...
  labels:
  ...
  dtm/zone: cloud
...
```

Add the `dtm/zone: mec` label to a Worker Node, say `worker-node-2`:
```
$ kubectl edit node worker-node-2
```

Then, edit the yaml file as follows:
```
apiVersion: v1
kind: Node
metadata:
  ...
  labels:
  ...
  dtm/zone: mec
...
```

Add the `dtm/zone: edge` label to a Worker Node, say `worker-node-3`:
```
$ kubectl edit node worker-node-3
```

Then, edit the yaml file as follows:
```
apiVersion: v1
kind: Node
metadata:
  ...
  labels:
  ...
  dtm/zone: edge
...
```

### Message Brokers and IIoT Devices

The physical layer includes two IIoT devices connected to an industrial machine. 
The IIoT devices communicate through the MQTT protocol, sending telemetry data associated to three sub-resources (energy consumption, battery level, and temperature). 
Each IIoT device sends a status update every second and an average payload size for each sensor information of 100 Bytes.

Deploy the physical broker, i.e., the MQTT broker being used by IIoT devices:
```
$ kubectl apply -f physical-broker.yml
```

Deploy the digital broker, i.e., the MQTT broker being used by DTs:
```
$ kubectl apply -f digital-broker.yml
```

Deploy the IIoT devices:
```
$ kubectl apply -f iiot-device-1.yml
$ kubectl apply -f iiot-device-2.yml
```

### Cyber-Physical Application

A cyber-physical application is a comprehensive construct describing digital representations (i.e., DTs) of the physical world (i.e., PTs). 
In this case, the IIoT devices are the physical counterparts.

Deploy a cyber-physical application:
```
$ ./create-app.sh <management-interface-ip>:<management-interface-port>/v1/apps app.json
```

### Grafana Dashboard

Import [this](experiments/dashboard.json) JSON file as a dashboard in Grafana to visualize what is going on in your Kubernetes cluster. 
See [these](https://grafana.com/docs/grafana/latest/dashboards/manage-dashboards/#import-a-dashboard) instructions.

## Run

Edit [this](experiments/config.json) configuration file to fine tune the experiment parameters:
```
{
	"kubeconfig": "/home/ubuntu/.kube/config",
	"clusterUrl": "http://middleware.aws",
	"prometheusURL": "http://prometheus.middleware.aws:31747",
	"iterations": 10,
	"phaseDuration": 5,
	"chaosSpecPath": "network-slowdown.yml",
	"digitalTwinTarget": "composed-digital-twin-1-mec",
	"primes": 100000,
	"rollbackAppSpecPath": "app.json",
	"rollbackAppUrl": "http://management-interface.middleware.aws:8080/v1/apps/1"
}
```

Here is the explanation of each configuration parameter:
* `kubeconfig` is the path to the kubeconfig file, which provides the credentials and connection parameters to access a Kubernetes cluster;
* `clusterUrl` is the URL of the Kubernetes cluster;
* `prometheusURL` is the URL of the Prometheus server;
* `iterations` is the number of iterations for the experiment;
* `phaseDuration` is the duration of each phase of the experiment (minutes);
* `chaosSpecPath` is the path to the YAML file defining the network slowdown phase;
* `digitalTwinTarget` is the CDT that is reconfigured in the CDT reconfiguration phase;
* `primes` is the number of prime numbers that the CDT must calculate while performing state transitions;
* `rollbackAppSpecPath` is the path to the spec to be used for restoring the cyber-physical application to its original state before the experiment was conducted;
* `rollbackAppUrl` is the URL to be used for restoring the cyber-physical application to its original state before the experiment was conducted.

Run the experiments:
```
$ go run run.go config.json
```

## Clean up

Delete the cyber-physical application:
```
$ ./delete-app.sh <management-interface-ip>:<management-interface-port>/v1/apps/<app-id>
```

Delete the IIoT devices:
```
$ kubectl delete -f iiot-device-1.yml
$ kubectl delete -f iiot-device-2.yml
```

Delete the message brokers:
```
$ kubectl apply -f physical-broker.yml
$ kubectl apply -f digital-broker.yml
```
