import socket
import os
import hashlib
import struct
import time

class UDPTx:
    def __init__(self, data_len: int = 1024):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, 10485760)  # Increase send buffer size
        self.sock.settimeout(10)  # Set a timeout for socket operations
        self.address = ('localhost', 4444)
        self.sock.bind(('localhost', 4447))
        self.datalen = data_len

    def wait_for_ack(self, seq_num: int, packet):
        while True:
            try:
                ack, _ = self.sock.recvfrom(8)
                ack = struct.unpack('!ii', ack)
                if ack[0] == 2:  # 2 is the ACK packet type
                    break
            except socket.timeout:
                print(f"Timeout waiting for ACK for seq_num {seq_num}. Retrying...")
                self.sock.sendto(packet, self.address)
                # Resend the last packet if needed or handle the timeout as appropriate
                # For example, you can break or continue based on your retry logic
                continue
            except socket.error as e:
                print(f"Socket error: {e}. Retrying...")
                continue

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

            self.wait_for_ack(seq_number, initial_packet)  # Block until ack packet is received

            seq_number += 1

            #print("sent init")

            # Now start sending file data
            md5 = hashlib.md5()
            while True:
                #print("seqNumber: ", seq_number)
                data = f.read(self.datalen)
                if not data:
                    break

                packet = struct.pack('!ii', transmission_id, seq_number) + data
                self.sock.sendto(packet, self.address)
                #print("sent reg")
                md5.update(data)
                
                self.wait_for_ack(seq_number, packet)  # Block until ack packet is received

                seq_number += 1

                time.sleep(0.01)  # Small delay to avoid overwhelming the network

            # Send the final packet with MD5 hash
            md5_hash = md5.digest()
            final_packet = struct.pack('!ii', transmission_id, seq_number) + md5_hash
            self.sock.sendto(final_packet, self.address)

def start():
    print("Which packet size? 1K/16K/64K")
    size = (int(input("Type 1, 16 or 64: ")) * 1024) - 64

    #print("Which file to send? 1MB/10MB/50MB/100MB")
    #file = input("Type 1, 10, 50 or 100: ")
    file_sizes = ["1", "10", "50", "100"]
    for file_size in file_sizes:
        file_path = f"./TestFiles/{file_size}MB_file"
        udp = UDPTx(size)

        for i in range(10):
            udp.send_file(file_path)
            print(f"Sent {file_size}MB file {i+1} times")
            time.sleep(2)
        udp.sock.close()
    if input("e to exit: ") != "e": start()

start()
