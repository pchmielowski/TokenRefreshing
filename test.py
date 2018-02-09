#!/usr/bin/python3
import http.client
import json
import time

conn = http.client.HTTPConnection("localhost:5000")

conn.request("POST", "/login", 
    json.dumps({ "email": "admin@example.com",
                "password": "dolphin" }))

response = conn.getresponse()
assert response.status == 200

data = response.read().decode("utf-8")
assert json.loads(data)["token"] == "ABCD"

print ("OK")