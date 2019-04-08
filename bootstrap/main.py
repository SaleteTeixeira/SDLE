import zmq

key_to_ip = {}

context = zmq.Context()

sock = context.socket(zmq.REP)
sock.bind('tcp://*:8080')

while True:
    [message, id] = sock.recv_json()

    sock.send_json(message)
