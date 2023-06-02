package main

import (
	"net/http"
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"os/exec"
	"fmt"
	"github.com/google/uuid"
	prom "github.com/prometheus/client_golang/api"
	"github.com/prometheus/common/model"
	promv1 "github.com/prometheus/client_golang/api/prometheus/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"io"
	"io/ioutil"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/tools/clientcmd"
	"os"
	"time"
	"strconv"
	"encoding/csv"
)

type Config struct {
	Kubeconfig        string        `json:"kubeconfig"`
	ClusterUrl	  string        `json:"clusterUrl"`
	PrometheusURL     string        `json:"prometheusURL"`
	Iterations        int           `json:"iterations"`
	PhaseDuration     time.Duration `json:"phaseDuration"`
	ChaosSpecPath     string        `json:"chaosSpecPath"`
	DigitalTwinTarget string        `json:"digitalTwinTarget"`
	Primes            int           `json:"primes"`
	RollbackAppSpecPath string      `json:"rollbackAppSpecPath"`
	RollbackAppUrl string           `json:"rollbackAppUrl"`
}

func main() {
	if len(os.Args) < 2 {
		fmt.Println("usage: run <config>")
		os.Exit(1)
	}

	configPath := os.Args[1]
	if _, err := os.Stat(configPath); os.IsNotExist(err) {
		fmt.Printf("config file %s does not exist\n", configPath)
		os.Exit(1)
	}

	config, err := validateConfig(configPath)
	if err != nil {
		fmt.Printf("error validating config: %s\n", err)
		os.Exit(1)
	}
	fmt.Println("config validated")

	kubeClient, err := newKubernetesClient(config.Kubeconfig)
	if err != nil {
		fmt.Printf("error creating kubernetes or chaos client: %s\n", err)
		os.Exit(2)
	}
	fmt.Println("kubernetes client created")

	promClient, err := newPrometheusClient(config.PrometheusURL)
	if err != nil {
		fmt.Printf("error creating prometheus client: %s\n", err)
		os.Exit(3)
	}
	fmt.Println("prometheus client created")

	for i := 0; i < config.Iterations; i++ {
		uuid, err := uuid.NewRandom()
		if err != nil {
			fmt.Printf("error creating uuid: %s\n", err)
			os.Exit(4)
		}
		fmt.Printf("\niteration %d: %s\n", i, uuid)

		dir := fmt.Sprintf("results/%s", uuid)
		if err := os.MkdirAll(dir, 0755); err != nil {
			fmt.Printf("error creating directory: %s\n", err)
			os.Exit(5)
		}
		fmt.Printf("created directory %s\n", dir)

		if err := copyFile(configPath, fmt.Sprintf("%s/config.json", dir)); err != nil {
			fmt.Printf("error copying config: %s\n", err)
			os.Exit(6)
		}

		start := time.Now().Unix()

		/*
			BASELINE
		*/
		fmt.Println("baseline started: ", time.Now().Unix())
		fmt.Printf("sleeping for %d minutes\n", config.PhaseDuration)
		time.Sleep(config.PhaseDuration * time.Minute)
		fmt.Println("baseline finished:", time.Now().Unix())

		/*
			NETWORK SLOWDOWN
		*/
		fmt.Println("network slowdown started:", time.Now().Unix())

		cmd := exec.Command("kubectl", "apply", "-f", config.ChaosSpecPath)
		if err := cmd.Run(); err != nil {
			fmt.Printf("error creating network chaos: %s\n", err)
			os.Exit(7)
		}
		fmt.Printf("created network chaos from %s\n", config.ChaosSpecPath)

		fmt.Printf("sleeping for %d minutes\n", config.PhaseDuration)
		time.Sleep(config.PhaseDuration * time.Minute)

		cmd = exec.Command("kubectl", "delete", "-f", config.ChaosSpecPath)
		if err := cmd.Run(); err != nil {
			fmt.Printf("error deleting network chaos: %s\n", err)
			os.Exit(8)
		}
		fmt.Printf("deleted network chaos from %s\n", config.ChaosSpecPath)

		fmt.Println("network slowdown finished:", time.Now().Unix())

		/*
			BASELINE
		*/
		fmt.Println("baseline started:", time.Now().Unix())
		fmt.Printf("sleeping for %d minutes\n", config.PhaseDuration)
		time.Sleep(config.PhaseDuration * time.Minute)
		fmt.Println("baseline finished:", time.Now().Unix())

		/*
			DIGITAL TWIN RECONFIGURATION
		*/
		fmt.Println("digital twin reconfiguration started:", time.Now().Unix())

		svc, err := kubeClient.CoreV1().Services("default").Get(context.Background(), config.DigitalTwinTarget, metav1.GetOptions{})
		if err != nil {
			fmt.Printf("error getting digital twin service: %s\n", err)
			os.Exit(9)
		}
		nodePort := svc.Spec.Ports[1].NodePort
		url := fmt.Sprintf("%s:%d/conf", config.ClusterUrl, nodePort)

		data := map[string]int{"primeNumbersComputationCount": config.Primes}
		jsonData, err := json.Marshal(data)
		if err != nil {
			fmt.Printf("error marshalling json data: %s\n", err)
			os.Exit(9)
		}

		req, err := http.NewRequest("PUT", url, bytes.NewBuffer(jsonData))
		if err != nil {
			fmt.Printf("error creating http request: %s\n", err)
			os.Exit(10)
		}

		req.Header.Set("Content-Type", "application/json")

		fmt.Printf("sending http request to %s\n", url)
		httpClient := &http.Client{}
		resp, err := httpClient.Do(req)
		if err != nil {
			fmt.Printf("error sending http request: %s\n", err)
			os.Exit(11)
		}

		if resp.StatusCode != http.StatusNoContent {
			fmt.Printf("error response status code: %d\n", resp.StatusCode)
			os.Exit(12)
		}
		fmt.Printf("digital twin reconfigured to compute %d prime numbers\n", config.Primes)

		fmt.Printf("sleeping for %d minutes\n", config.PhaseDuration)
		time.Sleep(config.PhaseDuration * time.Minute)

		fmt.Println("digital twin reconfiguration finished:", time.Now().Unix())

		/*
			BASELINE
		*/
		fmt.Println("baseline started:", time.Now().Unix())
		fmt.Printf("sleeping for %d minutes\n", config.PhaseDuration)
		time.Sleep(config.PhaseDuration * time.Minute)
		fmt.Println("baseline finished:", time.Now().Unix())

		end := time.Now().Unix()

		/*
			GET METRICS
		*/
		for _, target := range []string{"cloud", "edge", "mec"} {
			dir := fmt.Sprintf("results/%s/%s", uuid, target)
			if err := os.MkdirAll(dir, 0755); err != nil {
				fmt.Printf("error creating directory %s: %s\n", dir, err)
				os.Exit(13)
			}

			err := getZoneMetrics(promClient, fmt.Sprintf("results/%s/%s", uuid, target), target, start, end)
			if err != nil {
				fmt.Printf("error getting metrics for %s: %s\n", target, err)
				os.Exit(14)
			}
			fmt.Printf("got metrics for %s\n", target)
		}

		digitalTwinsCloud := []string{"digital-twin-1-cloud", "digital-twin-2-cloud", "composed-digital-twin-1-cloud"}
		for _, target := range digitalTwinsCloud {
			err := getDigitalTwinsMetrics(promClient, fmt.Sprintf("results/%s/cloud", uuid), target, start, end)
			if err != nil {
				fmt.Printf("error getting metrics for %s: %s\n", target, err)
				os.Exit(14)
			}
			fmt.Printf("got metrics for %s\n", target)
		}
		
		digitalTwinsEdge := []string{"digital-twin-1-edge", "digital-twin-2-edge"}
		for _, target := range digitalTwinsEdge {
			err := getDigitalTwinsMetrics(promClient, fmt.Sprintf("results/%s/edge", uuid), target, start, end)
			if err != nil {
				fmt.Printf("error getting metrics for %s: %s\n", target, err)
				os.Exit(14)
			}
			fmt.Printf("got metrics for %s\n", target)
		}

		digitalTwinsMec := []string{"composed-digital-twin-1-mec"}
		for _, target := range digitalTwinsMec {
			err := getDigitalTwinsMetrics(promClient, fmt.Sprintf("results/%s/mec", uuid), target, start, end)
			if err != nil {
				fmt.Printf("error getting metrics for %s: %s\n", target, err)
				os.Exit(14)
			}
			fmt.Printf("got metrics for %s\n", target)
		}

		/*
			ROLLBACK
		*/
		fmt.Println("rollback started:", time.Now().Unix())

		jsonData, err = ioutil.ReadFile(config.RollbackAppSpecPath)
		if err != nil {
			fmt.Printf("error reading rollback app spec: %s\n", err)
			os.Exit(15)
		}

		req, err = http.NewRequest("PUT", config.RollbackAppUrl, bytes.NewBuffer(jsonData))
		if err != nil {
			fmt.Printf("error creating http request: %s\n", err)
			os.Exit(16)
		}

		req.Header.Set("Content-Type", "application/json")

		httpClient = &http.Client{}
		resp, err = httpClient.Do(req)
		if err != nil {
			fmt.Printf("error sending http request: %s\n", err)
			os.Exit(17)
		}

		if resp.StatusCode != http.StatusOK {
			fmt.Printf("error response status code: %d\n", resp.StatusCode)
			os.Exit(18)
		}
		fmt.Printf("sent rollback http request to %s\n", config.RollbackAppUrl)
		
		fmt.Printf("sleeping for %d minutes\n", config.PhaseDuration)
		time.Sleep(config.PhaseDuration * time.Minute)

		fmt.Println("rollback finished:", time.Now().Unix())
	}
}

func validateConfig(configPath string) (*Config, error) {
	config := &Config{}
	data, err := ioutil.ReadFile(configPath)
	if err != nil {
		return nil, err
	}

	if err := json.Unmarshal(data, config); err != nil {
		return nil, err
	}

	if config.Kubeconfig == "" {
		return nil, errors.New("kubeconfig is required")
	}
	if _, err := os.Stat(config.Kubeconfig); os.IsNotExist(err) {
		return nil, errors.New("kubeconfig does not exist")
	}

	if config.ClusterUrl == "" {
		return nil, errors.New("clusterUrl is required")
	}

	if config.PrometheusURL == "" {
		return nil, errors.New("prometheusURL is required")
	}

	if config.Iterations < 1 {
		return nil, errors.New("iterations must be greater than 0")
	}

	if config.PhaseDuration < 0 {
		return nil, errors.New("phaseDuration must be greater than 0")
	}

	if config.ChaosSpecPath == "" {
		return nil, errors.New("chaosSpecPath is required")
	}
	if _, err := os.Stat(config.ChaosSpecPath); os.IsNotExist(err) {
		return nil, errors.New("chaosSpecPath does not exist")
	}

	if config.DigitalTwinTarget == "" {
		return nil, errors.New("digitalTwinTarget is required")
	}

	if config.Primes < 1 {
		return nil, errors.New("primes must be greater than 0")
	}

	if config.RollbackAppSpecPath == "" {
		return nil, errors.New("rollbackAppSpecPath is required")
	}
	if _, err := os.Stat(config.RollbackAppSpecPath); os.IsNotExist(err) {
		return nil, errors.New("rollbackAppSpecPath does not exist")
	}

	if config.RollbackAppUrl == "" {
		return nil, errors.New("rollbackAppUrl is required")
	}

	return config, nil
}

func newKubernetesClient(kubeconfig string) (*kubernetes.Clientset, error) {
	config, err := clientcmd.BuildConfigFromFlags("", kubeconfig)
	if err != nil {
		return nil, err
	}

	kubeCli, err := kubernetes.NewForConfig(config)
	if err != nil {
		return nil, err
	}
	return kubeCli, nil
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

func copyFile(src, dst string) error {
	dstFile, err := os.Create(dst)
	if err != nil {
		return err
	}
	defer dstFile.Close()

	srcFile, err := os.Open(src)
	if err != nil {
		return err
	}
	defer srcFile.Close()

	_, err = io.Copy(dstFile, srcFile)
	return err
}

func getZoneMetrics(promClient prom.Client, dir, target string, start, end int64) error {
	queries := []string{
		"container_network_receive_bytes_total",
		"container_network_transmit_bytes_total",
	}

	startTime := time.Unix(start, 0)
	endTime := time.Unix(end, 0)
	step := time.Second * 1

	for _, query := range queries {
		updatedQuery := fmt.Sprintf("sum(rate(%s{dtm_zone=\"%s\"}[30s]))", query, target)
		fmt.Printf("query: %s\n", updatedQuery)
		result, err := doQueryRange(promClient, updatedQuery, startTime, endTime, step)
		if err != nil {
			return err
		}

		fileName := fmt.Sprintf("%s/%s-%s.csv", dir, target, query)
		err = writeResultToCSV(result.(model.Matrix), fileName)
		if err != nil {
			return err
		}

	}
	return nil
}


func getDigitalTwinsMetrics(promClient prom.Client, dir, target string, start, end int64) error {
	queries := []string{
		"dt_odte_value",
		"dt_life_cycle_state",
	}

	startTime := time.Unix(start, 0)
	endTime := time.Unix(end, 0)
	step := time.Second * 1

	for _, query := range queries {
		updatedQuery := fmt.Sprintf("%s{digital_twin_id=\"%s\"}", query, target)
		fmt.Printf("query: %s\n", updatedQuery)
		result, err := doQueryRange(promClient, updatedQuery, startTime, endTime, step)
		if err != nil {
			return err
		}

		fileName := fmt.Sprintf("%s/%s-%s.csv", dir, target, query)
		err = writeResultToCSV(result.(model.Matrix), fileName)
		if err != nil {
			return err
		}

	}

	queries = []string{
		"container_cpu_usage_seconds_total",
		"container_memory_working_set_bytes",
	}

	for _, query := range queries {
		updatedQuery := fmt.Sprintf("sum(rate(%s{pod=~\"%s-.*\"}[30s])) by (pod)", query, target)
		fmt.Printf("query: %s\n", updatedQuery)
		result, err := doQueryRange(promClient, updatedQuery, startTime, endTime, step)
		if err != nil {
			return err
		}

		fileName := fmt.Sprintf("%s/%s-%s.csv", dir, target, query)
		err = writeResultToCSV(result.(model.Matrix), fileName)
		if err != nil {
			return err
		}
	}
	return nil
}

func doQueryRange(promClient prom.Client, query string, start, end time.Time, step time.Duration) (model.Value, error) {
	api := promv1.NewAPI(promClient)
	result, warnings, err := api.QueryRange(context.Background(), query, promv1.Range{
		Start: start,
		End:   end,
		Step:  step,
	})
	if err != nil {
		return nil, err
	}
	if len(warnings) > 0 {
		fmt.Printf("warnings: %v\n", warnings)
	}
	return result, nil
}

func writeResultToCSV(result model.Matrix, fileName string) (error) {
	file, err := os.Create(fileName)
	if err != nil {
		return err
	}
	defer file.Close()

	writer := csv.NewWriter(file)
	defer writer.Flush()

	for _, sample := range result {
		for _, pair := range sample.Values {
			timestamp := pair.Timestamp.Time().Unix()
			value := pair.Value.String()
			if err := writer.Write([]string{strconv.FormatInt(timestamp, 10), value}); err != nil {
				return err
			}
		}
	}
	return nil
}
