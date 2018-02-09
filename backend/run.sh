#!/bin/sh

python3 gui.py &
FLASK_APP=main.py FLASK_DEBUG=1 flask run

