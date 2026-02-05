import java.sql.SQLOutput;

public class RequestParser {
    public String requestParser(String input) {
        System.out.println("input: " + input);
        if (!input.startsWith("*")) {
            return "error: expected '*'\r\n";
        }
        String[] chunks = input.split("\r\n");
        String output = "";
        if (chunks.length >= 5 && chunks[2].equalsIgnoreCase("ECHO")) {
            String data = chunks[4];
            output = output.concat("$" + data.length() + "\r\n" + data + "\r\n");
        } else {
            output = output.concat("+PONG\r\n");
        }

        return output;
    }
}
