import socket
import os
import hashlib
import struct
from typing import Tuple

class UDPTx:
    def __init__(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.address = ('localhost', 4445)

    def send_file(self, file: str):
        with open(file, 'rb') as f:
            transmission_id = 1  # Example value
            seq_number = 0
            file_size = os.path.getsize(file)
            file_name = os.path.basename(file)
            max_seq_number = -(-file_size // 1024)  # Equivalent to Math.ceil(fileSize / 1024) in Java
            max_seq_number += 1  # Add one for the final packet with the MD5 hash

            print("maxSeqNumber: ", max_seq_number)
            print("fileSize: ", file_size)

            # Send initial packet
            initial_packet = struct.pack('!iii', transmission_id, seq_number, max_seq_number) + file_name.encode()
            self.sock.sendto(initial_packet, self.address)
            seq_number += 1

            # Now start sending file data
            md5 = hashlib.md5()
            while True:
                data = f.read(1024)
                if not data:
                    break

                packet = struct.pack('!ii', transmission_id, seq_number) + data
                self.sock.sendto(packet, self.address)
                md5.update(data)
                seq_number += 1

            # Send the final packet with MD5 hash
            md5_hash = md5.digest()
            final_packet = struct.pack('!ii', transmission_id, seq_number) + md5_hash
            self.sock.sendto(final_packet, self.address)

        self.sock.close()

udp = UDPTx()

udp.send_file('D:/test.txt')