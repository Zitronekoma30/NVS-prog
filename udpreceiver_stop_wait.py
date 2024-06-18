import socket
import struct
import hashlib
import time
import math

def receive_file():
    # Create a UDP socket
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    ack_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    # Increase socket receive buffer to 1MB
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 1024 * 1024)
    print('starting up on %s port %s' % ('localhost', 4445))

    server_address = ('localhost', 4444)
    server_ack_adress = ('localhost', 4447)
    sock.bind(server_address)

    md5 = hashlib.md5()

    # Initialize variables
    transmission_id = None
    max_seq_number = None
    file_name = None
    file_data = b''
    received_md5 = None

    bytes_received = 0 

    print("listening on %s:%s" % server_address)

    while True:
        data, _ = sock.recvfrom(1024*64)

        #print(f"received {len(data)} bytes")
        bytes_received += len(data)
        
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
            print(f"\r{seq_number}/{max_seq_number}", end='')
        # send ACK with seq_number
        #time.sleep(0.01)
        ack = struct.pack('!ii', 2, seq_number) # TODO: Fix rec receiving it's own ack
        ack_sock.sendto(ack, server_ack_adress)

    # Check MD5 hash
    calculated_md5 = md5.digest()

    current_time_ms = int(time.time() * 1000)
    receive_time = current_time_ms - start_time
    print(f"Time taken: {receive_time} ms")

    if calculated_md5 == received_md5:
        print('File received successfully')
        with open(file_name, 'wb') as f:
            f.write(file_data)
    else:
        print('File was not received correctly')

    sock.close()
    return receive_time, bytes_received

def store_result_in_txt(speed, size):
    size_in_mb = size / 1e6
    with open("results.txt", "a") as f:
        f.write(f"Avg. MBps for {math.floor(size_in_mb)}: {speed}\n")

times_ms = []
sizes_bytes = []
files_sent = 0

while True:
    files_sent += 1
    rcv_time, file_size = receive_file()
    times_ms.append(rcv_time)
    sizes_bytes.append(file_size)
    mbps = (sum(sizes_bytes) / 1e6) / (sum(times_ms) / 1000)
    print(f"Average MBps: {mbps} over {files_sent} files")
    if files_sent == 10:
        store_result_in_txt(mbps, sizes_bytes[0])
        times_ms = []
        sizes_bytes = []
        files_sent = 0
