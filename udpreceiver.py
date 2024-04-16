import socket
import struct
import hashlib

# Create a UDP socket
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

# Bind the socket to the port
server_address = ('localhost', 4445)
sock.bind(server_address)

# Initialize MD5 hash object
md5 = hashlib.md5()

# Initialize variables
transmission_id = None
max_seq_number = None
file_name = None
file_data = b''

print("listening on %s:%s" % server_address)

while True:
    data, _ = sock.recvfrom(4096)

    if not transmission_id:
        # This is the first packet, get transmissionId, maxSeqNumber, and file name
        transmission_id, seq_number, max_seq_number = struct.unpack('!iii', data[:12])
        file_name = data[12:].decode('utf-8', 'ignore')
        print('Receiving file:', file_name)
        print('seq_number:', seq_number)
        print('max_seq_number:', max_seq_number)
    else:
        # Regular packet, get data
        seq_number = struct.unpack('!i', data[4:8])[0]
        print(seq_number)
        file_data += data[8:]
        if (seq_number == max_seq_number): # final packet
            received_md5 = data[8:]
            break

    # Update MD5 hash with received data
    md5.update(data[8:])

# Check MD5 hash
calculated_md5 = md5.digest()
if calculated_md5 == received_md5:
    print('File received successfully')
    with open(file_name, 'wb') as f:
        f.write(file_data)
else:
    print('File was not received correctly')

sock.close()

input()