import java.net.DatagramSocket;
import java.net.InetAddress;

@FunctionalInterface
public interface ReliableSender {
    boolean send(DatagramSocket socket, InetAddress clientAddress, int clientPort, String body, int seqNum);
}

