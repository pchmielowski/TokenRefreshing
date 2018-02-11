from flask import Flask, request, jsonify, logging
import redis
import json
import string
import random
import typing

r = redis.StrictRedis(host='localhost', port=6379, db=0)

app = Flask(__name__)
log = logging.getLogger('werkzeug')
log.setLevel(logging.ERROR)

def newToken():
    return ''.join(random.choices(string.ascii_uppercase + string.digits, k=10))

def isTokenValid(request) -> bool:
    actual = request.headers.get('Authorization')
    expected = r.get('token').decode()
    valid = actual == expected
    if not valid:
        print ("Comparing {} with expected {}".format(actual, expected))
    return valid

@app.route("/data", methods=['GET'])
def data():
    if not isTokenValid(request):
        return "Invalid token", 403
    if r.get('expired') == b'True':
        return "Expired token", 401
    return jsonify({ "name" : "Piotrek" })

@app.route("/refresh", methods=['POST'])
def refresh():
    if not isTokenValid(request):
        return "Invalid token", 403
    r.set('expired', False)
    token = newToken()
    r.set('token', token)
    print ('Token refreshed to ' + token)
    return jsonify({ "token" : token })

@app.route("/login", methods=['POST'])
def login():
    r.set('expired', False)
    token = newToken()
    r.set('token', token)
    return jsonify({ "token" : token })
