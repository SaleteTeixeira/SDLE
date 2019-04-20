BOOTSTRAP_NODE = 'tcp://localhost:8080'

from collections import deque

#my_posts = deque(32)

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


def connectBootstrap():
    import zmq
    import sys
    import os

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
    message['ip'] = get_public_ip()
    message['key'] = key.exportKey()

    sock.send_json(message)

    res = sock.recv_json()
    if res['type'] == 'new neighbors':
        for k, v in res['neighbors']:
            neighbors[k] = v


def main_loop():
    import sys

    print('Hello\nCommands:\n\t1) Create post\n\t2) View timeline\n\t3) Subscribe\n\t4) Unsubscribe\n\t5)Find friends')

    while True:
        try:
            option = int(input('Choose: '))
            if option is 1:
                print('Write a single line post.')
                post = sys.stdin.readline()
                my_posts.append(post)
                print('Success!')
            elif option is 2:
                if neighbors.__len__() == 0:
                    connectBootstrap()


                pass
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
                    connectBootstrap()


                pass
            else:
                print('Poop 1')
        except:
            print('Poop 2')


if __name__ == '__main__':
    main_loop()
