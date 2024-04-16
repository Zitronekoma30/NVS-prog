import socket

# Create a UDP socket
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

server_address = ('localhost', 11000)
message = b'This is the message.'

try:
    # Send data
    print(f'Sending {message}')
    sent = sock.sendto(message, server_address)

finally:
    print('Closing socket')
    sock.close()