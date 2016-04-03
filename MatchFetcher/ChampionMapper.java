import java.util.Scanner;
import java.io.File;

public class ChampionMapper {

	public static final String DEFAULT_FILE = "../Data/championIds.txt";
    
    private int[] championMap = new int[432+1]; // highest id is 432

    public ChampionMapper() { }
    public void loadMapping(String mappingFileName)
    {
    	try 
        {
            Scanner scanner = new Scanner(new File(mappingFileName));

            int i = 0;
            while (scanner.hasNextInt()) 
            {
                int currentChampionId = scanner.nextInt();
                championMap[currentChampionId] = i++;
            }
        } 
        catch(Exception e) 
        {
            e.printStackTrace();
        }
    }

    public int idToIndex(int id) 
    {
        return championMap[id];
    }


}
