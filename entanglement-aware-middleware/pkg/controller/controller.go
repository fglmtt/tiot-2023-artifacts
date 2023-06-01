package controller

import (
	"context"
	"dtm/pkg/webserver"
	"encoding/json"
	"fmt"
	prom "github.com/prometheus/client_golang/api"
	promv1 "github.com/prometheus/client_golang/api/prometheus/v1"
	"github.com/prometheus/common/model"
	etcd "go.etcd.io/etcd/client/v3"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/tools/clientcmd"
	"log"
	"math/rand"
	"regexp"
	"strconv"
	"time"
)

type Controller struct {
	prometheusURL      string
	prometheusInterval int
	etcdURL            string
	kubeconfig         string
}

type Metric struct {
	id          int
	digitalTwin string
	affinity    string
	value       int
}

type Event struct {
	sender string
	data   interface{}
}

func New(prometheusURL string, prometheusInterval int, etcdURL string, kubeconfig string) Controller {
	return Controller{
		prometheusURL:      prometheusURL,
		prometheusInterval: prometheusInterval,
		etcdURL:            etcdURL,
		kubeconfig:         kubeconfig,
	}
}

func newEtcdClient(etcdURL string) (*etcd.Client, error) {
	cli, err := etcd.New(etcd.Config{
		Endpoints: []string{etcdURL},
	})
	if err != nil {
		return nil, err
	}
	return cli, nil
}

func newKubernetesClient(kubeconfig string) (*kubernetes.Clientset, error) {
	config, err := clientcmd.BuildConfigFromFlags("", kubeconfig)
	if err != nil {
		return nil, err
	}

	cli, err := kubernetes.NewForConfig(config)
	if err != nil {
		return nil, err
	}
	return cli, nil
}

func newPrometheusClient(prometheusURL string) (prom.Client, error) {
	cli, err := prom.NewClient(prom.Config{
		Address: prometheusURL,
	})
	if err != nil {
		return nil, err
	}
	return cli, nil
}

func watchEtcd(ctx context.Context, etcdCli *etcd.Client, eventCh chan Event) {
	watchChan := etcdCli.Watch(ctx, "/apps", etcd.WithPrefix(), etcd.WithPrevKV())
	for {
		select {
		case resp := <-watchChan:
			for _, ev := range resp.Events {
				eventCh <- Event{
					sender: "etcd",
					data:   ev,
				}
			}
		case <-ctx.Done():
			return
		}
	}
}

/*
switch digital twin's life cycle state

case 1: do nothing (the digital twin is not ready yet, so let's wait)
case 2: do nothing (the digital twins is not bound to a physical twin yet, so let's wait)
case 3: do nothing (the digital twin is bound to a physical twin but not entangled yet, so let's wait)
case 4: raise an event (the digital twin is NOT entangled, so do something about it)
case 5: do nothing (the digital twin is entangled, best case scenario)
*/
func scrapePrometheus(ctx context.Context, promCli prom.Client, interval int, eventCh chan Event) {
	ticker := time.NewTicker(time.Duration(interval) * time.Second)
	api := promv1.NewAPI(promCli)
	query := "dt_life_cycle_state"
	for {
		select {
		case <-ticker.C:
			resp, warn, err := api.Query(ctx, query, time.Now())
			if err != nil {
				log.Println("failed to scrape prometheus")
				continue
			}
			if warn != nil {
				log.Println("prometheus warning: ", warn)
			}
			var metrics []Metric
			for _, v := range resp.(model.Vector) {
				var id int
				id, err = strconv.Atoi(string(v.Metric["dtm_id"]))
				if err != nil {
					log.Println("failed to convert dtm_id to int: ", err)
					continue
				}

				metrics = append(metrics, Metric{
					id:          id,
					digitalTwin: string(v.Metric["dtm_digitaltwin"]),
					affinity:    string(v.Metric["dtm_affinity"]),
					value:       int(v.Value),
				})
			}

			for _, m := range metrics {
				if m.value == 4 {
					eventCh <- Event{
						sender: "prometheus",
						data:   m,
					}
				}
			}
		case <-ctx.Done():
			return
		}
	}
}

func createConfig(ctx context.Context, kubeCli *kubernetes.Clientset, config webserver.Config, labels map[string]string) error {
	switch config.Type {
	case "Deployment":
		var dep appsv1.Deployment
		err := json.Unmarshal([]byte(config.Spec), &dep)
		if err != nil {
			return fmt.Errorf("failed to unmarshal deployment: %v", err)
		}

		dep.ObjectMeta.Labels = labels
		for k, v := range labels {
			dep.Spec.Template.ObjectMeta.Labels[k] = v
		}

		_, err = kubeCli.AppsV1().Deployments("default").Create(ctx, &dep, metav1.CreateOptions{})
		if err != nil {
			return fmt.Errorf("failed to create deployment: %v", err)
		}
		// TODO: add a check to wait for ready status
	case "Service":
		var svc corev1.Service
		err := json.Unmarshal([]byte(config.Spec), &svc)
		if err != nil {
			return fmt.Errorf("failed to unmarshal service: %v", err)
		}
		svc.ObjectMeta.Labels = labels
		_, err = kubeCli.CoreV1().Services("default").Create(ctx, &svc, metav1.CreateOptions{})
		if err != nil {
			return fmt.Errorf("failed to create service: %v", err)
		}
	case "ConfigMap":
		var cm corev1.ConfigMap
		err := json.Unmarshal([]byte(config.Spec), &cm)
		if err != nil {
			return fmt.Errorf("failed to unmarshal configmap: %v", err)
		}
		cm.ObjectMeta.Labels = labels
		_, err = kubeCli.CoreV1().ConfigMaps("default").Create(ctx, &cm, metav1.CreateOptions{})
		if err != nil {
			return fmt.Errorf("failed to create configmap: %v", err)
		}
	}
	return nil
}

func createDeployment(ctx context.Context, kubeCli *kubernetes.Clientset, dep webserver.Deployment, labels map[string]string) error {
	for _, config := range dep.Configs {
		err := createConfig(ctx, kubeCli, config, labels)
		if err != nil {
			return fmt.Errorf("failed to create config: %v", err)
		}
	}
	return nil
}

func createDigitalTwin(ctx context.Context, kubeCli *kubernetes.Clientset, id int, dt webserver.DigitalTwin) error {
	labels := map[string]string{
		"dtm/id":          strconv.Itoa(id),
		"dtm/digitaltwin": dt.Name,
	}
	if dt.Requirements.PreferredAffinity == "" {
		labels["dtm/affinity"] = dt.Deployments[0].Affinity
		err := createDeployment(ctx, kubeCli, dt.Deployments[0], labels)
		if err != nil {
			return fmt.Errorf("failed to create deployment: %v", err)
		}
	} else {
		for _, dep := range dt.Deployments {
			if dt.Requirements.PreferredAffinity == dep.Affinity {
				labels["dtm/affinity"] = dep.Affinity
				err := createDeployment(ctx, kubeCli, dep, labels)
				if err != nil {
					return fmt.Errorf("failed to create deployment: %v", err)
				}
			}
		}
	}
	return nil
}

func createApp(ctx context.Context, kubeCli *kubernetes.Clientset, app webserver.App) error {
	for _, dt := range app.DigitalTwins {
		err := createDigitalTwin(ctx, kubeCli, int(app.Id), dt)
		if err != nil {
			return fmt.Errorf("failed to create digital twin: %v", err)
		}
	}
	return nil
}

func waitForPodTermination(ctx context.Context, kubeCli *kubernetes.Clientset, selector string, timeout time.Duration) error {
	ticker := time.NewTicker(10 * time.Second)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			pods, err := kubeCli.CoreV1().Pods("default").List(ctx, metav1.ListOptions{
				LabelSelector: selector,
			})
			if err != nil {
				return fmt.Errorf("failed to list pods: %v", err)
			}
			if len(pods.Items) == 0 {
				return nil
			}
		case <-time.After(timeout):
			return fmt.Errorf("timeout waiting for pod termination")
		}
	}
}

func deleteDeployment(ctx context.Context, kubeCli *kubernetes.Clientset, labels map[string]string) error {
	selector := metav1.FormatLabelSelector(&metav1.LabelSelector{
		MatchLabels: labels,
	})

	deployments, err := kubeCli.AppsV1().Deployments("default").List(ctx, metav1.ListOptions{
		LabelSelector: selector,
	})
	if err != nil {
		return fmt.Errorf("failed to list deployments by label: %v", err)
	}

	for _, dep := range deployments.Items {
		err := kubeCli.AppsV1().Deployments("default").Delete(ctx, dep.Name, metav1.DeleteOptions{})
		if err != nil {
			return fmt.Errorf("failed to delete deployment: %v", err)
		}

		selector, err := metav1.LabelSelectorAsSelector(dep.Spec.Selector)
		if err != nil {
			return fmt.Errorf("failed to parse deployment selector: %v", err)
		}

		err = waitForPodTermination(ctx, kubeCli, selector.String(), 5*time.Minute)
		if err != nil {
			return fmt.Errorf("failed to wait for deployment termination: %v", err)
		}
	}

	services, err := kubeCli.CoreV1().Services("default").List(ctx, metav1.ListOptions{
		LabelSelector: selector,
	})
	if err != nil {
		return fmt.Errorf("failed to list services by label: %v", err)
	}

	for _, svc := range services.Items {
		err := kubeCli.CoreV1().Services("default").Delete(ctx, svc.Name, metav1.DeleteOptions{})
		if err != nil {
			return fmt.Errorf("failed to delete service: %v", err)
		}
	}

	configmaps, err := kubeCli.CoreV1().ConfigMaps("default").List(ctx, metav1.ListOptions{
		LabelSelector: selector,
	})
	if err != nil {
		return fmt.Errorf("failed to list configmaps by label: %v", err)
	}

	for _, cm := range configmaps.Items {
		err := kubeCli.CoreV1().ConfigMaps("default").Delete(ctx, cm.Name, metav1.DeleteOptions{})
		if err != nil {
			return fmt.Errorf("failed to delete configmap: %v", err)
		}
	}
	return nil
}

func deleteApp(ctx context.Context, kubeCli *kubernetes.Clientset, id int) error {
	labels := map[string]string{
		"dtm/id": strconv.Itoa(id),
	}
	err := deleteDeployment(ctx, kubeCli, labels)
	if err != nil {
		return fmt.Errorf("failed to delete digital twin: %v", err)
	}
	return nil
}

func updateApp(ctx context.Context, kubeCli *kubernetes.Clientset, app webserver.App) error {
	err := deleteApp(ctx, kubeCli, int(app.Id))
	if err != nil {
		return fmt.Errorf("failed to delete app: %v", err)
	}

	err = createApp(ctx, kubeCli, app)
	if err != nil {
		return fmt.Errorf("failed to create app: %v", err)
	}

	return nil
}

/*
When a put event comes in

	(1) if PrevKv != nil (i.e. the key already exists), then
		(a) delete the deployment configuration with label "dtm/id: {id}"
		(b) go to step (2)
	(2) if PrevKv == nil (i.e. the key doesn't exist), then
		(a) create the deployment configuration with preferred affinity
		(b) otherwise, deploy the first deployment configuration.

Note: a put event is triggered when

	(*) a new app is created;
	(*) an existing app is updated.

if a delete event comes in, then

	(1) delete the deployment configuration with label "dtm/id: {id}".
*/
func handleEtcdEvent(ctx context.Context, kubeCli *kubernetes.Clientset, ev *etcd.Event) error {
	switch ev.Type {
	case etcd.EventTypePut:
		var app webserver.App
		err := json.Unmarshal(ev.Kv.Value, &app)
		if err != nil {
			return fmt.Errorf("failed to unmarshal app: %v", err)
		}

		if ev.PrevKv != nil {
			err = updateApp(ctx, kubeCli, app)
			if err != nil {
				return fmt.Errorf("failed to update app: %v", err)
			}
			log.Println("updated app:", app.Id)
		} else {
			err = createApp(ctx, kubeCli, app)
			if err != nil {
				return fmt.Errorf("failed to create app: %v", err)
			}
			log.Println("created app:", app.Id)
		}
	case etcd.EventTypeDelete:
		re := regexp.MustCompile(`^/[^/]+/(\d+)`)
		strId := re.FindStringSubmatch(string(ev.Kv.Key))[1]
		id, err := strconv.Atoi(strId)
		if err != nil {
			return fmt.Errorf("failed to convert id to int: %v", err)
		}

		err = deleteApp(ctx, kubeCli, id)
		if err != nil {
			return fmt.Errorf("failed to delete app: %v", err)
		}
		log.Println("deleted app:", id)
	}
	return nil
}

func handlePrometheusEvent(ctx context.Context, etcdCli *etcd.Client, kubeCli *kubernetes.Clientset, m Metric) error {
	resp, err := etcdCli.Get(ctx, fmt.Sprintf("/apps/%d", m.id))
	if err != nil {
		return fmt.Errorf("failed to get app from etcd: %v", err)
	}

	var app webserver.App
	err = json.Unmarshal(resp.Kvs[0].Value, &app)
	if err != nil {
		return fmt.Errorf("failed to unmarshal app: %v", err)
	}

	var digitalTwin webserver.DigitalTwin
	var currentDeployment webserver.Deployment
	for _, dt := range app.DigitalTwins {
		if dt.Name == m.digitalTwin {
			digitalTwin = dt
			for _, d := range dt.Deployments {
				if d.Affinity == m.affinity {
					currentDeployment = d
					break
				}
			}
		}
	}

	var tentativeDeployment webserver.Deployment
	if len(digitalTwin.Deployments) > 1 {
		for {
			rand.Seed(time.Now().UnixNano())
			i := rand.Intn(len(digitalTwin.Deployments))
			if digitalTwin.Deployments[i].Affinity != m.affinity {
				tentativeDeployment = digitalTwin.Deployments[i]
				break
			}
		}
	} else {
		return fmt.Errorf("no other deployment to switch to")
	}

	labels := map[string]string{
		"dtm/id":          strconv.Itoa(int(app.Id)),
		"dtm/digitaltwin": digitalTwin.Name,
		"dtm/affinity":    currentDeployment.Affinity,
	}
	err = deleteDeployment(ctx, kubeCli, labels)
	if err != nil {
		return fmt.Errorf("failed to delete deployment: %v", err)
	}

	labels = map[string]string{
		"dtm/id":          strconv.Itoa(int(app.Id)),
		"dtm/digitaltwin": digitalTwin.Name,
		"dtm/affinity":    tentativeDeployment.Affinity,
	}
	err = createDeployment(ctx, kubeCli, tentativeDeployment, labels)
	if err != nil {
		return fmt.Errorf("failed to create deployment: %v", err)
	}

	log.Printf("%s switched deployment from %s to %s\n",
		digitalTwin.Name,
		currentDeployment.Affinity,
		tentativeDeployment.Affinity,
	)
	return nil
}

func (ctrl Controller) Run() error {
	etcdCli, err := newEtcdClient(ctrl.etcdURL)
	if err != nil {
		return fmt.Errorf("failed to create etcd client: %v", err)
	}
	defer etcdCli.Close()
	log.Println("etcd client created")

	promCli, err := newPrometheusClient(ctrl.prometheusURL)
	if err != nil {
		return fmt.Errorf("failed to create prometheus client: %v", err)
	}
	log.Println("prometheus client created")

	kubeCli, err := newKubernetesClient(ctrl.kubeconfig)
	if err != nil {
		return fmt.Errorf("failed to create kubernetes client: %v", err)
	}
	log.Println("kubernetes client created")

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	eventCh := make(chan Event)

	go watchEtcd(ctx, etcdCli, eventCh)
	log.Println("etcd watcher started")

	go scrapePrometheus(ctx, promCli, ctrl.prometheusInterval, eventCh)
	log.Println("prometheus scraper started")

	for {
		select {
		case event := <-eventCh:
			switch event.sender {
			case "etcd":
				ev := event.data.(*etcd.Event)
				log.Println("etcd event:", ev.Type, string(ev.Kv.Key))
				err := handleEtcdEvent(ctx, kubeCli, ev)
				if err != nil {
					log.Println("failed to handle etcd event:", err)
				}
			case "prometheus":
				log.Println("prometheus event:", event.data.(Metric))
				err := handlePrometheusEvent(ctx, etcdCli, kubeCli, event.data.(Metric))
				if err != nil {
					log.Println("failed to handle prometheus event:", err)
				}
			}
		case <-ctx.Done():
			return nil
		}
	}

	return nil
}
