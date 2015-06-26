import csv, json, urllib2, hashlib, os,string,time, random
from urlparse import urlparse
from datetime import datetime

import socket
timeout=60
socket.setdefaulttimeout(timeout)

secretkey=""
publickey=""
protocol=""
hostname=""
port=""

def getConfig():
    config = json.load(open('config.json'))
    secretkey=config['secretkey']
    publickey=config['publickey']
    protocol=config['protocol']
    hostname=config['hostname']
    port=config['port']
    return secretkey,publickey,protocol,hostname,port

def makesig(timestmp, ending):
    return hashlib.sha256(publickey+timestmp+ending+secretkey).hexdigest()

def makeAuthenticatedReq(restOfUrl):
    timestmp = str(int(time.time()))
    ending = "/v2/"+restOfUrl
    endingParts = ending.split("?")
    justPathEnding = endingParts[0]
    sig = makesig(timestmp, justPathEnding)
    ul=protocol+"://"+hostname+":"+port+ending
    http_header = {
        "SBW-Key":publickey,
        "SBW-Timestamp":timestmp,
        "SBW-Signature":sig,
        "Content-Type":"application/json"
        }
    datar = {
  "is_diff": 0,
  "share_level": "PUB",
  "reports": [
    {
      "url": "http://empoweringselfimprovement.com/index.php",
      "reported_at": 1410148804,
      "type": "BADWARE_REPORTED",
      "removed_from_blacklist": 0,
      "event_data": {
        "type": "redirect",
        "info": "Domain name is hijacked by malicous TDS",
        "threat": "Malicious Traffic Distribution System",
        "verification_url": "hxxp://some.url/index.php"
      }
    }
  ]
}
    jdatar=json.dumps(datar)
    try:
        r = urllib2.Request(ul, data=jdatar, headers=http_header)
        req = urllib2.urlopen(r)
        try:
            resp=req.read()
            print "Response:",resp
        except Exception as e:
            print e
            return
    except urllib2.HTTPError as e:
        print e
        return

secretkey,publickey,protocol,hostname,port=getConfig()
makeAuthenticatedReq('add/thl')
