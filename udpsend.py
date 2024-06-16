import socket
import os
import hashlib
import struct
import time

class UDPTx:
    def __init__(self, data_len: int = 1024):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, 10485760)  # Increase send buffer size
        self.address = ('localhost', 4445)
        self.datalen = data_len

    def send_file(self, file: str):
        with open(file, 'rb') as f:
            transmission_id = 1  
            seq_number = 0
            file_size = os.path.getsize(file)
            file_name = os.path.basename(file)
            max_seq_number = -(-file_size // self.datalen)  
            max_seq_number += 1  

            print("maxSeqNumber: ", max_seq_number)
            print("fileSize: ", file_size)

            # Send initial packet
            initial_packet = struct.pack('!iii', transmission_id, seq_number, max_seq_number) + file_name.encode()
            self.sock.sendto(initial_packet, self.address)
            seq_number += 1

            # Now start sending file data
            md5 = hashlib.md5()
            while True:
                data = f.read(self.datalen)
                if not data:
                    break

                packet = struct.pack('!ii', transmission_id, seq_number) + data
                self.sock.sendto(packet, self.address)
                md5.update(data)
                seq_number += 1

                time.sleep(0.01)  # Small delay to avoid overwhelming the network

            # Send the final packet with MD5 hash
            md5_hash = md5.digest()
            final_packet = struct.pack('!ii', transmission_id, seq_number) + md5_hash
            self.sock.sendto(final_packet, self.address)


def start():
    print("Which packet size? 1K/16K/64K")
    size = (int(input("Type 1, 16 or 64: ")) * 1024) - 64

    print("Which file to send? 1MB/10MB/50MB/100MB")
    file = input("Type 1, 10, 50 or 100: ")
    file_path = f"./TestFiles/{file}MB_file"

    udp = UDPTx(size)

    for i in range(1):
        udp.send_file(file_path)
        print(f"Sent {file}MB file {i+1} times")
        time.sleep(2)
    if input("e to exit: ") != "e": start()
    udp.sock.close()

start()

