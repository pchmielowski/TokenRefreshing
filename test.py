#!/usr/bin/python3
import http.client
import json
import time
import redis

r = redis.StrictRedis(host='localhost', port=6379, db=0)
conn = http.client.HTTPConnection("localhost:5000")

# conn.request("POST", "/login", 
#     json.dumps({ "email": "admin@example.com",
#                 "password": "dolphin" }))

# response = conn.getresponse()
# assert response.status == 200

# data = response.read().decode("utf-8")
# assert json.loads(data)["token"] == "ABCD"

# print ("OK")

# Valid token - 200 OK
r.set('token', 'ABCD0123')
r.set('valid', True)
conn.request("GET", "/data", "", { 'Authorization': 'ABCD0123' })
response = conn.getresponse()
assert response.status == 200

# Invalid token - 401
r.set('token', 'ABCD0123')
r.set('valid', False)
conn.request("GET", "/data", "", { 'Authorization': 'ABCD0123' })
response = conn.getresponse()
assert response.status == 401

# refresh_token("ABCD0123")
# assert 200
# get_data("QWER9876") # from response
# assert 200

# get_data("ABCD0123")
# assert 403
