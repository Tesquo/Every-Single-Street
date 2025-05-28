import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Dataset {
    public ArrayList<Object> dataSet = new ArrayList<>();
    String dataset = "Datasets/Waterlooville.osm";

    public void addDataset() throws FileNotFoundException {
        try {
            BufferedReader input = new BufferedReader(new FileReader(dataset));
            String line;
            while ((line = input.readLine()) != null) {
                if (line.contains("addr:street")) {
                    String[] data = line.split(",");
                    dataSet.add(data);
                    System.out.println(Arrays.toString(data));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        Dataset dataset1 = new Dataset();
        dataset1.addDataset();
    }
}


