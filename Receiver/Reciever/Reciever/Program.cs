using System;
using System.Net;
using System.Net.Sockets;
using System.Text;

namespace Reciever
{
    class Program
    {
        private const int listenPort = 11000;
        static void Main(string[] args)
        {
            UdpClient udpClient = new UdpClient(listenPort);
            try
            {
                // This will block until a message is received
                Console.WriteLine("Waiting for UDP message...");
                IPEndPoint remoteEP = null;
                byte[] receivedData = udpClient.Receive(ref remoteEP);

                string receivedMessage = Encoding.ASCII.GetString(receivedData);
                Console.WriteLine($"Received message: {receivedMessage} from {remoteEP.Address}");

            }
            catch (Exception e)
            {
                Console.WriteLine(e.ToString());
            }
            finally
            {
                udpClient.Close();
            }
        }
    }
}
