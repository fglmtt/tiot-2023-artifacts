/*
 * webserver
 *
 * rest api
 *
 * API version: v1
 * Generated by: OpenAPI Generator (https://openapi-generator.tech)
 */

package webserver

import (
	"context"
	"net/http"
)

// DefaultApiRouter defines the required methods for binding the api requests to a responses for the DefaultApi
// The DefaultApiRouter implementation should parse necessary information from the http request,
// pass the data to a DefaultApiServicer to perform the required actions, then write the service results to the http response.
type DefaultApiRouter interface {
	AppsGet(http.ResponseWriter, *http.Request)
	AppsIdDelete(http.ResponseWriter, *http.Request)
	AppsIdGet(http.ResponseWriter, *http.Request)
	AppsIdPut(http.ResponseWriter, *http.Request)
	AppsPost(http.ResponseWriter, *http.Request)
}

// DefaultApiServicer defines the api actions for the DefaultApi service
// This interface intended to stay up to date with the openapi yaml used to generate it,
// while the service implementation can be ignored with the .openapi-generator-ignore file
// and updated with the logic required for the API.
type DefaultApiServicer interface {
	AppsGet(context.Context) (ImplResponse, error)
	AppsIdDelete(context.Context, int32) (ImplResponse, error)
	AppsIdGet(context.Context, int32) (ImplResponse, error)
	AppsIdPut(context.Context, int32, App) (ImplResponse, error)
	AppsPost(context.Context, App) (ImplResponse, error)
}