
/*Write a code snippet for identifying the string and its occurances in a given line:
        "I love to play cricket game And cricket is my favourite game. I love cricket a lot."*/

import java.util.HashMap;

public class TestString {
    public static void main(String arg[]){
        String s = "I love to play cricket game And cricket is my favourite game. I love cricket a lot.";
        String strArr[] = s.split(" ");

        HashMap<String, Integer> map = new HashMap<>();

        for(int i = 0 ; i < strArr.length ; i ++ ){
            String str = strArr[i];
            if(str.endsWith("."))
                str = str.replace(".","");
            if(!map.containsKey(str)){
                map.put(str, 1 );
            }
            else {
                map.put(str, map.get(str)+1);
            }
        }

        map.forEach((k,v) -> System.out.println(k + " " + v));
    }
}
