import os
from smbus2 import SMBus
from math import cos, sin, pi, floor
from adafruit_rplidar import RPLidar
import time
import os
import signal
import socket
import threading
import time
from multiprocessing import Pipe
import queue

q_in = queue.Queue()
q_out = queue.Queue()


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


class UDPComm:

    def __init__(self):
        self.server = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

        self.server.bind(("", 50000))
        self.msg = b"EZRobot-123"
        self.clientIP = None
        udpServerthread = threading.Thread(target=self.run, args=())
        udpServerthread.daemon = True
        udpServerthread.start()

    def run(self):
        while True:
            bytesAddressPair = self.server.recv(1024)
            message = bytesAddressPair[0]
            self.clientIP = bytesAddressPair[1]
            print("Client address: {}".format(clientIP))

            if self.clientIP is not None:
                self.server.sendto(self.msg, clientIP)
            # print("UDP Sent")


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
                    print(command)
                    q_in.put(command)
                q_out.join()
                q_in.join()

                """raw_msg = self.p_out.recv()
                self.mClient.send(raw_msg)

                self.p_input.send(data)"""

            except socket.error as e:
                print(e)


class WifiSender:
    def __init__(self):
        self.bufferSize = 1024
        self.PORT = 5000
        self.HOST = "127.0.0.1"
        self.disconnected = True
        self.client_sockets = []
        self.server_socket = None
        # self.killer = GracefulKiller()
        thread = threading.Thread(target=self.run, args=())
        thread.daemon = True
        thread.start()

    def run(self):
        try:
            broadcastThread = UDPBroadcast()
            # robot = Robot()
            server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            server_socket.bind(('', PORT))
            server_socket.listen(5)
        except socket.error as msg:
            # os.system("sudo reboot")
            # Socket.close()
            print(msg)
        if self.server_socket is not None:
            while True:
                while self.disconnected:
                    print("listening")
                    client, addr = self.server_socket.accept()
                    if addr is not None:
                        print("connected to {}".format(str(addr)))
                        self.disconnected = False
                        client_sockets.append(ThreadObject(client))


class Robot:
    # Setup the RPLidar
    PORT_NAME = '/dev/ttyUSB0'
    lidar = RPLidar(None, PORT_NAME)
    scan_data = [0] * 360

    # Setup I2C
    addr = 0x8  # bus address
    bus = SMBus(1)  # i2c channel

    # Define variables
    MODE_COLOR = 1
    MODE_LINE = 2
    Mode = MODE_LINE

    # initialize wifi
    # wifi = WifiSender()
    broadcast = UDPBroadcast()
    udp = UDPComm()

    try:
        print(lidar.info)
        for scan in lidar.iter_scans():
            for (_, angle, distance) in scan:
                scan_data[min([359, floor(angle)])] = distance
            # print(scan_data[30])
        # bus.write_byte_data(addr, i+1, numb)

    except KeyboardInterrupt:
        print('Stopping.')
    lidar.stop()
    lidar.disconnect()
