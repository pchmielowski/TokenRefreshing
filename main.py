from flask import Flask, request, jsonify
import redis
import json

r = redis.StrictRedis(host='localhost', port=6379, db=0)

app = Flask(__name__)

@app.route("/")
def hello():
    return r.get('foo')

@app.route("/data", methods=['GET'])
def login():
    # token = request.headers.get('Authorization')
    if r.get('valid') == b'True':
        return jsonify({ "name" : "Piotrek" })
    return "", 401