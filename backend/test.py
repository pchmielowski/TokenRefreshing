#!/usr/bin/python3
import http.client
import json
import time
import redis

r = redis.StrictRedis(host='localhost', port=6379, db=0)
conn = http.client.HTTPConnection("localhost:5000")


# Valid token - 200 OK
r.set('token', 'ABCD0123')
r.set('expired', False)
conn.request("GET", "/data", "", { 'Authorization': 'ABCD0123' })
response = conn.getresponse()
assert response.status == 200
assert response.headers["Authorization"] == 'ABCD0123'

# Expired token - 401
r.set('token', 'ABCD0123')
r.set('expired', True)
conn.request("GET", "/data", "", { 'Authorization': 'ABCD0123' })
response = conn.getresponse()
assert response.status == 401
# assert response.headers["Authorization"] == 'ABCD0123'

# Invalid token - 403
r.set('token', 'QWER5555')
r.set('expired', False)
conn.request("GET", "/data", "", { 'Authorization': 'ABCD0123' })
response = conn.getresponse()
assert response.status == 403, response.status

# Refresh token
r.set('token', 'ABCD0123')
r.set('expired', True)
conn.request("POST", "/refresh", "", { 'Authorization': 'ABCD0123' })
response = conn.getresponse()
assert response.status == 200
data = response.read().decode("utf-8")
assert json.loads(data)["token"] != 'ABCD0123'
assert r.get('expired').decode() == "False"

# Refresh token with invalid one
r.set('token', 'QWER5555')
r.set('expired', True)
conn.request("POST", "/refresh", "", { 'Authorization': 'ABCD0123' })
response = conn.getresponse()
assert response.status == 403
assert r.get('token').decode() == "QWER5555"

# Login
r.set('token', 'ABCD0123')
r.set('expired', True)
conn.request("POST", "/login")
response = conn.getresponse()
assert response.status == 200
data = response.read().decode("utf-8")
assert r.get('token').decode() == json.loads(data)["token"]
assert r.get('expired').decode() == "False"

print ("OK")
