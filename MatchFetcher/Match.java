import java.util.Map;
import java.util.HashMap;

class Summoner
{
	Summoner(long summonerId, int championId)
	{
		this.summonerId = summonerId;
		this.championId = championId;
	}

	long getSummonerId() { return summonerId; }
	int getChampionId() { return championId; }

	private long summonerId;
	private int championId;
}



public class Match {
    public static int numChampions = 128;
    public static int blueTeamId = 100;

    private long matchId;
    private boolean blueWin;
    private Summoner[] blueTeam = new Summoner[5];
    private Summoner[] purpleTeam = new Summoner[5];



    public Match(long matchId, Summoner[] blueTeam, Summoner[] purpleTeam, boolean blueWin) {
		this.matchId = matchId;    	
        this.blueTeam = blueTeam;
        this.purpleTeam = purpleTeam;
        this.blueWin = blueWin;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        ChampionMapper cm = new ChampionMapper();
        cm.loadMapping(ChampionMapper.DEFAULT_FILE);
        int[] championArray = new int[numChampions*2];

        // Add blue team
        for (int i = 0; i < blueTeam.length; i++)
        {
        	int championIndex = cm.idToIndex(blueTeam[i].getChampionId());
        	championArray[championIndex] = 1;
        }

        // Now add purple team
        for (int i = 0; i < purpleTeam.length; i++)
        {
        	int championIndex = cm.idToIndex(purpleTeam[i].getChampionId());
        	championArray[numChampions + championIndex] = 1;
        }

        // transform array into string
        for (int i = 0; i < championArray.length; i++)
        {
        	sb.append(Integer.toString(championArray[i]) + " ");
        }
        sb.append(blueWin ? "1" : "0");
        return sb.toString();
    }

    public Summoner[] getSummoners()
    {
    	Summoner[] t = new Summoner[10];
    	for (int i = 0; i < 5; i++)
    	{
    		t[i] = blueTeam[i];
    		t[5+i] = purpleTeam[i];
    	}
    	return t;
    }
}

// 128 champions
// teamId = 100 = blue
// teamId = 200 = purple
// go from 1 5 9 11 15
// to 0 1 2 3 4 5
//
