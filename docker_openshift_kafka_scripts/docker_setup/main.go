package main

import (
	"fmt"
	"log"
	"net/http"
)

func handler(w http.ResponseWriter, r *http.Request) {
	log.Println("receive a request")
	fmt.Fprintf(w, "<h1>Hello, I am app1!</h1> Developed using <b>Golang</b>")
}

func main() {
	http.HandleFunc("/", handler)
	http.ListenAndServe(":8080", nil)
}