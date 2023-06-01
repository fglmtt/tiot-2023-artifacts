package main

import (
	"dtm/pkg/webserver"
	"flag"
	"fmt"
	"log"
	"net/http"
)

func main() {
	var port int

	flag.IntVar(&port, "port", 8080, "port to listen on")
	flag.Parse()

	if port < 1 || port > 65535 {
		log.Fatal("invalid port")
	}

	log.Printf("Server started")

	DefaultApiService := webserver.NewDefaultApiService()
	DefaultApiController := webserver.NewDefaultApiController(DefaultApiService)

	router := webserver.NewRouter(DefaultApiController)

	log.Fatal(http.ListenAndServe(fmt.Sprintf(":%d", port), router))
}
