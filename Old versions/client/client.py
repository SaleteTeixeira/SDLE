from dataclasses import dataclass

BOOTSTRAP_NODE = 'tcp://localhost:8080'

from collections import deque

my_posts = []

neighbors = {}
subscribers = {}


def get_public_ip():
    from requests import get
    return get('https://api.ipify.org').text


def get_rsa_key(filename):
    from Crypto.PublicKey import RSA
    f = open(filename, 'r')
    key = RSA.importKey(f.read())
    return key


def connect_bootstrap():
    import zmq.green as zmq
    import sys
    import os
    print('Sending')

    if len(sys.argv) < 1 and not os.path.exists(sys.argv[1]):
        print('SSH key file not found')
        sys.exit(1)

    key_file = sys.argv[1]
    key = get_rsa_key(key_file)

    context = zmq.Context()
    sock = context.socket(zmq.REQ)
    sock.connect(BOOTSTRAP_NODE)

    message = {}
    message['type'] = 'send network'
    message['ip'] = str(get_public_ip())
    message['key'] = str(key.exportKey())

    sock.send_json(message)

    res = sock.recv_json()
    if res['type'] == 'new neighbors':
        print(res)
        for k in res['neighbors'].keys():
            neighbors[k] = res['neighbors'][k]
    # TODO outros tipos de mensagem (se já tiver vizinhos salta)


@dataclass
class Post():
    import datetime

    counter: int
    content: str
    timestamp: datetime.datetime


def main_loop():
    import sys
    import gevent
    import datetime

    connect_bootstrap()
    # starter = gevent.spawn(connect_bootstrap)
    # gevent.joinall([starter])
    counter = 0

    while True:
        try:
            option = int(input(
                'Hello\nCommands:\n\t1) Create post\n\t2) View timeline\n\t3) Subscribe\n\t4) Unsubscribe\n\t5) Find friends\n\nChoose: '))

            if option is 1:
                print('Write a single line post.')
                content = sys.stdin.readline()
                p = Post(counter, content, datetime.datetime.utcnow())
                my_posts.append(p)
                print('Success!')
                counter += 1
            elif option is 2:
                for key, v in subscribers:
                    for post in v['accepted']:
                        print('User: {}\nTime: {}\n{}'.format(key, post.timestamp, post.content))
            elif option is 3:
                print('Insert key')
                key = sys.stdin.readline()
                subscribers[key] = {}
                print('Subscribed')
            elif option is 4:
                print('Insert key')
                key = sys.stdin.readline()
                try:
                    del subscribers[key]
                    print('Unsubscribed')
                except:
                    print('You are not subscribed to that person')
            elif option is 5:
                if neighbors.__len__() == 0:
                    connect_bootstrap()

                pass
            else:
                print('Poop 1')
        except:
            print('Poop 2')


main_loop()
