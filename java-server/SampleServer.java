import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.net.*;

public class SampleServer {

    public static void main(String[] args) {
        byte[] bytes = new byte[4];
        try {
            ServerSocket serverSocket = new ServerSocket(9999);
            System.out.println("Server started....");
            while (true) {
                Socket controller = serverSocket.accept();
                System.out.println("New Controller");
                Scanner scanner = new Scanner(controller.getInputStream());
                InputStream inputStream = controller.getInputStream();
                while (true) {
                    String receivedString = "";
                    try {
                        DataInputStream dataInputStream = new DataInputStream(inputStream);
                        receivedString = dataInputStream.readUTF();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    
                    System.out.println(receivedString);

                }
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
