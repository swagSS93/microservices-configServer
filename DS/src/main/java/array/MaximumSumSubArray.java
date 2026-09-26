package array;

public class MaximumSubarray {
    public static void main(String arg[]){
        int arr[] = { -2, -3, 4, -1, -2, 1, 5, -3 };
        int cSum = 0, max = Integer.MIN_VALUE;

        for(int i=0; i< arr.length; i++){
            cSum = cSum + arr[i];

            if(max < cSum )
                max = cSum;

            if(cSum < 0)
                cSum = 0;

        }

        System.out.println(max);
    }
}
