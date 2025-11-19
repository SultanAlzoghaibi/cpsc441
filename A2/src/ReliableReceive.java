import java.net.DatagramSocket;

@FunctionalInterface
public interface ReliableReceive {
    String receive(DatagramSocket socket);
}
