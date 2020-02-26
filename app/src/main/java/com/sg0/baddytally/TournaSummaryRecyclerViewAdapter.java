package com.sg0.baddytally;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class TournaSummaryRecyclerViewAdapter extends RecyclerView.Adapter<TournaSummaryRecyclerViewAdapter.ViewHolder> {
    private static final String TAG = "TournaSummaryAdapter";
    private final Activity mContext;
    private String mTourna;
    private MatchInfo mMInfo;
    private final SharedData mCommon;
    private HashMap<String,ArrayList<GameJournalDBEntry>> mGameJournalMap;
    private ArrayList<String> mMatchList;


    public TournaSummaryRecyclerViewAdapter(Activity context) {
        this.mContext = context;
        mCommon = SharedData.getInstance();
        mGameJournalMap = new HashMap<>();
        mMatchList = new ArrayList<>();
    }

    void setMatch(final String tournament, final MatchInfo mInfo) {
        this.mTourna = tournament;
        this.mMInfo = mInfo;
        if(mInfo==null) fetchAllGameJournals();
        else fetchGameJournals();
    }

    private void fetchGameJournals(){
        if(mTourna.isEmpty()) return;
        if(mMInfo==null || mMInfo.T1.isEmpty()) return;

        //Log.i(TAG, "fetchGameJournals:" + mTourna + "/../data/" + mMInfo.key);
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(mCommon.mClub).child(Constants.TOURNA)
                .child(mTourna).child(Constants.MATCHES).child(Constants.DATA).child(mMInfo.key);

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Log.d(TAG, "fetchGameJournals:" + dataSnapshot.getKey());
                mGameJournalMap.clear();
                mMatchList.clear();
                for (DataSnapshot child : dataSnapshot.getChildren()) {  //M1, M2, ..
                    String matchId = child.getKey();
                    final ArrayList<GameJournalDBEntry> gameList= new ArrayList<>();
                    for (DataSnapshot gc : child.getChildren()) {  //game entries 0, 1, 2
                        GameJournalDBEntry jEntry = gc.getValue(GameJournalDBEntry.class);
                        if(jEntry==null) continue;
                        if(jEntry.getmWS()<21) continue;
                        gameList.add(jEntry);
                        //Log.d(TAG, "fetchGameJournals:" + jEntry.toReadableString());
                    }
                    if(gameList.size()>0) {
                        mGameJournalMap.put(matchId, gameList);
                        mMatchList.add(matchId);
                    }

                }
                if(mGameJournalMap.size()>0) {
                    notifyDataSetChanged();
                } else {
                    Toast.makeText(mContext, "Match yet to be played!", Toast.LENGTH_LONG).show();
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(mContext, "DB error while fetching games: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchAllGameJournals(){
        if(mTourna.isEmpty()) return;

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(mCommon.mClub).child(Constants.TOURNA)
                .child(mTourna).child(Constants.MATCHES).child(Constants.DATA);

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Log.d(TAG, "fetchGameJournals:" + dataSnapshot.getKey());
                mGameJournalMap.clear();
                mMatchList.clear();
                for (DataSnapshot msDS : dataSnapshot.getChildren()) {  //0, 1 ..
                    String matchSetId = msDS.getKey();
                    for (DataSnapshot child : msDS.getChildren()) {  //M1, M2, ..
                        String matchId = child.getKey();
                        final ArrayList<GameJournalDBEntry> gameList = new ArrayList<>();
                        for (DataSnapshot gc : child.getChildren()) {  //game entries 0, 1, 2
                            GameJournalDBEntry jEntry = gc.getValue(GameJournalDBEntry.class);
                            if (jEntry == null) continue;
                            if (jEntry.getmWS() < 21) continue;
                            gameList.add(jEntry);
                            //Log.d(TAG, "fetchGameJournals:" + jEntry.toReadableString());
                        }
                        if (gameList.size() > 0) {
                            String tmp = String.format(Locale.getDefault(),"%s-%s",matchSetId,matchId);
                            mGameJournalMap.put(tmp, gameList);
                            mMatchList.add(tmp);
                            //Log.d(TAG, "onDataChange: SGO added:" + tmp);
                            //mGameJournalMap.put(matchId, gameList);
                            //mMatchList.add(matchId);
                        } else {
                            Toast.makeText(mContext, "No matches found in DB", Toast.LENGTH_LONG).show();
                        }

                    }
                }

                if(mGameJournalMap.size()>0) {
                    notifyDataSetChanged();
                } else {
                    Toast.makeText(mContext, "Matches yet to be played!",
                            Toast.LENGTH_LONG).show();
                    mCommon.killActivity(mContext, Activity.RESULT_OK);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(mContext, "DB error while fetching games: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getPlayer1FromTeam1(final GameJournalDBEntry game1Entry) {
        String p1 = game1Entry.getmW1();
        //dont want to read all the players, if not already read. Just return winner1 as player1.
        //the game summary might be displayed with team2 printed first.
        if(mCommon.mTeamInfoMap.size()==0) {
            Log.d(TAG, "getPlayer1FromTeam1, mCommon.mTeamInfoMap.size is 0");
            return p1;
        }

        if(mMInfo==null) return p1;

        TeamInfo t1Info = mCommon.mTeamInfoMap.get(mMInfo.T1);
        if(t1Info==null) {
            Log.d(TAG, "getPlayer1FromTeam1, team not found in mTeamInfoMap:" + mMInfo.T1);
            return p1;
        }

        for(String pShort: t1Info.p_nicks) {
            if (game1Entry.playerInvolved(pShort)) {
                //Log.d(TAG, "getPlayer1FromTeam1, Player found:" + pShort + " in " + mMInfo.T1);
                return pShort;
            }
        }
        return p1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Log.d(TAG, "onCreateViewHolder");
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_summary_listitem, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        String matchId = mMatchList.get(position);
        ArrayList<GameJournalDBEntry> gameList = mGameJournalMap.get(matchId);
        GameJournalDBEntry game1Entry = gameList.get(0);
        String p1T1 = getPlayer1FromTeam1(game1Entry);
        //Log.d(TAG, "onBindViewHolder, getPlayer1FromTeam1:" + p1T1);

        StringBuilder sb = new StringBuilder();
        sb.append(matchId); sb.append(":  ");
        sb.append(game1Entry.toPlayersString(p1T1));
        for(GameJournalDBEntry jEntry: gameList) {
            sb.append(jEntry.toScoreString(p1T1)); sb.append("  ");
        }
        holder.journalEntry.setText(sb.toString());
        holder.journalEntryUser.setText(gameList.get(0).getmU());
        holder.parentLayout.setDividerPadding(100);  //Padding value in pixels that will be applied to each end
/*
        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //showAlert(view, holder);
            }
        });

        holder.parentLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (SharedData.getInstance().isRoot() || SharedData.getInstance().isAdmin())
                    showOptions(view, holder);
                return true;
            }
        });*/

    }

    @Override
    public int getItemCount() {
        return mMatchList.size();
    }

    //SGO: TODO: option to correct a bad entry. Deleting a match would mean, reverting the points.

/*
    private void showAlert(final View view, final ViewHolder holder) {
        int position = holder.getAdapterPosition();
        if (null == mGameJournalDBEntry.get(position)) return;
        GameJournalDBEntry jEntry = mGameJournalDBEntry.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(SharedData.getInstance().getTitleStr("Game stats:", mContext));
        Spanned spanString = (Spanned) TextUtils.concat("\n", SharedData.getInstance().getStyleString(jEntry.toJournalEntry(), Typeface.ITALIC),
                "\n\nDate: " + jEntry.getmDate() +
                        "\nInnings: " + jEntry.getmIn() +
                        "\nEntered by: " + jEntry.getmU() +
                        "\nPlayers repeated: " + (jEntry.getmGNo() > 1 ? "Yes" : "No"));
        builder.setMessage(spanString);
        builder.setNeutralButton("Ok", null);
        builder.show();
    }

    private void showOptions(View view, final ViewHolder holder) {
        final int position = holder.getAdapterPosition();
        final int DELETE_IDX = 1;
        final int CANCEL_IDX = 2;
        final PopupMenu popup = new PopupMenu(mContext, view);
        popup.getMenuInflater().inflate(R.menu.summary_popup_menu, popup.getMenu());
        if (Build.VERSION.SDK_INT >= 23) {
            popup.setGravity(Gravity.END);
        }
        popup.getMenu().clear();
        GameJournalDBEntry jEntry = mGameJournalDBEntry.get(position);
        Menu pMenu = popup.getMenu();
        pMenu.add("");  //add some space at the top & bottom of the game text for that to standout
        CharSequence gameDetails = SharedData.getInstance().getStyleString(jEntry.getWinnersString(true) + "  vs  " + jEntry.getLosersString(true), Typeface.ITALIC);
        pMenu.add(gameDetails);
        pMenu.add(" ");
        pMenu.addSubMenu(Menu.NONE, DELETE_IDX, Menu.NONE, "Delete"); //groupId, itemId, order, title
        pMenu.addSubMenu(Menu.NONE, CANCEL_IDX, Menu.NONE, "Cancel"); //groupId, itemId, order, title
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (CANCEL_IDX == menuItem.getItemId()) {
                    popup.dismiss();
                    return false;
                }
                Log.v(TAG, "onMenuItemClick DELETE");

                deleteGameJournal(position);

                //the current list shown is not accurate after the deletion.
                mGameJournalKeys.remove(position);
                mGameJournalDBEntry.remove(position);
                //menuItem.collapseActionView();
                popup.dismiss();
                notifyDataSetChanged();
                return true;


            }
        });
        popup.show();//showing popup menu
    }

    private void deleteGameJournal(final int position) {

        if (!SharedData.getInstance().isDBConnected()) {
            Toast.makeText(mContext, "DB connection is stale, refresh and retry...", Toast.LENGTH_SHORT).show();
            return;
        }

        GameJournalDBEntry jEntry = mGameJournalDBEntry.get(position);
        String jKey = mGameJournalKeys.get(position);
        DatabaseReference mClubDBRef = FirebaseDatabase.getInstance().getReference().child(SharedData.getInstance().mClub);
        Log.v(TAG, "Deleting key=" + jKey + " jEntry=" + jEntry.toReadableString());
        if (jEntry.getmW1().isEmpty()) {
            Toast.makeText(mContext, "winner name is empty!", Toast.LENGTH_LONG).show();
            return;
        }
        boolean singles = Constants.SINGLES.equals(jEntry.getmGT()); //game type

        //delete score from club/GROUPS/group/player
        DatabaseReference dbRef_winner1 = mClubDBRef.child(Constants.GROUPS).child(mGroup).child(jEntry.getmW1());
        dbRef_winner1.addListenerForSingleValueEvent(new UpdateScores(mContext, singles, true, dbRef_winner1, true, true));
        DatabaseReference dbRef_loser1 = mClubDBRef.child(Constants.GROUPS).child(mGroup).child(jEntry.getmL1());
        dbRef_loser1.addListenerForSingleValueEvent(new UpdateScores(mContext, singles, false, dbRef_loser1, true, false));
        if (!singles) {
            DatabaseReference dbRef_winner2 = mClubDBRef.child(Constants.GROUPS).child(mGroup).child(jEntry.getmW2());
            dbRef_winner2.addListenerForSingleValueEvent(new UpdateScores(mContext, singles, true, dbRef_winner2, true, true));
            DatabaseReference dbRef_loser2 = mClubDBRef.child(Constants.GROUPS).child(mGroup).child(jEntry.getmL2());
            dbRef_loser2.addListenerForSingleValueEvent(new UpdateScores(mContext, singles, false, dbRef_loser2, true, false));
        }

        //delete journal entry from club/JOURNAL/innings/round/group/
        mClubDBRef.child(Constants.JOURNAL).child(SharedData.getInstance().mInnings)
                .child(SharedData.getInstance().mRoundName).child(mGroup).child(jKey).setValue(null);
        Log.i(TAG, "DELETED jEntry: " + jEntry.toReadableString());
        SharedData.getInstance().setDBUpdated(true);
    }
    */

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView journalEntry;
        final TextView journalEntryUser;
        final LinearLayout parentLayout;

        ViewHolder(View itemView) {
            super(itemView);
            journalEntry = itemView.findViewById(R.id.journalEntry);
            journalEntryUser = itemView.findViewById(R.id.journalEntryUser);
            parentLayout = itemView.findViewById(R.id.journalEntry_ll);
        }
    }
}
