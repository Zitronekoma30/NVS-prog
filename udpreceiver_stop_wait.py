import socket
import struct
import hashlib
import time

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

    print("listening on %s:%s" % server_address)

    while True:
        data, _ = sock.recvfrom(1024*64)

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
    return receive_time

times = []
files_sent = 0

while True:
    files_sent += 1
    rcv_time = receive_file()
    times.append(rcv_time)
    print(f"Average time: {sum(times) / len(times)} ms over {files_sent} files")
