from flask import Flask
import redis

r = redis.StrictRedis(host='localhost', port=6379, db=0)

app = Flask(__name__)

@app.route("/")
def hello():
    return r.get('foo')