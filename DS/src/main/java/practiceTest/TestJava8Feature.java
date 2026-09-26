package practiceTest;//String input = "Java articles are Awesome";
//        Find the first non-repeated character using Java 8 features

// -1 , index , -2

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TestJava8Feature {
    public static void main(String args[]){
        String input = "Java articles are Awesome";
        int count[] = new int[256];
        for(int i = 0 ; i < 256 ; i++){
            count[i] = -1;
        }

        char strChar[] = input.toCharArray();

        for(int i = 0 ; i < strChar.length ; i++){
            if(count['A' - strChar[i]] == -1 ){
                count['A' - strChar[i]] = i;
            }
            else
                count['A' - strChar[i]] = -2;
        }

        for(int i = 0 ; i < strChar.length ; i++){
            if(count['A' - strChar[i]] >= 0){
                System.out.println(strChar[i]);
                break;
            }
        }

       Map<Character, Long> charMap =  input.chars().mapToObj(i -> (char)i ).collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));

       Optional<Character> firstNonRep =  charMap.entrySet().stream().filter(e -> e.getValue() == 1).map(e -> e.getKey()).findFirst();

        Optional<Character> firstRep = charMap.entrySet().stream().filter(em -> em.getValue() > 1).map(em -> em.getKey()).findFirst();

        //Remove Character from String in Java
        Character character = 'm';
       String res = input.chars().filter(ch -> ch != character).mapToObj(c -> String.valueOf((char)c)).collect(Collectors.joining());




    }
}
