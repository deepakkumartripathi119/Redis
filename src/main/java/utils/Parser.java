package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Parser {
    public List<String> parse(String input){
        if(input == null || input.isEmpty()){
            return new ArrayList<>();
        }
        List<String> args = new ArrayList<>();
        System.out.println("Hlo");
        int ind = 0;
        int totLen = 1;
        while(totLen > 0){
            if(input.charAt(ind) == '*'){
                int endPt = input.indexOf("\r\n" , ind);
                totLen = Integer.parseInt(input.substring(ind+1 , endPt));
                ind = endPt + 2;
            }
            else if(input.charAt(ind) == '$'){
                int endPt = input.indexOf("\r\n" , ind);
                int comSize = Integer.parseInt(input.substring(ind+1 , endPt));
                args.add(input.substring(endPt + 2 , endPt + 2 + comSize));
                ind = endPt + comSize + 4;
                totLen--;
            }
        }
        System.out.println(args);
        return args;
    }
}
