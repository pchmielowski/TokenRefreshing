from flask import Flask, request, jsonify
import redis
import json
import string
import random

r = redis.StrictRedis(host='localhost', port=6379, db=0)

app = Flask(__name__)

def newToken():
    return ''.join(random.choices(string.ascii_uppercase + string.digits, k=10))

@app.route("/data", methods=['GET'])
def data():
    if request.headers.get('Authorization') != r.get('token').decode('utf-8'):
        print ('403 Expected: {}, actual: {}'.format(r.get('token').decode('utf-8'), request.headers.get('Authorization')))
        return "Invalid token", 403
    if r.get('expired') == b'True':
        return "Expired token", 401
    return jsonify({ "name" : "Piotrek" })

@app.route("/refresh", methods=['POST'])
def refresh():
    if request.headers.get('Authorization') != r.get('token').decode('utf-8'):
        print ('403 Expected: {}, actual: {}'.format(r.get('token').decode('utf-8'), request.headers.get('Authorization')))        
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
