import zmq
import sys
import os
from requests import get

my_ip = get('https://api.ipify.org').text
key_file = sys.argv[1]
if not os.path.exists(key_file):
    print('SSH key file not found!')

context = zmq.Context()
sock = context.socket(zmq.REQ)
sock.connect('tcp://localhost:8080')


# while True:
#     sock.send_json(['Hello!', idd])
#     response = sock.recv_json()
#     print(response)
