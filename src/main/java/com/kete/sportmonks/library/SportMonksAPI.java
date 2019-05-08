package com.kete.sportmonks.library;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.kete.sportmonks.library.model.league.League;
import com.kete.sportmonks.library.model.league.LeagueResponse;
import com.kete.sportmonks.library.model.match.Match;
import com.kete.sportmonks.library.model.match.MatchData;
import com.kete.sportmonks.library.model.match.MatchDetail;
import com.kete.sportmonks.library.model.match.MatchsResponse;
import com.kete.sportmonks.library.model.odds.OddType;
import com.kete.sportmonks.library.model.odds.OddsResponse;
import com.kete.sportmonks.library.model.player.PlayerData;
import com.kete.sportmonks.library.model.season.SeasonData;
import com.kete.sportmonks.library.model.season.SeasonDataList;
import com.kete.sportmonks.library.model.season.SeasonDataResponse;
import com.kete.sportmonks.library.model.stage.Stage;
import com.kete.sportmonks.library.model.stage.StagesData;
import com.kete.sportmonks.library.model.standings.StandingTeam;
import com.kete.sportmonks.library.model.standings.StandingsData;
import com.kete.sportmonks.library.model.standings.StandingsDataInfo;
import com.kete.sportmonks.library.model.team.Team;
import com.kete.sportmonks.library.model.team.TeamDetail;
import com.kete.sportmonks.library.model.team.TeamsResponse;
import com.kete.sportmonks.library.model.topscorers.TopScores;
import com.kete.sportmonks.library.model.topscorers.TopScoresPlayer;
import com.kete.sportmonks.library.model.venue.Venue;
import com.kete.sportmonks.library.model.venue.VenueDetail;
import com.kete.sportmonks.library.net.GetResponse;
import com.kete.sportmonks.library.net.HttpFunctions;
import com.kete.sportmonks.library.util.Constants;
import com.kete.sportmonks.library.util.PlayerDataAdapter;
import com.kete.sportmonks.library.util.SportMonksException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.kete.sportmonks.library.util.Constants.baseURL;

/**
 * SocceramaAPI Main Class
 * 
 * Singleton class
 *
 */
public class SportMonksAPI 
{
	private static SportMonksAPI instance;
	private static final Logger logger = LogManager.getLogger(SportMonksAPI.class);
	private String apiKey = null;
	private String headerPending = null;
	private String headerTotal = null;
	
	private SportMonksAPI(String api)
	{
		apiKey = api;
	}

	public static SportMonksAPI getInstance(String api) {
		if (instance == null) {
			instance = new SportMonksAPI(api);
			return instance;
		}
		else return instance;
	}

	public String getRemainingRequests() {
		return headerPending;
	}

	public String getMaximumRequests() {
		return headerTotal;
	}

    /**
     * Return includes in string format
     *
     * @param includes Includes
     * @return Includes in string format to be attached on URL
     */
	private String getIncludes(String... includes) {
        if (includes != null && includes.length > 0) {
            return "&include=" + String.join(",", includes);
        }
        else {
			return "";
		}
    }

	/**
	 * Update headers (pending requests) obtained in response
	 *
	 * @param response Response
	 */
	private void updateHeaders(GetResponse response) {
		headerPending = response.getHeaderPending();
		headerTotal = response.getHeaderTotal();
	}

	// REQUESTS
	/**
	 * Get list of matches by date range
	 * 
	 * @param beginDate Begin date
	 * @param endDate End date
	 * @return List of matches
	 * @throws IOException
	 * @throws SportMonksException 
	 */
	public List<Match> getMatchesByDateRange(String beginDate, String endDate) throws IOException, SportMonksException
	{
		String[] includes = {};
		return getMatchesByDateRange(beginDate, endDate, includes);
	}

	/**
	 * Get list of matches by date range
	 *
	 * @param beginDate Begin date
	 * @param endDate End date
	 * @param includes Includes
	 * @return List of matches
	 * @throws IOException
	 * @throws SportMonksException
	 */
	public List<Match> getMatchesByDateRange(String beginDate, String endDate, String... includes) throws IOException, SportMonksException
	{
		String url = baseURL + "fixtures/between/" + beginDate + "/" + endDate + "?api_token=" + apiKey + getIncludes(includes);
		GetResponse response = HttpFunctions.get(url);
		updateHeaders(response);
		if (response.getResponseCode() == Constants.RESPONSE_OK) {
			Gson gson = new Gson();
			MatchsResponse matchsResponse = gson.fromJson(response.getResponse(), MatchsResponse.class);
			List<Match> matchsList = matchsResponse.getListOfMatches();
			int page = 1; int totalPages = matchsResponse.getMetadata().getPagination().getTotalPages();
			while (page < totalPages) {
				page++;
				String urlAux = url + "&page=" + page;
				response = HttpFunctions.get(urlAux);
				updateHeaders(response);
				if (response.getResponseCode() == Constants.RESPONSE_OK) {
					gson = new Gson();
					matchsResponse = gson.fromJson(response.getResponse(), MatchsResponse.class);
					List<Match> matchsListAux = matchsResponse.getListOfMatches();
					for (int i=0; i<matchsListAux.size(); i++)
						matchsList.add(matchsListAux.get(i));
				}
				else throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());
			}
			return matchsList;
		}
		else throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());
	}

	/**
	 * Get match detail
	 * 
	 * @param matchId Match ID
	 * @return Match Detail
	 * @throws IOException
	 * @throws SportMonksException 
	 */
	public MatchDetail getMatchDetail(String matchId) throws IOException, SportMonksException
	{
	    String[] includes = {"localTeam", "visitorTeam", "events.player", "lineup.player", "stats"};
        return getMatchDetail(matchId, includes);
	}

    /**
     * Get match detail
     *
     * @param matchId Match ID
	 * @param includes Includes
     * @return Match Detail
     * @throws IOException
     * @throws SportMonksException
     */
    public MatchDetail getMatchDetail(String matchId, String... includes) throws IOException, SportMonksException
    {
        String url = baseURL + "fixtures/" + matchId + "?api_token=" + apiKey + getIncludes(includes);
        GetResponse response = HttpFunctions.get(url);
		updateHeaders(response);
        if (response.getResponseCode() == Constants.RESPONSE_OK) {
            Type myOtherClassListType = new TypeToken<PlayerData>() {}.getType();
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(myOtherClassListType, new PlayerDataAdapter())
                    .create();
            MatchData matchData = gson.fromJson(response.getResponse(), MatchData.class);
            return matchData.getMatchDetail();
        }
        else throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());
    }

	/**
	 * Get season data
	 * 
	 * @param seasonID Season ID
	 * @return Season Data (Results, stages, league)
	 * @throws IOException
	 */
	public SeasonData getSeasonData(String seasonID) throws IOException, SportMonksException
	{
		String[] includes = {"stages.fixtures.localTeam", "stages.fixtures.visitorTeam", "league", "results", "groups.fixtures", "groups.standings", "groups.fixtures.localTeam", "groups.fixtures.visitorTeam"};
		return getSeasonData(seasonID, includes);
	}

	/**
	 * Get season data
	 *
	 * @param seasonID Season ID
	 * @param includes Includes
	 * @return Season Data (Results, stages, league)
	 * @throws IOException
	 */
	public SeasonData getSeasonData(String seasonID, String... includes) throws IOException, SportMonksException
	{
		String url = baseURL + "seasons/" + seasonID + "?api_token=" + apiKey + getIncludes(includes);
		GetResponse response = HttpFunctions.get(url);
		updateHeaders(response);
		if (response.getResponseCode() == Constants.RESPONSE_OK) {
			Gson gson = new Gson();
			SeasonDataResponse seasonResults = gson.fromJson(response.getResponse(), SeasonDataResponse.class);
			return seasonResults.getData();
		}
		else throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());
	}
	
	/**
	 * Get a list with different odds (pre-match)
	 * 
	 * @param matchID Match ID
	 * @return List with pre-match odds
	 * @throws IOException
	 */
	public List<OddType> getMatchOdds(String matchID) throws IOException, SportMonksException {
		String url = baseURL + "odds/fixture/" + matchID + "?api_token=" + apiKey;
		GetResponse response = HttpFunctions.get(url);
		updateHeaders(response);
		if (response.getResponseCode() == Constants.RESPONSE_OK) {
			Gson gson = new Gson();
			OddsResponse oddsResponse = gson.fromJson(response.getResponse(), OddsResponse.class);
		    return oddsResponse.getOddTypes();
		}
		else throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());	
	}

	/**
	 * Get a list with different odds (pre-match)
	 *
	 * @param matchID      Match ID
	 * @param bookMakerId BookMaker ID
	 * @return List with pre-match odds
	 * @throws IOException
	 */
	public List<OddType> getMatchOdds(String matchID, String bookMakerId) throws IOException, SportMonksException {
		String url = baseURL + "odds/fixture/" + matchID + "/bookmaker/" + bookMakerId + "?api_token=" + apiKey;
		GetResponse response = HttpFunctions.get(url);
		updateHeaders(response);
		if (response.getResponseCode() == Constants.RESPONSE_OK) {
			Gson gson = new Gson();
			OddsResponse oddsResponse = gson.fromJson(response.getResponse(), OddsResponse.class);
			return oddsResponse.getOddTypes();
		} else
			throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());
	}

	/**
	 * Get a list with different odds (pre-match)
	 *
	 * @param matchID Match ID
	 * @param market  BookMaker ID
	 * @return List with pre-match odds
	 * @throws IOException
	 */
	public List<OddType> getMatchOddsByMarket(String matchID, String market) throws IOException, SportMonksException {
		String url = baseURL + "odds/fixture/" + matchID + "/market/" + market + "?api_token=" + apiKey;
		GetResponse response = HttpFunctions.get(url);
		updateHeaders(response);
		if (response.getResponseCode() == Constants.RESPONSE_OK) {
			Gson gson = new Gson();
			OddsResponse oddsResponse = gson.fromJson(response.getResponse(), OddsResponse.class);
			return oddsResponse.getOddTypes();
		} else
			throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());
	}

	/**
	 * Get Topscores list
	 *
	 * @param seasonId Season ID
	 * @return List of topscores
	 * @throws IOException
	 */
	public List<TopScoresPlayer> getTopScores(String seasonId) throws IOException, SportMonksException
	{
		String[] includes = {"goalscorers.player", "goalscorers.team"};
		return getTopScores(seasonId, includes);
	}

	/**
	 * Get Topscores list
	 * 
	 * @param seasonId Season ID
	 * @param includes Includes
	 * @return List of topscores
	 * @throws IOException
	 */
	public List<TopScoresPlayer> getTopScores(String seasonId, String... includes) throws IOException, SportMonksException
	{
		String url = baseURL + "topscorers/season/" + seasonId + "?api_token=" + apiKey + getIncludes(includes);
		GetResponse response = HttpFunctions.get(url);
		updateHeaders(response);
		if (response.getResponseCode() == Constants.RESPONSE_OK) {
			Gson gson = new Gson();
		    TopScores topScoresResponse = gson.fromJson(response.getResponse(), TopScores.class);
		    return topScoresResponse.getListOfTopScores();
		}
		else throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());		
	}

	/**
	 * Get standings list
	 * 
	 * @param seasonId Season ID
	 * @return List of standing teams
	 * @throws IOException
	 */
	public List<StandingTeam> getStandings(String seasonId) throws IOException, SportMonksException
	{
		String url = baseURL + "standings/season/" + seasonId + "?api_token=" + apiKey;
		GetResponse response = HttpFunctions.get(url);
		updateHeaders(response);
		if (response.getResponseCode() == Constants.RESPONSE_OK) {
			Gson gson = new Gson();
		    StandingsData standingsResponse = gson.fromJson(response.getResponse(), StandingsData.class);
		    if (standingsResponse.getStandings() != null)
		    	return standingsResponse.getStandings().getStandingsList();
		    else
		    	return new ArrayList<>();
		}
		else throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());			
	}

	/**
	 * Get standings list
	 *
	 * @param seasonId Season ID
	 * @return List of standing teams
	 * @throws IOException
	 */
	public List<StandingsDataInfo> getCupStandings(String seasonId) throws IOException, SportMonksException {
		String url = baseURL + "standings/season/" + seasonId + "?api_token=" + apiKey;
		GetResponse response = HttpFunctions.get(url);
		updateHeaders(response);
		if (response.getResponseCode() == Constants.RESPONSE_OK) {
			Gson gson = new Gson();
			StandingsData standingsResponse = gson.fromJson(response.getResponse(), StandingsData.class);
			if (standingsResponse.getStandings() != null)
				return standingsResponse.getStandingsDataInfo();
			else
				return new ArrayList<>();
		} else
			throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());
	}

	/**
	 * Get list of today's matches
	 *
	 * @param includes include parameters
	 * @return List of matches
	 * @throws IOException
	 * @throws SportMonksException
	 */
	public List<Match> getTodayMatches(String... includes) throws IOException, SportMonksException {
		String url = baseURL + "livescores" + "?api_token=" + apiKey + getIncludes(includes);
		GetResponse response = HttpFunctions.get(url);
		updateHeaders(response);
		if (response.getResponseCode() == Constants.RESPONSE_OK) {
			Gson gson = new Gson();
			MatchsResponse matchsResponse = gson.fromJson(response.getResponse(), MatchsResponse.class);
			List<Match> matchsList = matchsResponse.getListOfMatches();
			int page = 1;
			int totalPages = matchsResponse.getMetadata().getPagination().getTotalPages();
			while (page < totalPages) {
				page++;
				String urlAux = url + "&page=" + page;
				response = HttpFunctions.get(urlAux);
				updateHeaders(response);
				if (response.getResponseCode() == Constants.RESPONSE_OK) {
					gson = new Gson();
					matchsResponse = gson.fromJson(response.getResponse(), MatchsResponse.class);
					List<Match> matchsListAux = matchsResponse.getListOfMatches();
					for (int i = 0; i < matchsListAux.size(); i++)
						matchsList.add(matchsListAux.get(i));
				} else
					throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());
			}
			return matchsList;
		} else
			throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());
	}

	/**
	 * Get list of today's live matches
	 *
	 * @param includes include parameters
	 * @return List of matches
	 * @throws IOException
	 * @throws SportMonksException
	 */
	public List<Match> getLiveMatches(String... includes) throws IOException, SportMonksException {
		String url = baseURL + "livescores/now" + "?api_token=" + apiKey + getIncludes(includes);
		GetResponse response = HttpFunctions.get(url);
		updateHeaders(response);
		if (response.getResponseCode() == Constants.RESPONSE_OK) {
			Gson gson = new Gson();
			MatchsResponse matchsResponse = gson.fromJson(response.getResponse(), MatchsResponse.class);
			return matchsResponse.getListOfMatches();
		} else
			throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());
	}

	/**
	 * Get list of  matches for a particular date
	 *
	 * @param includes include parameters
	 * @return List of matches
	 * @throws IOException
	 * @throws SportMonksException
	 */
	public List<Match> getMatches(String date, String... includes) throws IOException, SportMonksException {
		String url = baseURL + "fixtures/date/" + date + "?api_token=" + apiKey + getIncludes(includes);
		GetResponse response = HttpFunctions.get(url);
		updateHeaders(response);
		if (response.getResponseCode() == Constants.RESPONSE_OK) {
			Gson gson = new Gson();
			MatchsResponse matchsResponse = gson.fromJson(response.getResponse(), MatchsResponse.class);
			List<Match> matchsList = matchsResponse.getListOfMatches();
			int page = 1;
			int totalPages = matchsResponse.getMetadata().getPagination().getTotalPages();
			while (page < totalPages) {
				page++;
				String urlAux = url + "&page=" + page;
				response = HttpFunctions.get(urlAux);
				updateHeaders(response);
				if (response.getResponseCode() == Constants.RESPONSE_OK) {
					gson = new Gson();
					matchsResponse = gson.fromJson(response.getResponse(), MatchsResponse.class);
					List<Match> matchsListAux = matchsResponse.getListOfMatches();
					for (int i = 0; i < matchsListAux.size(); i++)
						matchsList.add(matchsListAux.get(i));
				} else
					throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());
			}
			return matchsList;
		} else
			throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());
	}

	/**
	 * Get venue detail
	 *
	 * @param seasonId Season ID
	 * @return Venue detail
	 * @throws IOException
	 * @throws SportMonksException
	 */
	public List<VenueDetail> getVenues(String seasonId) throws IOException, SportMonksException {
		String url = baseURL + "venues/season/" + seasonId + "?api_token=" + apiKey;
		GetResponse response = HttpFunctions.get(url);
		updateHeaders(response);
		if (response.getResponseCode() == Constants.RESPONSE_OK) {
			Gson gson = new Gson();
			Venue venue = gson.fromJson(response.getResponse(), Venue.class);
			if (venue != null && venue.getData() != null)
				return venue.getData();
			else
				return new ArrayList<>();
		} else
			throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());
	}

	/**
	 * Get list of teams
	 *
	 * @param seasonId Season ID
	 * @param includes Includes
	 * @return List of teams
	 * @throws IOException
	 * @throws SportMonksException
	 */
	public List<TeamDetail> getTeams(String seasonId, String... includes) throws IOException, SportMonksException {
		String url = baseURL + "teams/season/" + seasonId + "?api_token=" + apiKey;
		if (includes != null && includes.length > 0) {
			url += "&include=" + String.join(",", includes);
		}
		GetResponse response = HttpFunctions.get(url);
		updateHeaders(response);
		if (response.getResponseCode() == Constants.RESPONSE_OK) {
			Gson gson = new Gson();
			TeamsResponse teamsResponse = gson.fromJson(response.getResponse(), TeamsResponse.class);
			if (teamsResponse != null && teamsResponse.getListOfTeams() != null)
				return teamsResponse.getListOfTeams();
			else
				return new ArrayList<>();
		} else
			throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());
	}

	/**
	 * Get team
	 *
	 * @param teamId Team ID
	 * @param includes Includes
	 * @return Team info
	 * @throws IOException
	 * @throws SportMonksException
	 */
	public TeamDetail getTeam(String teamId, String... includes) throws IOException, SportMonksException {
		String url = baseURL + "teams/" + teamId + "?api_token=" + apiKey + getIncludes(includes);
		GetResponse response = HttpFunctions.get(url);
		updateHeaders(response);
		if (response.getResponseCode() == Constants.RESPONSE_OK) {
			Gson gson = new Gson();
			Team team = gson.fromJson(response.getResponse(), Team.class);
			if (team != null && team.getTeamDetail() != null)
				return team.getTeamDetail();
			else
				return new TeamDetail();
		} else
			throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());
	}

	/**
	 * Get list of seasons
	 * @return List of seasons
	 * @throws IOException
	 * @throws SportMonksException
	 */
	public List<SeasonData> getSeasons() throws IOException, SportMonksException {
		String url = baseURL + "seasons" + "?api_token=" + apiKey;
		GetResponse response = HttpFunctions.get(url);
		updateHeaders(response);
		if (response.getResponseCode() == Constants.RESPONSE_OK) {
			Gson gson = new Gson();
			SeasonDataList seasonResponse = gson.fromJson(response.getResponse(), SeasonDataList.class);
			List<SeasonData> seasonDataList = seasonResponse.getListOfSeasons();
			int page = 1;
			int totalPages = seasonResponse.getMetadata().getPagination().getTotalPages();
			while (page < totalPages) {
				page++;
				String urlAux = url + "&page=" + page;
				response = HttpFunctions.get(urlAux);
				updateHeaders(response);
				if (response.getResponseCode() == Constants.RESPONSE_OK) {
					gson = new Gson();
					seasonResponse = gson.fromJson(response.getResponse(), SeasonDataList.class);
					List<SeasonData> seasonsListAux = seasonResponse.getListOfSeasons();
					for (int i = 0; i < seasonsListAux.size(); i++)
						seasonDataList.add(seasonsListAux.get(i));
				} else
					throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());
			}
			return seasonDataList;
		} else
			throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());

	}

	/**
	 * Get list of leagues
	 * @return List of leagues
	 * @throws IOException
	 * @throws SportMonksException
	 */
	public List<League> getLeagues(String... includes) throws IOException, SportMonksException {
		String url = baseURL + "leagues" + "?api_token=" + apiKey + getIncludes(includes);
		GetResponse response = HttpFunctions.get(url);
		updateHeaders(response);
		if (response.getResponseCode() == Constants.RESPONSE_OK) {
			Gson gson = new Gson();
			LeagueResponse leagueResponse = gson.fromJson(response.getResponse(), LeagueResponse.class);
			if (leagueResponse != null && leagueResponse.getListOfLeagues() != null)
				return leagueResponse.getListOfLeagues();
			else
				return new ArrayList<>();
		} else
			throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());
	}

	/**
	 * Return stages list
	 * @param seasonId Season ID
	 * @param includes Includes
	 * @return Stages list
	 * @throws IOException
	 * @throws SportMonksException
	 */
	public List<Stage> getStages(String seasonId, String... includes) throws IOException, SportMonksException {
		String url = baseURL + "stages/season/" + seasonId + "?api_token=" + apiKey + getIncludes(includes);
		GetResponse response = HttpFunctions.get(url);
		updateHeaders(response);
		if (response.getResponseCode() == Constants.RESPONSE_OK) {
			Gson gson = new Gson();
			StagesData stagesData = gson.fromJson(response.getResponse(), StagesData.class);
			if (stagesData != null && stagesData.getListOfStages() != null)
				return stagesData.getListOfStages();
			else
				return new ArrayList<>();
		} else
			throw new SportMonksException(response.getResponseCode() + " - " + response.getResponse());
	}
}
