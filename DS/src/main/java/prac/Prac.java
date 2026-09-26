import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Prac {

    public static void main (String arg[]) {
        try {
            List<String> lines = Files.readAllLines(Path.of("D:\\JAVA\\SpringBootWorkspace\\DS\\src\\main\\resources\\input.txt"));

            System.out.println(lines);
            List<Integer> integerList = new ArrayList<>();
            for ( int i=0; i< lines.size() ; i++){
                integerList.add(Integer.parseInt(lines.get(i)));
            }

          integerList.sort(Comparator.comparing(Integer::intValue));
            System.out.println(integerList);
        } catch (Exception e) {

        }
    }
}
