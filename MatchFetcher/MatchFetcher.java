import org.json.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.URI;
import java.net.URLConnection;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class MatchFetcher {

    public static final String API_KEY = "56746db4-bd66-4d35-a24d-b9d817688fb5";
    public static final String ROOT = "https://na.api.pvp.net";
    public static final long SEED_SUMMONER = 390600;

    // Requesting from MATCHLIST
    public static final String REQUEST_MATCHLIST = "/api/lol/na/v2.2/matchlist/by-summoner/";
    public static final String QUEUE_TYPE = "RANKED_SOLO_5x5";
    public static final String SEASON = "PRESEASON2016";

    // Requesting from MATCH
    public static final String REQUEST_MATCH = "/api/lol/na/v2.2/match/";

    // Request settings
    public static final int REQUEST_LIMIT_SHORT = 10;
    public static final int REQUEST_LIMIT = 500;

    // Serialization for hashmap locations
    public static final String MATCH_SER_LOCATION = "matchDuplicateFound.ser";
    public static final String SUMMONER_SER_LOCATION = "summonerDuplicateFound.ser";

    private int numRequests;
    private final int numMatchesToGet;
    private int currentNumMatches;
    private final int numLayers;
    private final String saveLocation;
    private ArrayList<Match> matchList = new ArrayList<Match>();
    private Map<Long, Boolean> checkedMatch;
    private Map<Long, Boolean> checkedSummoner;

    /** INTERFACE ---------------------------------------------------------------------------**/
    public MatchFetcher(final int numMatchesToGet, final String saveLocation)
    {
    	this.numRequests = 0;
    	this.numMatchesToGet = numMatchesToGet;
    	matchList.ensureCapacity(numMatchesToGet); // now, it's pretty much a simple array with access to the top index to append

    	this.saveLocation = saveLocation;

    	// Find the number of layers to go through
    	int maxPossible = 10;
    	int layerCount = 1;
    	if (maxPossible > numMatchesToGet)
    	{
    		layerCount = 1;
    	}
    	while (maxPossible <= numMatchesToGet)
    	{
    		maxPossible *= 100;
    		layerCount++;
    	}

    	// add an extra layer to account for duplicate matches
    	this.numLayers = ++layerCount;

    	// Load hashmaps(deserialize)
    	loadDuplicateHashMaps();
    }
    public void getMatches()
    {
    	search(numLayers, SEED_SUMMONER);
    	saveMatchesToTextFile();
    }
    public void saveMatchesToTextFile()
    {
    	if (matchList.isEmpty())
    	{
    		// TODO: Throw an error, there are no matches to save
            System.out.println("No matches to save");
    		return;
    	}

    	// Save matches
    	System.out.println("Saving matches...");
    	try (PrintWriter out = new PrintWriter(saveLocation))
    	{
    		for (int i = 0; i < matchList.size(); i++)
    		{
    			out.println(matchList.get(i).toString());
    		}
    	}
    	catch (Exception e)
    	{
    		System.out.println("Error writing to save file");
    		System.out.println(e.getMessage());
    	}
        // Serialize hashmaps
        saveDuplicateHashMaps();

    }
    /** IMPLEMENTATION ---------------------------------------------------------------------------**/
    private void search(int currentLayer, long parentSummonerId)
    {
    	// Go as deep as numLayers says, since we want to try and cover as much of the tree as possible, not just go down one branch continuously
    	if (currentLayer == 0)
    		return;
    	if (checkedSummoner.containsKey(parentSummonerId))
    		return;

    	// Get the recent matches of a summoner
    	JSONArray parentRecentMatchIdWrappers = getSummonerRecentMatchIdWrappers(parentSummonerId);

    	for (int i = 0; i < parentRecentMatchIdWrappers.length(); i++)
    	{
    		// Get the matchId
    		JSONObject jsonMatchIdWrapper = (JSONObject) parentRecentMatchIdWrappers.get(i);
    		long matchId = jsonMatchIdWrapper.getLong("matchId");

    		// Check if it exists
    		if (!checkedMatch.containsKey(matchId))
    		{
    			// Add match
	    		Match match = getMatch(matchId);
    			matchList.add(match);
    			checkedMatch.put(matchId, true);
    			++currentNumMatches;

    			System.out.println("Added a match (" + currentNumMatches + "/" + numMatchesToGet + ")");	

    			if (currentNumMatches == numMatchesToGet)
    			{
    				break;
    			}

    			Summoner[] matchSummonerList = match.getSummoners();
    			for (int j = 0; j < matchSummonerList.length; j++)
    			{
    				search(currentLayer-1, matchSummonerList[j].getSummonerId());
    				if (currentNumMatches == numMatchesToGet) { break; }
    			}
    		}
    		else
    		{
    			System.out.println("Match already exists");
    		}
    		if (currentNumMatches == numMatchesToGet) { break; }
    	}

        // I haven't fully checked this summoner until I've gone through all of its children
        checkedSummoner.put(parentSummonerId, true);
    }
    private JSONObject getJSONMatch(long matchId)
    {
        URLConnection conn = null;
    	try
    	{
	    	URI uri = new URI(ROOT + REQUEST_MATCH + Long.toString(matchId) 
	    				+ "?api_key=" + API_KEY);
            conn = uri.toURL().openConnection();
            JSONTokener tokener = new JSONTokener(conn.getInputStream());
	    	incrementHandleRequest();
	    	return new JSONObject(tokener);
    	}	 
    	catch (Exception e)
    	{
            int numSecondsToRetry = conn.getHeaderFieldInt("Retry-After", 0);
            System.out.println("Request rate limit reached, retrying in " + numSecondsToRetry + " seconds");
            try 
            {
                Thread.sleep(numSecondsToRetry * 1000);
            }
            catch (InterruptedException ie)
            {
                
            }
    		return getJSONMatch(matchId);
    	}
    }
    private Match getMatch(long matchId) // calls getJSONMatch, and then calls parseJSONMatch
    {
    	JSONObject jsonMatch = getJSONMatch(matchId);
    	return parseJSONMatch(jsonMatch);
    }
    private Match parseJSONMatch(JSONObject jsonMatch)
    {
    	// get matchId
    	long matchId = jsonMatch.getLong("matchId");

    	// get win
    	JSONArray teamArray = jsonMatch.getJSONArray("teams");
    	JSONObject t = (JSONObject) teamArray.get(0);
    	boolean blueTeamWin = (t.getInt("teamId") == Match.blueTeamId) ?
    						  (t.getBoolean("winner")) : 
    						  !(t.getBoolean("winner"));

    	// get players
    	JSONArray allSummoners = jsonMatch.getJSONArray("participantIdentities");
    	Summoner[] blueTeamSummoners = new Summoner[5];
    	Summoner[] purpleTeamSummoners = new Summoner[5];
    	int blueCount = 0;
    	int purpleCount = 0;
    	for (int i = 0; i < allSummoners.length(); i++)
    	{
    		// Get summonerId
    		JSONObject summoner = (JSONObject) allSummoners.get(i);
    		long summonerId = summoner.getJSONObject("player")
    								  .getLong("summonerId");
    		
    		// Get associated championId and teamId
    		int championId = 0;
    		int teamId = 0;
    		long participantId = summoner.getInt("participantId");
    		JSONArray allParticipants = jsonMatch.getJSONArray("participants");
    		for (int j = 0; j < allParticipants.length(); j++)
    		{
    			if (participantId == ((JSONObject)allParticipants.get(j)).getInt("participantId"))
    			{
    				championId = ((JSONObject)allParticipants.get(j)).getInt("championId");
    				teamId = ((JSONObject)allParticipants.get(j)).getInt("teamId");
    				break;
    			}
    		}
    		boolean blueTeam = teamId == Match.blueTeamId;

    		if (blueTeam)
    		{
    			blueTeamSummoners[blueCount++] = new Summoner(summonerId, championId);
    		}
    		else
    		{
    			purpleTeamSummoners[purpleCount++] = new Summoner(summonerId, championId);
    		}
    	}

    	return new Match(matchId, blueTeamSummoners, purpleTeamSummoners, blueTeamWin);
    }
    private JSONArray getSummonerRecentMatchIdWrappers(long summonerId)
    {
    	// get root
        URLConnection conn = null;
    	try
    	{
	    	URI uri = new URI(ROOT + REQUEST_MATCHLIST + Long.toString(summonerId) 
	    				+ "?rankedQueues=" + QUEUE_TYPE 
	    				+ "&seasons=" + SEASON
	    				+ "&api_key=" + API_KEY);
            conn = uri.toURL().openConnection();
	    	JSONTokener tokener = new JSONTokener(conn.getInputStream());
	    	JSONObject root = new JSONObject(tokener);
	    	incrementHandleRequest();
	    	return root.getJSONArray("matches");

    	}
    	catch (Exception e)
    	{
            int numSecondsToRetry = conn.getHeaderFieldInt("Retry-After", 0);
    		System.out.println("Request rate limit reached, retrying in " + numSecondsToRetry + " seconds");
    		try 
    		{
    			Thread.sleep(numSecondsToRetry * 1000);
    		}
    		catch (InterruptedException ie)
    		{
    			
    		}
    		return getSummonerRecentMatchIdWrappers(summonerId);
    	}
    }

    private void incrementHandleRequest()
    {
		numRequests++;
		if (numRequests % REQUEST_LIMIT == 0)
		{
			// Sleep for 10 minutes
			try 
			{
				System.out.println("Waiting 10 minutes due to request rate...");
				Thread.sleep(60 * 10 * 1000 + 500);
			}
			catch (InterruptedException e)
			{

			}
		}
        else if (numRequests % REQUEST_LIMIT_SHORT == 0)
        {
            // Sleep for 10 seconds
            try 
            {
                System.out.println("Waiting 10 seconds due to request rate...");
                Thread.sleep(10 * 1000 + 500); // extra 0.5 seconds for padding
            }
            catch (InterruptedException e)
            {

            }
        }
    }


    void saveDuplicateHashMaps()
    {
    	try
    	{
    		FileOutputStream fos = new FileOutputStream(MATCH_SER_LOCATION);
    		ObjectOutputStream oos = new ObjectOutputStream(fos);
    		oos.writeObject(checkedMatch);
    		oos.close();
    		fos.close();
    		System.out.println("Saved match duplicate checker");

    		fos = new FileOutputStream(SUMMONER_SER_LOCATION);
    		oos = new ObjectOutputStream(fos);
    		oos.writeObject(checkedSummoner);
    		oos.close();
    		fos.close();
    		System.out.println("Saved summoner duplicate checker");
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}
    }

    void loadDuplicateHashMaps()
    {
    	checkedMatch = new HashMap<Long, Boolean>();
    	checkedSummoner = new HashMap<Long, Boolean>();

    	try
    	{
    		FileInputStream fis = new FileInputStream(MATCH_SER_LOCATION);
    		ObjectInputStream ois = new ObjectInputStream(fis);
    		checkedMatch = (HashMap) ois.readObject();
    		ois.close();
    		fis.close();

    		fis = new FileInputStream(SUMMONER_SER_LOCATION);
    		ois = new ObjectInputStream(fis);
    		checkedSummoner = (HashMap) ois.readObject();
    		ois.close();
    		fis.close();


    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    		return;
    	}
    }





    public static void main(String[] args)
	{
		if (args.length != 2)
		{
			System.out.println("Incorrect number of arguments");
			return;
		}
		MatchFetcher mf = new MatchFetcher(Integer.parseInt(args[0]), args[1]);
		mf.getMatches();
	}
}

/*
Idea:
Start with a seed summoner, get his recent matches(ranked, diamond+)
for everybody in that match, get their recent matches.
I will do this recursively, and I will have a global count and a layer parameter
in the recursive call
layers = ceil(global count / (10.0 * 10.0)
10 = match
10 = players in a match

layer 1
10 matches
10 * 9 players

layer 2
10 * 9*10 matches
10 * 9*10 * 9 players

layer 3
900 * 9 matches
 players

10 + 900 + 9000
start = 10;
second = 900;

from then on, *100



When I discover matches, I dont want duplicates, so I should have a HashMap
that maps matchIds to booleans that indicate whether they were already chosen or
not


search(int layers, int parentSummoner)
{
	if layer == 0
		return;

	get all ranked matches of parent summoner
	but i also want the team members of a match(the match will contain this data)
	1) I could either get the json match and get all of the necessary details
	2) Just get the match and let the match contain all the data
	Let's take route 2(and when we save the data, we'll only take what we need)

	class ArrayList that contains all of my matches, then I can ask for it with 
	getMatches

	I will append the match, and then recursively call the function on the summoner
	within the match, with layers-1
}



*/
