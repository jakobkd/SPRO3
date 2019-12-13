from smbus2 import SMBus
import time

addr = 0x8  # bus address
bus = SMBus(1)  # i2c channel

numb = 10

while 1:
    bus.write_byte_data(addr, 0, numb)
    time.sleep(1)
