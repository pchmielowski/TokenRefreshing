#!/usr/bin/python3
import http.client
import json
import time

conn = http.client.HTTPConnection("localhost:5000")

conn.request("GET", "/")

print (conn.getresponse().read().decode("utf-8"))
