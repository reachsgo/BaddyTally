package com.sg0.baddytally;

class Constants {
    public static final String APPNAME = "ScoreTally";
    static final String APPSHORT = "ST";
    static final String GROUPS = "groups";
    static final String INNINGS = "innings";
    static final String INTERNALS = "tmp";
    static final String LOCK = "locked";
    static final String ACTIVE_USERS = "users";
    static final String HISTORY = "story";
    static final String SEASON = "season";
    static final String PROFILE = "profile";
    static final String NEWS = "news";
    static final String JOURNAL = "journal";
    static final String ROUND = "round";
    static final String NEWCLUBS = "newC";
    static final String CLUBDETAILS = "detC";
    static final String ACTIVECLUBS = "actC";
    static final String STCLUB = "C1ub";
    static final String SINGLES = "singles";
    static final String DOUBLES = "doubles";
    static final String GOLD = "gold";
    static final String SILVER = "silver";
    static final String ADMIN = "admin";
    static final String MEMBER = "member";
    static final String SUPERUSER = "super";
    static final String ROOT = "root";
    static final String ROOTCLUB = "sgo";
    static final String DEMO_CLUB = "demo";
    static final String NEWROUND = "start_new_round";
    static final String USERDATA = "Userdata";
    static final String USERDATA_ID = "UserdataID";
    static final String USERID_TMP = "ST-";
    static final String USERDATA_LASTCLUB = "tmpClubdata";
    static final String DATA_CLUB = "club";
    static final String DATA_USER = "user";
    static final String DATA_USER2 = "uid2";
    static final String DATA_SEC = "secpd";
    static final String DATA_ROLE = "role";
    static final String DATA_PHNUMS = "phnums";
    static final String DATA_TMODE = "tmode";
    static final String DATA_LOCKED = "locked";
    static final String DATA_OFFLINE_MODE = "offlineM";
    static final String DATA_FLAGS = "stFlags";
    static final String DATA_FLAG_NAV_TELIM = "N_TEL";  //Navigation help for Tourna Elimination
    static final String DATA_FLAG_NAV_TRACK = "N_STR";  //Navigation help for Score Tracker
    static final String DATA_FLAG_DEMO_MODE1 = "N_DEMO1";  //demo mode alert
    static final String DATA_FLAG_DEMO_MODE2 = "N_DEMO2";  //demo mode alert
    static final String DATA_FLAG_DEMO_MODE3 = "N_DEMO3";  //demo mode alert
    static final String DATA_FLAG_DEMO_MODE4 = "N_DEMO4";  //demo mode alert
    static final String DATA_FLAG_DEMO_MODE5 = "N_DEMO5";  //demo mode alert
    static final String DATA_FLAG_DEMO_MODE6 = "N_DEMO6";  //demo mode alert
    static final String DELETE = "delete";
    static final String CREATE = "create";
    static final String ACCESS = "access";
    static final int DATA_LOCKED_COUNT_MAX = 9;
    static final int REFRESH_TIMEOUT = 15000; //15s
    static final Integer NUM_OF_GROUPS = 2;
    static final int SEASON_IDX = 0;
    static final int INNINGS_IDX = 1;
    static final String ROUND_DATEFORMAT = "yyyy-MM-dd'T'HH:mm";
    static final String ROUND_DATEFORMAT_SHORT = "yyyy-MM-dd";
    static final int SHUFFLE_WINPERC_NUM_GAMES = 12;
    static final int SETTINGS_ACTIVITY = 100;
    static final int LOGIN_ACTIVITY = 101;
    static final int SUMMARY_ACTIVITY = 102;
    static final int ENTERDATA_ACTIVITY = 103;
    static final int TRACKSCORES_ACTIVITY = 104;
    static final int RESTARTAPP = 665;
    static final int EXIT_APPLICATION = -666;
    static final int TINYNAMELENGTH = 8;
    static final int MAX_NUM_TEAMS = 64;
    static final int DB_READ_TIMEOUT = 5000;  //5s
    static final int SHOWTOAST_TIMEOUT = 3000;  //5s
    static final int MAXNUM_CLUBS_PER_USER = 1;
    static final int MAX_NUM_PLAYERS_DEFAULT = 16;
    static final int TOO_MANY_ENTRIES = 500;

    static final String ACTIVITY = "Activity";
    static final String ACTIVITY_SETTINGS = "ClubLeagueSettings";
    static final String ACTIVITY_CLUB_ENTERDATA = "ClubLeagueEnterData";
    static final String ACTIVITY_TOURNA_SETTINGS = "TournaSettings";
    static final String INITIAL = "Initial";
    static final String TOURNA = "tournaments";
    static final String DESCRIPTION = "desc";
    static final String ACTIVE = "active";
    static final String TEAMS_SUMMARY = "teams_sum";
    static final String TEAMS = "teams";
    static final String TEAM_DELIM1 = "> ";
    static final String TEAM_DELIM2 = " vs ";
    static final String SEED = "seed";
    static final String PLAYERS = "players";
    static final String COLON_DELIM = ": ";
    static final String NAME = "name";
    static final String SCORE = "score";
    static final String MATCHES = "matches";
    static final String META = "meta";
    static final String DATA = "data";
    static final String INFO = "info";
    static final String NUM_OF_MATCHES = "mNum";
    static final String NUM_OF_GAMES = "bestOf";
    static final String COMPLETED = "done";
    static final String MATCHDATE = "date";
    static final String MATCHID_PREFIX = "M";
    static final String MATCHSETID_PREFIX = "MS";
    static final String CB_READTOURNA = "fetchActiveTournaments";
    static final String CB_SHOWTOURNA = "showTournaments";
    static final String CB_READMATCHMETA = "readDBMatchMeta";
    static final String CB_SHOWMATCHES = "showMatches";
    static final String CB_NOMATCHFOUND = "No match found!";
    static final String FIXTURE_UPPER = "fixU";
    static final String FIXTURE_LOWER = "fixL";
    static final String DE_FINALS = "F-1";
    static final String DE_FINALS_M1 = "1";
    static final String DE_FINALS_M2 = "2";
    static final String DE_EXTLINK_INDICATOR = "*";
    static final String DE_EXTLINK_INDICATOR_DISPLAY = "[UB]";
    static final String DE_EXTLINK_INDICATOR_DISPLAY_LONG = " [from UB]";
    static final String WINNER = "w";
    static final String TEAM1PLAYERS = "Team1Players";
    static final String TEAM2PLAYERS = "Team2Players";
    static final String TOURNATYPE = "TournaType";
    static final String EXTRAS = "Extras";
    static final String VIEWONLY = "ViewOnly";
    static final String MATCH = "Match";
    static final String FIXTURE = "Fixture";
    static final String TYPE = "type";
    static final String CLUBLEAGUE = "ClubLeague";
    static final String LEAGUE = "League";
    static final String SE = "SE";  //Single Elimination
    static final String DE = "DE";  //Double Elimination
    static final String SE_LONG = "SE: Single Elimination";  //Single Elimination
    static final String DE_LONG = "DE: Double Elimination";  //Double Elimination
    static final String BYE = "(bye)";
    static final String BYE_DISPLAY = "bye";

    static final String SCORETRACKDATA = "ScoreTracker";
    static final String DATA_T1 = "ST_T1";
    static final String DATA_T2 = "ST_T2";
    static final String DATA_T1P1 = "ST_T1P1";
    static final String DATA_T1P2 = "ST_T1P2";
    static final String DATA_T2P1 = "ST_T2P1";
    static final String DATA_T2P2 = "ST_T2P2";
    static final String DATA_G1 = "ST_G1";
    static final String DATA_G2 = "ST_G2";
    static final String DATA_G3 = "ST_G3";
    static final String DATA_LEFTTEAM = "ST_LEFT";
    static final String DATA_SRVCTEAM = "ST_SRVC";

    static final String SUBSC_FREE = "Free";
    static final String SUBSC_PAID = "Paid";

    static final String  CHANNEL_NEWCLUB = "ST_NEWCLUB";
    static final String  CHANNEL_NEWCLUB_NAME = "New Club";  //name visible to user
    static final String  INTENT_DATASTR1 = "INTENT_DATA_str_1";
    static final String  INTENT_DATASTR2 = "INTENT_DATA_str_2";
    static final String  INTENT_DATASTR3 = "INTENT_DATA_str_3";

}
