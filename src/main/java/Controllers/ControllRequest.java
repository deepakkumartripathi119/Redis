package Controllers;

import static Services.ProcessRequest.*;

public class ControllRequest {
    public String requestParser(String input) {
        if (!input.startsWith("*")) {
            return "error: expected '*'\r\n";
        }
        String[] chunks = input.split("\r\n");
        return requestController(chunks);
    }

    public String requestController(String[] chunks) {
        if (chunks.length < 3) return "$-1\r\n";

        String command = chunks[2];

        if (command.equalsIgnoreCase("PING")) {
            return "+PONG\r\n";
        } else if (command.equalsIgnoreCase("ECHO")) {
            return processEcho(chunks);
        } else if (command.equalsIgnoreCase("SET")) {
            return processSet(chunks);
        } else if (command.equalsIgnoreCase("GET")) {
            return processGet(chunks);
        }else if (command.equalsIgnoreCase("RPUSH")) {
            return processRPush(chunks);
        }
        else if (command.equalsIgnoreCase("LRANGE")) {
            return processLrange(chunks);
        }


        return "$-1\r\n";
    }




}