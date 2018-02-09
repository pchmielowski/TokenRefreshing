#!/bin/sh

python3 gui.py &
FLASK_APP=main.py flask run

