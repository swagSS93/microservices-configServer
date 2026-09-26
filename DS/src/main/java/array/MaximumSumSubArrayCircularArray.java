package array;/*

Given a circular integer array, find a contiguous subarray with the largest sum in it.

Input : [2, 1, -5, 4, -3, 1, -3, 4, -1]
Output: 6
Explanation: Subarray with the largest sum is [4, -1, 2, 1] with sum 6.

Input : [8, -7, -3, 5, 6, -2, 3, -4, 2]
Output: 18
Explanation: Subarray with the largest sum is [5, 6, -2, 3, -4, 2, 8] with sum 18.

*/


import java.util.HashSet;

public class LargestSumSubarray {

    public static void main(String arg[]){


        int arr[] = {2, 1, -5, 4, -3, 1, -3, 4, -1};
        int cSum = 0, max = 0;
        HashSet<Integer> set = new HashSet<>();

        for(int i = 0 ; i < arr.length ; i++){
            if( i == (arr.length - 1)){
                i = 0;
            }
            cSum = cSum + arr[i];
            if(set.contains(arr[i]))
                return;
            set.add(arr[i]);

            if(cSum < arr[i])
                cSum = arr[i];
            if(cSum > max)
                max = cSum;



        }

        System.out.println(max);
    }
}
