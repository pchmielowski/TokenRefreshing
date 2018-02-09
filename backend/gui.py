#!/usr/bin/python3
# -*- coding: utf-8 -*-

import sys
from PyQt5.QtWidgets import QMainWindow, QPushButton, QApplication
import redis
import string
import random

r = redis.StrictRedis(host='localhost', port=6379, db=0)

def newToken():
    return ''.join(random.choices(string.ascii_uppercase + string.digits, k=10))

def printState():
    print (r.get('token').decode(), end='')
    print (' Expired: ' + r.get('expired').decode())
    print ()

class Example(QMainWindow):
    
    def __init__(self):
        super().__init__()
        
        self.initUI()
        
        
    def initUI(self):      

        btn1 = QPushButton("New token", self)
        btn1.move(30, 50)

        btn2 = QPushButton("Expire", self)
        btn2.move(150, 50)
      
        btn1.clicked.connect(self.newTokenButtonClicked)            
        btn2.clicked.connect(self.expireTokenButtonClicked)

        self.setGeometry(300, 300, 290, 150)
        self.setWindowTitle('Token')
        self.show()
    
    def newTokenButtonClicked(self):
        r.set('token', newToken())
        r.set('expired', False)
        printState()

    def expireTokenButtonClicked(self):
        r.set('expired', True)
        printState()
        
        
if __name__ == '__main__':
    
    app = QApplication(sys.argv)
    ex = Example()
    sys.exit(app.exec_())