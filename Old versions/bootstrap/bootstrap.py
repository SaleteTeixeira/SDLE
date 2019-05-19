import zmq
import random
import pickle

f = open('/home/salete/MIEI/ano4/SDLE/trabalho/SDLE/bootstrap.pickle', 'rb')
try:
    key_to_ip = pickle.load(f)
except EOFError:
    key_to_ip = {}
f.close()

context = zmq.Context()
sock = context.socket(zmq.REP)
sock.bind('tcp://*:8080')



f = open('/home/salete/MIEI/ano4/SDLE/trabalho/SDLE/bootstrap.pickle', 'wb')

while True:
    message = sock.recv_json()

    if message['type'] == 'send network':
        key_to_ip[message['key']] = message['ip']
        pickle.dump(key_to_ip, f, pickle.HIGHEST_PROTOCOL)

        try:
            keys = random.sample(list(key_to_ip.keys()), 5)
        except ValueError:
            keys = random.sample(list(key_to_ip.keys()), key_to_ip.__len__())

        network = {}
        for k in keys:
            if k is not message['key']:
                network[k] = key_to_ip[k]

        msg = {'type': 'new neighbors', 'neighbors': network}
        sock.send_json(msg)

    # ver se é necessário - dúvida no relatório
    elif message['type'] == 'send ip':
        key_to_ip[message['key']] = message['ip']
        pickle.dump(key_to_ip, f, pickle.HIGHEST_PROTOCOL)

        msg = {'type': 'sending ip', 'neighbor key': message['neighbor key'], 'neighbor ip': key_to_ip[message['neighbor key']]}
        sock.send_json(msg)

f.close()

