import socket
import struct
import hashlib
import time

# Create a UDP socket
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

# Increase socket receive buffer to 1MB
sock.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 1024 * 1024)

server_address = ('localhost', 4445)
sock.bind(server_address)

md5 = hashlib.md5()

# Initialize variables
transmission_id = None
max_seq_number = None
file_name = None
file_data = b''
received_md5 = None

print("listening on %s:%s" % server_address)

while True:
    data, _ = sock.recvfrom(4096)

    print(f"received {len(data)} bytes")
    
    if not transmission_id:
        # This is the first packet, get transmissionId, maxSeqNumber, and file name
        start_time = int(time.time() * 1000)
        transmission_id, seq_number, max_seq_number = struct.unpack('!iii', data[:12])
        file_name = data[12:].decode('utf-8', 'ignore')
        print('Receiving file:', file_name)
        print('seq_number:', seq_number)
        print('max_seq_number:', max_seq_number)
    else:
        # Regular packet, get data
        seq_number = struct.unpack('!i', data[4:8])[0]
        if seq_number == max_seq_number:
            print(f"final: {seq_number}")
            # get md5
            received_md5 = data[8:]
            break
        file_data += data[8:]
        md5.update(data[8:])
        print(f"seq_number: {seq_number}")

# Check MD5 hash
calculated_md5 = md5.digest()

current_time_ms = int(time.time() * 1000)
print(f"Time taken: {current_time_ms - start_time} ms")

if calculated_md5 == received_md5:
    print('File received successfully')
    with open(file_name, 'wb') as f:
        f.write(file_data)
else:
    print('File was not received correctly')

sock.close()

input()