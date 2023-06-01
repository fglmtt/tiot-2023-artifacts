# Entanglement-Aware Middleware for Cyber-Physical Systems

The first objective of the entanglement-aware middleware is to manage the execution of DTs while ensuring compliance with cyber-physical application requirements. This includes selecting the most suitable configuration and deployment strategy based on the current context. The middleware proactively monitors the quality of cyber-physical entanglement, facilitate optimal deployment execution, and plan countermeasures against performance degradation.

## Requirements

### Testbed Setup

Run [`kubemake`](../kubemake) to automatically set up the tesbed.

### Go

Ensure that you have Go installed on your machine:
```
$ go version
```

If Go is not installed, follow the instructions from the [official Go website](https://go.dev/).

### Docker

Ensure that you have Docker installed on your machine:
```
$ docker --version
```

If Docker is not installed, follow the instructions from the [official Docker website](https://docs.docker.com/).

## Management Interface

The Management Interface is a RESTful API that offers endpoints to create, update, and delete cyber-physical applications.

### Swagger UI

Build the Docker image:
```
$ docker build -t <username>/<imagename> api/v1
```

**Note 1**: replace `username` with your Docker Hub username and `imagename` with the Docker image name you want to assign.

Run the Docker image:
```
$ docker run -it --rm -p 8080:8080 <username>/<imagename>
```

The previous command launches an instance of the `<username>/<imagename>` Docker image, in an interactive mode (`-it`). 
The container is configured to automatically clean up the container files on exit (`--rm`), ensuring no residual files are left. 
For networking, the host's port `8080` is mapped to the container's port `8080` (`-p 8080:8080`), enabling access to the Swagger UI service running inside the container from outside. The Swagger UI is accessible at http://localhost:8080.

### Installation

Build the Management Interface:
```
$ go build -o management-interface cmd/webserver/main.go 
```

### Usage
```
$ ./management-interface
```

## Application Repository

The Application Repository stores cyber-physical application descriptions.

Run the Application Repository:
```
$ docker run -it --rm -p 2379:2379 -e ALLOW_NONE_AUTHENTICATION=yes bitnami/etcd
```

The previous command launches an instance of the `bitnami/etcd` Docker image, in an interactive mode (`-it`). 
The container is configured to automatically clean up the container files on exit (`--rm`), ensuring no residual files are left. 
For networking, the host's port `2379` is mapped to the container's port `2379` (`-p 2379:2379`), enabling access to the Etcd service running inside the container from outside.

**Note 2**: The environment variable `ALLOW_NONE_AUTHENTICATION` is set to `yes`, thus disabling any form of authentication for Etcd. However, this setting is not recommended for production deployments due to security concerns. Upon execution, you'll have a temporary, interactive Etcd instance running on your host machine, accessible via port `2379` without any authentication.

## Orchestrator

The primary objective of the Orchestrator is to keep the quality of cyber-physical entanglement within the application constraints. The Orchestrator interacts to
* Etcd to be notified whenever an application definition is created, updated, or deleted;
* Prometheus to know whenever a DT becomes disentangled;
* Kubernetes to enforce orchestration decisions.

### Installation

Build the Orchestrator:
```
$ go build -o orchestrator cmd/controller/main.go 
```

The previous command creates an executable file in your current directory.

### Usage

Run the Orchestrator:
```
$ ./orchestrator \
    --prometheus-url=<prometheus-url> \
    --prometheus-interval=<prometheus-interval> \
    --etcd-url=<etcd-url> \
    --kubeconfig=<kubeconfig-path>
```

Here is the explanation of each flag:
* `prometheus-url` is the URL where Prometheus is running. The default value is http://localhost:9090;
* `prometheus-interval` is the interval (in seconds) at which Prometheus scrapes the metrics. The default value is 10;
* `etcd-url` is the URL where Etcd is running. The default value is http://localhost:2379;
* `kubeconfig` is the absolute path to the kubeconfig file. The default value is ~/.kube/config.
