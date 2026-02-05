package Controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SendMultipleRequest {
    static ProcessRequest processRequest = new ProcessRequest();

    void multipleResponse(Socket clientSocket) {
        try {
            OutputStream outputStream = clientSocket.getOutputStream();
            InputStream inputStream = clientSocket.getInputStream();

            while (true) {
                byte[] input = new byte[1024];
                int byteCount = inputStream.read(input);
                if (byteCount == -1) {
                    break;
                }
                String inputString = new String(input, 0, byteCount).trim();
                String parsedString = processRequest.requestParser(inputString);
                outputStream.write(parsedString.getBytes());
                outputStream.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
