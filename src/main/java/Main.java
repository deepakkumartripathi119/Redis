import Controllers.SendMultipleRequest;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    static SendMultipleRequest sendMultipleRequest = new SendMultipleRequest();

    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");

        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        int port = 6379;
        try {
            serverSocket = new ServerSocket(port);
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            // Wait for connection from client.

            while (true) {
                clientSocket = serverSocket.accept();
                HandleClients handleClients = new HandleClients(clientSocket);

                Thread worker = new Thread(handleClients);
                worker.start();
            }


        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }
    }

    static class HandleClients implements Runnable {
        private final Socket clientSocket;
        HandleClients(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            sendMultipleRequest.multipleResponse(clientSocket);
        }
    }
}



