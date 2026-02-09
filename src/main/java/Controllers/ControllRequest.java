package Controllers;

import static Services.ProcessRequest.*;

public class ControllRequest {
    public String requestController(String[] chunks) throws InterruptedException {
        if (chunks.length < 1) return "$-1\r\n";

        String command = chunks[0];

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
        else if (command.equalsIgnoreCase("LPUSH")) {
            return processLPush(chunks);
        }
        else if (command.equalsIgnoreCase("LRANGE")) {
            return processLrange(chunks);
        }
        else if (command.equalsIgnoreCase("LLEN")) {
            return processLlen(chunks);
        }
        else if (command.equalsIgnoreCase("LPOP")) {
            return processLpop(chunks);
        }
        else if (command.equalsIgnoreCase("BLPOP")) {
            return processBLpop(chunks);
        }
        else if (command.equalsIgnoreCase("TYPE")) {
            return processType(chunks);
        }




        return "$-1\r\n";
    }




}