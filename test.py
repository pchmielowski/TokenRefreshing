#!/usr/bin/python3
import http.client
import json
import time

conn = http.client.HTTPConnection("localhost:5000")

conn.request("POST", "/login", 
    json.dumps({ "email": "admin@example.com" }))

assert conn.getresponse().read().decode("utf-8") == "OK"

print ("OK")