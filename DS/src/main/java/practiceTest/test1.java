// + array n - arr

import java.awt.desktop.SystemEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class test {
    public static void main(String arg[]){
        int arr[] = {1,3, -5, 6, -8,10};

        if(arr[0] < 0)
            swap(arr, 0, 1);

        System.out.println(arr[0]);

        for(int i=1; i< arr.length ; i++){
            System.out.println(arr[i]);
            if(i % 2 == 0 && arr[i] < 0){
                System.out.println(arr[i]);
                for(int j = i+1 ; j < arr.length; j++){
                    if(arr[j] > 0){
                        swap(arr,i,j);
                        return;
                    }
                }
                System.out.println(arr[i]);
            }
            else{
                if(arr[i] > 0){
                    for(int j = i+1 ; j < arr.length; j++){
                        if(arr[j] < 0){
                            swap(arr ,i,j);
                            return;
                        }
                    }
                }
            }
        }

        for(int i=0; i< arr.length ; i++){
            System.out.println(arr[i]);
        }

    }

    static void swap(int arr[], int i, int j){
        int t ;
        t = arr[i];
        arr[j]= arr[i];
        arr[i]=t;
    }
}
