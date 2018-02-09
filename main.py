from flask import Flask, jsonify
import redis
import json

r = redis.StrictRedis(host='localhost', port=6379, db=0)

app = Flask(__name__)

@app.route("/")
def hello():
    return r.get('foo')

@app.route("/login", methods=['POST'])
def login():
    return jsonify({ "token" : "ABCD" })