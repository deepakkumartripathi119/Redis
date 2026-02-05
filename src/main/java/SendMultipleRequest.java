import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SendMultipleRequest {

    void multipleResponse(Socket clientSocket)
    {
        try {
            OutputStream outputStream = clientSocket.getOutputStream();
            InputStream inputStream  = clientSocket.getInputStream();

            while(true)
            {
                byte[] input = new byte[1024];
                int byteCount = inputStream.read(input);
                String inputString = new String(input).trim();

                outputStream.write("+PONG\r\n".getBytes());
                outputStream.flush();

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
