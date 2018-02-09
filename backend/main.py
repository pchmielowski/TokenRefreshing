from flask import Flask, request, jsonify
import redis
import json

r = redis.StrictRedis(host='localhost', port=6379, db=0)

app = Flask(__name__)

@app.route("/data", methods=['GET'])
def data():
    if request.headers.get('Authorization') != r.get('token').decode('utf-8'):
        return "Invalid token", 403
    if r.get('expired') == b'True':
        return "Expired token", 401
    return jsonify({ "name" : "Piotrek" })

@app.route("/refresh", methods=['POST'])
def refresh():
    if request.headers.get('Authorization') != r.get('token').decode('utf-8'):
        return "Invalid token", 403
    r.set('expired', False)
    return jsonify({ "token" : "Piotrek" })
