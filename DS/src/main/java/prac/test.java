//maximum subsequence sum of nth elm

public class test {
    public static void main(String arg[]){
        int a[] = {8, 5, 9, 10, 5, 6, 21, 8 };
        int n = 3;

        System.out.println(findSum(n, a));
    }

    public static int findSum(int n, int a[]){
        int  prev = a[0], max = 0;
        for(int i = 2; i < a.length; i++){
            int cSum =  prev + a[i-1] + a[i];

            max = Math.max(cSum, max);

            prev = a[i-1];

        }

        return max;
    }
}
