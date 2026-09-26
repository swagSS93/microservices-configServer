//List of Input Words :
//
//        listen
//        pot
//        take
//        opt
//        silent
//        top
//        give
//
//        Output : Print sets of anagrams (listen, silent), (pot, top, opt)


import java.util.*;
import java.util.stream.Collectors;

public class Anagram {
    public static void main(String arg[]){
        List<String> words = List.of("listen", "pot", "take", "opt", "silent", "top" , "give");
        int arr[] = new int[words.size()];
        Set<String> set = new HashSet<>();

//        Map<String, List<String>> anagrams =
//                words.stream().collect(Collectors.groupingBy(w -> sortChars(w)));
        for(int i = 0; i < words.size(); i++){
            for(int j = 1; j < words.size(); j++){
                if(words.get(i) != words.get(j) && words.get(i).length() == words.get(j).length()){

                    if((arr[i] == 0 || arr[j] == 0) && checkAnagram(words.get(i),words.get(j))){
                        set.add(words.get(i));
                        set.add(words.get(j));
                        arr[i]++;
                        arr[j]++;
                    }
                }
            }

            if(set.size() !=0)
                System.out.println(set);
            set.clear();
        }
    }

    static boolean checkAnagram (String st1, String st2){
        int count[] = new int[256];

        for(int i = 0 ; i< st1.length() ; i++){
            count[st1.charAt(i)]++;
            count[st2.charAt(i)]--;
        }
        for(int i = 0 ; i < 256; i++){
            if(count[i] != 0)
                return false;
        }

        return true;
    }
}
