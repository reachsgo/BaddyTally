package com.sg0.baddytally;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


public class TournaRecyclerViewAdapter extends RecyclerView.Adapter<TournaRecyclerViewAdapter.ViewHolder> implements CallbackRoutine{
    private static final String TAG = "TournaRecyclerAdapter";
    private static final String ADD_PLAYER = "Add new player";
    private static final String REMOVE_PLAYER = "Remove a player";
    private static final String DELETE_TEAM = "Delete team";
    private static final String NEWPLAYER_TITLE = "Add new player:";
    final private Context mContext;
    final public Activity mParentActivity;
    final private SharedData mCommon;
    final private View mV;
    final private String mTourna;
    final private TournaEditTextDialog mCustomDialog;
    private HashMap<String, PlayerInfo> mNewPlayer;
    private Handler mMainHandler;

    public TournaRecyclerViewAdapter(final Context context,final Activity a,
                                     final String tournament, View ll_view, final Handler handler) {
        mContext = context;
        mParentActivity = a;
        mCommon = SharedData.getInstance();
        mCommon.mTeams.clear();
        mCommon.mTeamInfoMap.clear();
        mV = ll_view;
        mTourna = tournament;
        mMainHandler = handler;

        mCustomDialog = new TournaEditTextDialog(mParentActivity, TournaRecyclerViewAdapter.this);
        mNewPlayer=null; mNewPlayer = new HashMap<>();
        Log.d(TAG, "TournaRecyclerViewAdapter:" + mTourna);
        //readDBTeam(false);
    }

    public void readDBTeam(final boolean showToast) {
        if (mTourna.isEmpty()) {
            if(showToast)
                Toast.makeText(mContext,
                    "Tournament not known!", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!mCommon.isDBConnected()) {
            if(showToast) //in init scenarios, there are other toasts shown
                Toast.makeText(mContext,
                    "DB connection is stale, refresh and retry...", Toast.LENGTH_SHORT).show();
            return;
        }
        final DatabaseReference teamScoreDBRef = FirebaseDatabase.getInstance().getReference()
                .child(mCommon.mClub).child(Constants.TOURNA)
                .child(mTourna).child(Constants.TEAMS);
        teamScoreDBRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Log.d(TAG, "onDataChange: readDBTeam");

                mMainHandler.removeCallbacksAndMessages(null);
                mCommon.stopProgressDialog(mParentActivity);
                mCommon.mTeamInfoMap.clear();
                mCommon.mTeams.clear();
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    if(childSnapshot==null) {
                        Log.d(TAG, mTourna + ":readDBTeam: null");
                        return;
                    }

                    final String team = childSnapshot.getKey();
                    //Log.i(TAG, "onDataChange Got:" + team);

                    TeamInfo tI = new TeamInfo(team);
                    mCommon.mTeams.add(team);  //still need array to get a list of teams in order

                    DataSnapshot descDS = childSnapshot.child(Constants.DESCRIPTION);
                    String desc = descDS.getValue(String.class);
                    if(desc!=null)  tI.desc = desc;
                    else tI.desc = "";

                    DataSnapshot scoreData = childSnapshot.child(Constants.SCORE);
                    TeamScoreDBEntry scoreDBEntry = scoreData.getValue(TeamScoreDBEntry.class);
                    if(scoreDBEntry!=null) tI.score = scoreDBEntry;
                    else tI.score = new TeamScoreDBEntry();
                    //Log.i(TAG, "onDataChange, scoreDBEntry:" + tI.score.toString());

                    mCommon.mTeamInfoMap.put(team, tI);
                }

                if (mCommon.mTeams.size() > 0) {
                    mCommon.sortTeams();
                    readDBPlayers();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mCommon.showToast(mContext, "DB error on read: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT);
            }
        });
    }

    private void readDBPlayers() {

        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.TOURNA)
                .child(mTourna).child(Constants.PLAYERS);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        final String player_short = childSnapshot.getKey();
                        //Log.d(TAG, "onDataChange Got:" + player_short);
                        PlayerInfo pI = childSnapshot.getValue(PlayerInfo.class);
                        TeamInfo tI = mCommon.getTeamInfo(pI.T);
                        if(tI != null) {
                            //Log.i(TAG, "onDataChange Got:" + player_short + " " + pI.toString() + " " + tI.toString());
                            tI.p_nicks.add(player_short);
                            tI.players.add(pI.name);
                        } else {
                            Log.e(TAG, "onDataChange team not found for:" + player_short + " " + pI.toString());
                        }
                    }

                if (mCommon.mTeams.size() > 0) {
                    mV.findViewById(R.id.title_tv).setVisibility(View.GONE);
                    mV.findViewById(R.id.header_ll).setVisibility(View.VISIBLE);
                    mV.findViewById(R.id.enter_button).setVisibility(View.VISIBLE);
                    notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mCommon.showToast(mContext, "DB error on read: " + databaseError.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_listitem, parent, false);
        //Log.d(TAG, "onCreateViewHolder: ");
        return new ViewHolder(v);
    }

    /*
    Below error log is seen on API19 (Android 4.4.2), though the app logs I have added stop coming after this, app seems to be functional
    09-18 10:47:33.091 12225-12225/com.sg0.baddytally D/ViewGroup: addInArray been called, this = androidx.recyclerview.widget.RecyclerView{421f9bf8 VFED.V.. .F....ID 0,123-680,519 #7f080071 app:id/gold_view}call stack =
    java.lang.Throwable: addInArray
     */

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        //Log.d(TAG, "onBindViewHolder: " + position);

        if(mCommon.mTeams.size()==0) return;
        //ArrayList<String> teamScores = mTeamScores;

        final String team = mCommon.mTeams.get(position);
        TeamInfo tI = mCommon.getTeamInfo(team);
        if(tI == null) {
            Log.d(TAG, "onBindViewHolder: teamInfo is null");
            return;
        }
        //Log.d(TAG, "onBindViewHolder: " + team);
        holder.team.setText(tI.get2LinesDesc());
        holder.points.setText(String.format(Locale.getDefault(),"%d",tI.score.getPts()));
        holder.wins.setText(String.format(Locale.getDefault(),"%d / %d",tI.score.getmW(), tI.score.getmP()));

        String mBgColor = "#c0c0c0";
        if ((position % 2) == 0) {
            holder.team.setBackgroundColor(0x00000000); //transparent background
            holder.points.setBackgroundColor(0x00000000);
            holder.wins.setBackgroundColor(0x00000000);
        } else {
            holder.team.setBackgroundColor(Color.parseColor(mBgColor));
            holder.points.setBackgroundColor(Color.parseColor(mBgColor));
            holder.wins.setBackgroundColor(Color.parseColor(mBgColor));
        }

        holder.parent_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTeamInfo(team);
            }
        });

        holder.parent_layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(!mCommon.isRoot()) return false;
                Context wrapper = new ContextThemeWrapper(mContext, R.style.RegularPopup);
                //PopupMenu popup = new PopupMenu(wrapper, view);
                final PopupMenu popup = new PopupMenu(wrapper, view);
                popup.getMenuInflater().inflate(R.menu.summary_popup_menu, popup.getMenu());
                if (Build.VERSION.SDK_INT >= 23) {
                    popup.setGravity(Gravity.END);
                }
                popup.getMenu().clear();
                Menu pMenu = popup.getMenu();
                pMenu.add(ADD_PLAYER);
                pMenu.add(REMOVE_PLAYER);
                pMenu.add(DELETE_TEAM);

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        Log.v(TAG, "onMenuItemClick:" + menuItem.getTitle().toString());
                        String choice = menuItem.getTitle().toString();
                        switch (choice) {
                            case ADD_PLAYER:
                                mCustomDialog.setContents(team, NEWPLAYER_TITLE,
                                        mTourna + "\nAdd new player to " + team,
                                        "Nick name", "  JACK  ",   //8 chars
                                        "Full name", "  Jack Daniels  ");
                                //mCustomDialog.setTitle(mTourna);
                                if (null != mCustomDialog.getWindow()) {
                                    mCustomDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                                }

                                mCustomDialog.show();
                                //Callback from TournaEditTextDialog is handled by addPlayerToTeam();
                                break;
                            case REMOVE_PLAYER:
                                removePlayerFromTeam(team);
                                break;
                            case DELETE_TEAM:
                                deleteTeam(team);
                                break;
                        }
                        popup.dismiss();
                        return true;
                    }
                });
                popup.show();//showing popup menu
                return true;
            }
        });
    }

    private void showTeamInfo(final String team) {
        TeamInfo tI = mCommon.getTeamInfo(team);
        if (tI == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(SharedData.getInstance().getTitleStr( tI.name + " team info:", mContext));
        //Spanned spanString = (Spanned) TextUtils.concat(mPlayers.get(position).getPtsDetailFormat_innings(), "\n", mPlayers.get(position).getPtsDetailFormat_season());
        StringBuilder players = new StringBuilder(" Players:\n");
        for (int i=0; i<tI.p_nicks.size(); i++) {
            players.append("       ").append(tI.getPlayerNameLong(i)).append("\n");
        }
        if(tI.p_nicks.size()==0) players.append("       None.");
        SpannableString span1 = new SpannableString(tI.desc + "\n\n");
        SpannableString span2 = new SpannableString("\n\n" + players);
        span2.setSpan(new RelativeSizeSpan(0.8f), 0, players.length()+2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        Spanned spanString = (Spanned) TextUtils.concat(span1, tI.score.toDisplayString(), span2);
        builder.setMessage(spanString)
                .setNeutralButton("Ok", null)
                .show();
    }

    private void addPlayerToTeam(final String key, final Object inobj) {
        Log.v(TAG, "addPlayerToTeam:" + key);

        if(inobj==null) {
            Log.d(TAG, "callback: null");
        } else {
            //noinspection unchecked: we know it is List of String
            ArrayList<String> strList = (ArrayList<String>)inobj;
            if(strList.size()!=3) {
                Log.e(TAG, "callback: unexpected input! " + strList.size());
                Log.e(TAG, "callback: unexpected input! " + strList.size());
                Toast.makeText(mContext, "Internal error in callback", Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            final String team = strList.get(0);
            final String pShort = strList.get(1);
            final String pLong = strList.get(2);
            Log.d(TAG, "callback: got back:" + team+ pShort + pLong);
            Boolean errVal = false;
            if(!pShort.matches("[A-Za-z0-9-_]+")) errVal = true;
            if(!pLong.matches("[A-Za-z0-9 -_]+")) errVal = true;
            if(errVal) {
                Toast.makeText(mContext, "Bad Input! Enter only alphanumeric values", Toast.LENGTH_SHORT)
                        .show();
                Log.i(TAG, "Bad Input! Enter only alphanumeric values: [" + pShort + "] [" + pLong + "]");
            } else {
                if(mCommon.checkIfPlayerAlreadyExists(mContext, pShort)) {
                    Toast.makeText(mContext, "Player nick name '" + pShort + "' is taken. Use something else.", Toast.LENGTH_SHORT)
                            .show();
                    Log.i(TAG,  "Player id '" + pShort + "' is taken. Use another short name. [" + pLong + "]");
                    return;
                }
                mNewPlayer.clear();
                SharedData.getInstance().wakeUpDBConnection();
                final PlayerInfo pInfo = new PlayerInfo();
                pInfo.T = team;
                pInfo.name = pLong;
                String msg = " You are about to create a new player with:" +
                        "\n   short name = " + pShort +
                        "\n   full name = " + pInfo.name +
                        "\n   team = " + pInfo.T ;
                mNewPlayer.put(pShort, pInfo);
                mCommon.showAlert(TournaRecyclerViewAdapter.this, mContext, ADD_PLAYER, msg);
            }

        }
    }


    private void removePlayerFromTeam(final String team) {
        Log.v(TAG, "removePlayerFromTeam:" + team);
        TeamInfo tInfo = mCommon.getTeamInfo(team);
        if(tInfo==null) return;
        Context wrapper = new ContextThemeWrapper(mContext, R.style.RedPopup);
        final PopupMenu popup = new PopupMenu(wrapper, mV.findViewById(R.id.title_tv));
        popup.getMenuInflater().inflate(R.menu.summary_popup_menu, popup.getMenu());
        if (Build.VERSION.SDK_INT >= 23) {
            popup.setGravity(Gravity.END);
        }
        popup.getMenu().clear();
        Menu pMenu = popup.getMenu();
        for (int i=0; i<tInfo.p_nicks.size(); i++) {
            pMenu.add(tInfo.getPlayerNameLong(i));
        }
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Log.v(TAG, "removePlayerFromTeam: onMenuItemClick:" + menuItem.getTitle().toString());
                popup.dismiss();
                SharedData.getInstance().wakeUpDBConnection();
                String tStr = menuItem.getTitle().toString();
                String pShort = TeamInfo.getShortName(tStr);
                if(pShort.equals(tStr)) return false;
                String msg = " Are you sure? ";
                mCommon.showAlert(TournaRecyclerViewAdapter.this, mContext, REMOVE_PLAYER + Constants.COLON_DELIM + pShort, msg);
                return true;
            }
        });
        popup.show();//showing popup menu

    }

    private void deleteTeam(final String team) {
        Log.v(TAG, "deleteTeam:" + team);
        SharedData.getInstance().wakeUpDBConnection();
        String msg = "Existing team data will be lost permanently. Are you sure? ";
        mCommon.showAlert(this, mContext, DELETE_TEAM + Constants.COLON_DELIM + team, msg);
    }

    //CallbackRoutine Callback interfaces
    public void profileFetched() { }
    public void callback(final String key, final Object inobj) {
        if(key.equals(NEWPLAYER_TITLE)) {
            //callback for ADD_PLAYER
            addPlayerToTeam(key, inobj);
        }
    }
    public void completed (final String in, final Boolean ok) {}
    public void alertResult(final String in, final Boolean ok, final Boolean ko) {
        if(in.contains(DELETE_TEAM)) {
            if(ok) {
                String[] parts = in.split(Constants.COLON_DELIM);
                if(parts.length != 2) {
                    Log.v(TAG, "alertResult Internal error:" + parts.length);
                    mCommon.showToast(mContext, "Internal error!" , Toast.LENGTH_SHORT);
                    return;
                }
                updateDB_deleteTeam(parts[1]);
            }
        } else if(in.contains(REMOVE_PLAYER)) {
            if(ok) {
                String[] parts = in.split(Constants.COLON_DELIM);
                if(parts.length != 2) {
                    Log.v(TAG, "alertResult Internal error:" + parts.length);
                    mCommon.showToast(mContext, "Internal error!" , Toast.LENGTH_SHORT);
                    return;
                }
                updateDB_removePlayer(parts[1], true);
            }
        } else if(in.equals(ADD_PLAYER)) {
            updateDB_addPlayer();
        }
    }

    private void updateDB_deleteTeam(final String short_name) {
        if(!mCommon.isDBConnected()) {
            Toast.makeText(mContext,
                    "DB connection is stale, refresh and retry...", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i(TAG, "updateDB_deleteTeam:" + short_name);
        DatabaseReference teamsDBRef = FirebaseDatabase.getInstance().getReference()
                .child(mCommon.mClub).child(Constants.TOURNA)
                .child(mCommon.mTournament).child(Constants.TEAMS);
        teamsDBRef.child(short_name).setValue(null);
        FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.TOURNA)
                .child(mCommon.mTournament).child(Constants.TEAMS_SUMMARY).child(short_name).setValue(null);

        //Delete the players of this team.
        TeamInfo tI = mCommon.getTeamInfo(short_name);
        if (tI != null) {
            for (String p: tI.p_nicks) {
                updateDB_removePlayer(p, false);
            }
        }

        mCommon.addHistory(
                FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                        .child(Constants.TOURNA).child(mCommon.mTournament),
                String.format(Locale.getDefault(),"DEL %s/%s",
                        Constants.TEAMS, short_name));

        readDBTeam(true);
    }

    private void updateDB_addPlayer() {
        if(mNewPlayer.size() != 1) {
            Log.v(TAG, "alertResult Internal error:" + mNewPlayer.size());
            mCommon.showToast(mContext, "Internal error!" , Toast.LENGTH_SHORT);
            return;
        }
        if(!mCommon.isDBConnected()) {
            Toast.makeText(mContext,
                    "DB connection is stale, refresh and retry...", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder playerString = new StringBuilder();
        for(Map.Entry<String, PlayerInfo> entry : mNewPlayer.entrySet()) {
            String p = entry.getKey();
            PlayerInfo pInfo = entry.getValue();
            Log.i(TAG, "updateDB_addPlayer:" + p + " info:" + pInfo.toString());
            DatabaseReference teamsDBRef = FirebaseDatabase.getInstance().getReference()
                    .child(mCommon.mClub).child(Constants.TOURNA)
                    .child(mCommon.mTournament).child(Constants.PLAYERS);

            teamsDBRef.child(p).setValue(pInfo);

            playerString.append(' ');
            playerString.append(p);
            readDBTeam(true);
        }

        mCommon.addHistory(
                FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                        .child(Constants.TOURNA).child(mCommon.mTournament),
                String.format(Locale.getDefault(),"ADD %s/[%s]",
                        Constants.PLAYERS, playerString));
    }

    private void updateDB_removePlayer(final String short_name, final boolean readDB) {
        if(!mCommon.isDBConnected()) {
            Toast.makeText(mContext,
                    "DB connection is stale, refresh and retry...", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i(TAG, "updateDB_removePlayer:" + short_name);
        FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.TOURNA)
                .child(mCommon.mTournament).child(Constants.PLAYERS).child(short_name).setValue(null);

        mCommon.addHistory(
                FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                        .child(Constants.TOURNA).child(mCommon.mTournament),
                String.format(Locale.getDefault(),"DEL %s/%s",
                        Constants.PLAYERS, short_name));

        if(readDB) readDBTeam(true);
    }

    @Override
    public int getItemCount() {
        //Log.d(TAG, "getItemCount: "+Integer.toString(mCommon.mTeams.size()));
        return mCommon.mTeams.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        final TextView team;
        final TextView points;
        final TextView wins;
        final LinearLayout parent_layout;

        ViewHolder(View itemView) {
            super(itemView);
            team = itemView.findViewById(R.id.player);
            points = itemView.findViewById(R.id.innings_score);
            wins = itemView.findViewById(R.id.overall_score);
            parent_layout = itemView.findViewById(R.id.parent_linearlayout);
        }
    }
}
