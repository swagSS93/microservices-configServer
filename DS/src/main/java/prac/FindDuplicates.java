
/*arr = [app , bb , app , bb, bb]

        duplicate elements - count
        app = 2
        bb = 3*/

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FindDuplicates {
    public static void main(String[] args) {
        List<String> stringList = List.of("app" , "bb" , "app" , "bb", "bb" , "cc");

        int c = 0;
        Map<String, Integer> result = new HashMap<>();

        for(int i = 0; i < stringList.size() ; i ++){
            if(result.containsKey(stringList.get(i)))
                result.put(stringList.get(i), result.get(stringList.get(i))+1 );

            else
                result.put(stringList.get(i), 1);

        }

        System.out.println(result);
    }



}
