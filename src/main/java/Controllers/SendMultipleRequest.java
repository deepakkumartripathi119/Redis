package Controllers;

import utils.Parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SendMultipleRequest {
    static ControllRequest controllRequest = new ControllRequest();
    static Parser parser = new Parser();

    public void multipleResponse(Socket clientSocket) {
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
                String[] chunks = parser.parse(inputString).toArray(new String[0]);
                String parsedString = controllRequest.requestController(chunks);
                outputStream.write(parsedString.getBytes());
                outputStream.flush();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
