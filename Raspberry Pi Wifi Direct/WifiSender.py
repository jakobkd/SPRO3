import os
import signal
import socket
import threading
import time
from multiprocessing import Pipe
import Queue

q_in = Queue.Queue()
q_out = Queue.Queue()


class GracefulKiller:
    kill_now = False

    def __init__(self):
        signal.signal(signal.SIGINT, self.exit_gracefully)
        signal.signal(signal.SIGTERM, self.exit_gracefully)
        signal.signal(signal.SIGHUP, self.exit_gracefully)

    def exit_gracefully(self, signum, frame):
        with open('kill.txt', 'w') as fpntr:
            fpntr.write('killed')
        self.kill_now = True


class UDPBroadcast:

    def __init__(self):
        self.server = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
        self.server.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        self.server.settimeout(0.2)
        self.server.bind(("", 44444))
        self.msg = b"EZRobot-123"
        udpthread = threading.Thread(target=self.run, args=())
        udpthread.daemon = True
        udpthread.start()

    def run(self):
        while True:
            self.server.sendto(self.msg, ('192.168.4.255', 37020))
            # print("UDP Sent")
            time.sleep(1)


class ThreadObject:
    commands = []

    def __init__(self, client):
        self.mClient = client
        self.p_out, self.p_input = Pipe()
        print("Got it")
        thread = threading.Thread(target=self.run, args=())
        thread.daemon = True
        thread.start()

    def run(self):
        while True:
            try:
                while not q_out.empty():
                    command = q_out.get()
                    self.mClient.send(command)
                    q_out.task_done()
                data = self.mClient.recv(1024)
                self.commands = data.split(':')
                for command in self.commands:
                    q_in.put(command)
                q_out.join()
                q_in.join()

                """raw_msg = self.p_out.recv()
                self.mClient.send(raw_msg)

                self.p_input.send(data)"""

            except socket.error as e:
                print(e)


class Robot:

    MSG_ROBOT_MOVE = 1
    MSG_ROBOT_ROTATE = 2
    MSG_ROBOT_TARGET = 3

    def __init__(self):
        self.p_out, self.p_input = Pipe()
        self.current_level = 0
        self.current_line = 0
        self.current_progression = 0

        self.target_level = 0
        self.target_line = 0
        self.target_progression = 0

        thread = threading.Thread(target=self.run, args=())
        thread.daemon = True
        thread.start()

    def run(self):
        while True:
            while not q_in.empty():
                command_raw = q_in.get()
                command = command_raw.split(',')
                if command[0] == self.MSG_ROBOT_TARGET:
                    self.target_level = command[1]
                    if self.target_level == 0:
                        self.target_line = 0
                        self.target_progression = 0
                    elif self.target_level == 1:
                        self.target_line = 0
                        self.target_progression = command[2]
                    elif self.target_level == 2:
                        self.target_line = command[2]
                        self.target_progression = command[3]

    @staticmethod
    def rotate(degrees):
        out_command = str(Robot.MSG_ROBOT_ROTATE) + ',' + str(degrees) + ':'
        q_out.put(out_command)
        q_out.join()

    @staticmethod
    def move(distance):
        out_command = str(Robot.MSG_ROBOT_MOVE) + ',' + str(distance) + ':'
        q_out.put(out_command)
        q_out.join()


class WifiSender:
    bufferSize = 1024
    PORT = 5000
    HOST = "127.0.0.1"
    disconnected = True
    client_sockets = []
    killer = GracefulKiller()

    try:
        broadcastThread = UDPBroadcast()
        robot = Robot()
        server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server_socket.bind(('', PORT))
        server_socket.listen(5)
    except socket.error as msg:
        os.system("sudo reboot")
        # Socket.close()
        print(msg)
    if server_socket is not None:
        while True:
            while disconnected:
                print("listening")
                client, addr = server_socket.accept()
                if addr is not None:
                    print("connected to {}".format(str(addr)))
                    disconnected = False
                    client_sockets.append(ThreadObject(client))


