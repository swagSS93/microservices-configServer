package string;// interger array 3 7 5 2 8
// target = 9
// 7 2


import java.util.HashMap;
import java.util.HashSet;

public class Test {

    public static  void main(String ars[]){
        int arr[] = {3, 5 , 7 , 2 , 8, 2}; //5+8 - 21 = 8
        int target = 21;
        HashMap<Integer,Integer> map = new HashMap<>();
        //set.add(arr[0]);

        for(int i = 0; i < arr.length ; i++){

            for(int j = 0; j < arr.length ; j++){
                if( i != j ){
                    int val = target - (arr[i] + arr[j]);
                    if(!map.containsKey(j) && map.containsValue(val) ){
                        System.out.println(val + " " + arr[i] + " " + arr[j]);
                        return;
                    }
                    else if(!(map.containsValue(arr[j]))){
                        map.put(j, arr[j]);
                    }
                }
                }



        }




    }


}
