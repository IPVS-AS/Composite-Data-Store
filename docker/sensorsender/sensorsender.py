#pip install pika
import pika
#pip install redis
import redis
#pip install names
import names
import time
import json
import uuid
import random
import sys
from random import uniform

ipadress = 'localhost'

if len(sys.argv) > 1 and sys.argv[1] == 'docker':
	print ('using docker profile')
	ipadress = '192.168.209.241'

rabbitIP = ipadress
redisIP = ipadress

rediscache = redis.Redis(host=redisIP, port=6379, password='reddispwd')

credentials = pika.PlainCredentials('rabbitmq', 'rabbitmqpwd')
connection = pika.BlockingConnection(pika.ConnectionParameters(rabbitIP, 5672, '/', credentials))
channel = connection.channel()
channel.queue_declare(queue='gps')

counter = 0
switch = True
phonetype = 'android'

while True:

	id = str(uuid.uuid4())
	genderr = random.choice(["male","female"])
	
	gpsdata = {
			"id": id,
			"owner" : names.get_first_name(gender=genderr),
			"gender" : genderr,
			"phonetype" : random.choice(["android", "iphone"]),
			"gpslat": str(uniform(-180,180)),
			"gpslng": str(uniform(-90, 90))
		}
	
	message = json.dumps(gpsdata)
	channel.basic_publish(exchange='',
						routing_key='gps',
						body= message)
	print("[x] Sent Message ")
	print (message)

	# Redis
	idredis = str(uuid.uuid4())
	tempdata = {
			"id": idredis,
			"device": random.choice(["HDD","CPU","Mainboard", "RAM"]),
			"temp" : random.randint(40,90),
			"type" : 'celsius'
		}
	redisentry = json.dumps(tempdata)
	print (rediscache.getset('temp', redisentry))

	time.sleep(1)

connection.close()