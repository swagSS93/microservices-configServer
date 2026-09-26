package string;

public class PalindromUsingReverse {
    public static void main(String arg[]) {

        String s = "ABCBA";

        StringBuilder stringBuilder = new StringBuilder(s);
        stringBuilder.reverse();
        System.out.println(s.equals(stringBuilder.toString()));
    }
}
