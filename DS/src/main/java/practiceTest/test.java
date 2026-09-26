package practiceTest;//Given an unsorted integer array of nums, return the smallest missing positive integer. Don't use any imports, in-built functions, interfaces or classes.
//        Input: nums = [1,2,0]
//        Output: 3
//
//
//
//        Input: nums = [7,8,9,11,12]
//        Output: 1
//
//
//
//        Input: nums = [3,4,-1,1]
//        Output: 2



public class test {
    public static void main(String arg[]){
        //int arr[] = {2,1};
        //int arr[] = {7,8,9,11,12};
        int arr[] = {3,4,-1,1};
        int j = 1;
        for(int i=0 ; i < arr.length ; i++){
            if(arr[i] == j && arr[i] - j == 0){
                j++;
            }
            else if(arr[i] - j == 1 || arr[i] - j == -1  ){
                j++;
            }
        }

        System.out.println("smallest missing positive integer : "+ j);



    }
}
