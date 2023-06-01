package main

import (
	"dtm/pkg/controller"
	"flag"
	"k8s.io/client-go/util/homedir"
	"log"
	"path/filepath"
)

func main() {
	prometheusURL := flag.String(
		"prometheus-url",
		"http://localhost:9090",
		"Prometheus URL",
	)
	prometheusInterval := flag.Int(
		"prometheus-interval",
		10,
		"Prometheus scrape interval in seconds",
	)
	etcdURL := flag.String(
		"etcd-url",
		"http://localhost:2379",
		"etcd URL",
	)

	var kubeconfig *string
	if home := homedir.HomeDir(); home != "" {
		kubeconfig = flag.String(
			"kubeconfig",
			filepath.Join(home, ".kube", "config"),
			"(optional) absolute path to the kubeconfig file",
		)

	} else {
		kubeconfig = flag.String(
			"kubeconfig",
			"",
			"absolute path to the kubeconfig file",
		)
	}

	flag.Parse()

	ctrl := controller.New(
		*prometheusURL,
		*prometheusInterval,
		*etcdURL,
		*kubeconfig,
	)
	if err := ctrl.Run(); err != nil {
		log.Fatal(err)
	}
}
