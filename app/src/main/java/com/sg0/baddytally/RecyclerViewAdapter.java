package com.sg0.baddytally;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    private static final String TAG = "RecyclerViewAdapter";
    private Context mContext;
    private ArrayList<PlayerData> mPlayers;
    private String mGroup;
    private String mBgColor;
    private boolean descending;
    private ArrayList<PlayerData> mFullListOfPlayers;

    public RecyclerViewAdapter(Context context, String group, ArrayList<PlayerData> players) {
        mContext = context;
        this.mPlayers = players;
        mGroup = group;
        descending = false;
    }

    public void setPlayers(final ArrayList<PlayerData> players) {
        this.mPlayers = players;
        sortPlayers();
    }

    //Support a temporary read-only (only for viewing, no editing for root also) view
    //to show all the players in a single window. Useful towards the end of the season to
    //see the complete list with points.
    //mFullListOfPlayers will hold the fill list of players only for the time period
    //that supports this temporary read-only full view.
    public void setFullListOfPlayers(final ArrayList<PlayerData> otherGroupPlayers) {
        if(null==otherGroupPlayers) {
            mFullListOfPlayers = null;
            return;
        }
        if(null==mFullListOfPlayers) {
            mFullListOfPlayers = new ArrayList<>(mPlayers);
            mFullListOfPlayers.addAll(otherGroupPlayers);
        }
    }

    public ArrayList<PlayerData> getPlayers() {
        return this.mPlayers;
    }

    public void setBgColor(String color) {
        mBgColor = color;
    }

    public void sortPlayers() {
        mFullListOfPlayers = null;
        SharedData.getInstance().sortPlayers(mPlayers, Constants.INNINGS_IDX, false, mContext, false);
    }

    public void sortOnSeason(){
        mFullListOfPlayers = null;
        descending = ! descending;  //change the sort order
        SharedData.getInstance().sortPlayers(mPlayers, Constants.SEASON_IDX, descending, mContext, false);
        notifyDataSetChanged();
    }

    public void sortOnInnings(){
        mFullListOfPlayers = null;
        descending = ! descending;  //change the sort order
        SharedData.getInstance().sortPlayers(mPlayers, Constants.INNINGS_IDX, descending, mContext, false);
        notifyDataSetChanged();
    }

    public void sortAllOnSeason(){
        if(null==mFullListOfPlayers) return;
        descending = ! descending;  //change the sort order
        SharedData.getInstance().sortPlayers(mFullListOfPlayers, Constants.SEASON_IDX, descending, mContext, false);
        notifyDataSetChanged();
        Log.i(TAG, "SGO: sortAllOnSeason....");
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

        ArrayList<PlayerData> playerList = mPlayers;
        if (null!=mFullListOfPlayers) playerList = mFullListOfPlayers;

        holder.player.setText(playerList.get(position).getName());
        holder.innings_score.setText(playerList.get(position).getPtsFormat_innings());
        holder.overall_score.setText(playerList.get(position).getPtsFormat_season());

        if ((position % 2) == 0) {
            holder.player.setBackgroundColor(0x00000000); //transparent background
            holder.innings_score.setBackgroundColor(0x00000000);
            holder.overall_score.setBackgroundColor(0x00000000);
        } else {
            holder.player.setBackgroundColor(Color.parseColor(mBgColor));
            holder.innings_score.setBackgroundColor(Color.parseColor(mBgColor));
            holder.overall_score.setBackgroundColor(Color.parseColor(mBgColor));
        }
        holder.parent_layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (SharedData.getInstance().isRoot()) showOptions(view, holder);
                return true;
            }
        });
        holder.parent_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAlert(view, holder);
            }
        });
    }

    @Override
    public int getItemCount() {
        ArrayList<PlayerData> playerList = mPlayers;
        if (null!=mFullListOfPlayers) playerList = mFullListOfPlayers;
        //Log.d(TAG, "getItemCount: "+Integer.toString(playerList.size()));
        return playerList.size();
    }

    private void showAlert(final View view, final ViewHolder holder) {
        if (null!=mFullListOfPlayers) return;  //nothing to be done if the view (supposed to be readonly transient view) is for the full set of players.
        int position = holder.getAdapterPosition();
        if (null == mPlayers.get(position)) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(SharedData.getInstance().getTitleStr(mPlayers.get(position).getName() + " stats:", mContext));
        Spanned spanString = (Spanned) TextUtils.concat(mPlayers.get(position).getPtsDetailFormat_innings(), "\n", mPlayers.get(position).getPtsDetailFormat_season());
        builder.setMessage(spanString)
                .setNeutralButton("Ok", null);
        AlertDialog dialog =  builder.show();
        // Must call show() prior to fetching text view
        TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setGravity(Gravity.CENTER);
        }

        final int alertTitle = mContext.getResources().getIdentifier("alertTitle", "id", "android");
        TextView titleView = dialog.findViewById(alertTitle);  //android.R.id.title is also not working
        if (titleView != null) {
            titleView.setGravity(Gravity.CENTER);   //this is not working
        }
    }

    private void showOptions(final View view, final ViewHolder holder) {

        //nothing to be done if the view (supposed to be readonly transient view) is for the full set of players.
        if (null!=mFullListOfPlayers) return;

        Context wrapper = new ContextThemeWrapper(mContext, R.style.RegularPopup);
        PopupMenu popup = new PopupMenu(wrapper, view);
        popup.getMenuInflater().inflate(R.menu.main_popup_menu, popup.getMenu());
        if (Build.VERSION.SDK_INT >= 23) {
            popup.setGravity(Gravity.RIGHT);
        }
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.update_innings:
                        showEditText(holder, Constants.INNINGS_IDX);
                        break;
                    case R.id.update_season:
                        showEditText(holder, Constants.SEASON_IDX);
                        break;
                    case R.id.move_to_other_pool:
                        movePlayer(holder);
                        break;

                }
                return true;
            }
        });
        popup.show();//showing popup menu
    }

    private void showEditText(final ViewHolder holder, final Integer key) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        int position = holder.getAdapterPosition();
        if (null == mPlayers.get(position)) return;
        String period = "";
        // Set an EditText view to get user input
        final EditText input = new EditText(mContext);
        switch (key) {
            case Constants.INNINGS_IDX:
                input.setText(mPlayers.get(position).getPoints_innings());
                period = "Innings";
                break;
            case Constants.SEASON_IDX:
                input.setText(mPlayers.get(position).getPoints_season());
                period = "Season";
                break;
        }
        builder.setTitle(SharedData.getInstance().getTitleStr("Enter new " + period + " score for " + mPlayers.get(position).getName() + ":", mContext));
        input.setSelection(input.getText().length());  //move cursor to end
        builder.setView(input);
        builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Integer score;
                try {
                    score = Integer.parseInt(input.getText().toString());
                } catch (NumberFormatException nfe) {
                    Toast.makeText(mContext, "Invalid entry!", Toast.LENGTH_SHORT).show();
                    return;
                }
                //final int position = holder.getAdapterPosition();
                //Toast.makeText(mContext, "Update " + key + ":" + score + " " + mPlayers.get(position).toString(), Toast.LENGTH_LONG).show();
                updatedDB(holder, key, score);
            }
        });
        builder.show();
    }

    private void updatedDB(final ViewHolder holder, final int key, final Integer score) {

        if(!SharedData.getInstance().isDBConnected()) {
            Toast.makeText(mContext, "DB connection is stale, refresh and retry...", Toast.LENGTH_SHORT).show();
            return;
        }

        final int position = holder.getAdapterPosition();
        try {
            //just being too conservative!! not sure if the below code will be ever hit.
            if (mPlayers.get(position) == null) {
                Toast.makeText(mContext, "Something went wrong! data not in sync!", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(mContext, "Something went wrong! data not in sync!", Toast.LENGTH_LONG).show();
            return;
        }
        Log.i(TAG, "updatedDB for [" + mPlayers.get(position).toPrintString() + "] " + key + "=" + score);
        DatabaseReference mClubDBRef = FirebaseDatabase.getInstance().getReference().child(SharedData.getInstance().mClub);

        //update DB attribute club/innings/group/player
        final DatabaseReference childDBRef = mClubDBRef.child(Constants.GROUPS)
                .child(mPlayers.get(position).getGroup())
                .child(mPlayers.get(position).getName());
        childDBRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                GenericTypeIndicator<List<PointsDBEntry>> genericTypeIndicator = new GenericTypeIndicator<List<PointsDBEntry>>() {
                };
                List<PointsDBEntry> list = dataSnapshot.getValue(genericTypeIndicator);
                if (null == list || list.size() != 2) return;
                Log.v(TAG, "FETCH: key=" + key + " player:" + dataSnapshot.getKey());
                //PlayerData pData = new PlayerData(mPlayers.get(position).getGroup(), mPlayers.get(position).getDesc(), list);
                Log.v(TAG, "FETCH: player data[" + key + "] : " + list.get(key).toString());
                if (Integer.toString(list.get(key).getPts()).equals(mPlayers.get(position).getPoints(key))) {
                    //Current Value in DB is same as that is in the list. This makes sure that the mPlayers list is not stale!
                    list.get(key).setPts(score);
                    childDBRef.setValue(list);
                    Toast.makeText(mContext, "Updated innings score to " + score + " for " + mPlayers.get(position).toPrintString(), Toast.LENGTH_LONG).show();
                    mPlayers.get(position).setPoints(key, score);
                    sortPlayers();
                    notifyDataSetChanged();
                    Log.v(TAG, "updatedDB: player data[" + key + "] : " + mPlayers.get(position).toPrintString());
                } else {
                    Toast.makeText(mContext, "DB not in sync with local data, Try refreshing...", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(mContext, "DB error while fetching innings score, Try refreshing...", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void movePlayer(final ViewHolder holder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final int position = holder.getAdapterPosition();
        if (null == mPlayers.get(position)) return;
        String targetGroup = Constants.GOLD;
        if (mGroup.equals(Constants.GOLD)) targetGroup = Constants.SILVER;
        builder.setTitle(SharedData.getInstance().getTitleStr("Move " + mPlayers.get(position).getName() + " to " + targetGroup + "?", mContext));
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(mContext, "Moving " + mPlayers.get(position).getName(), Toast.LENGTH_SHORT).show();
                moveUser(mPlayers.get(position));
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.show();
    }

    private void moveUser(final PlayerData p) {
        final SharedData mCommon = SharedData.getInstance();
        if (!mCommon.isDBConnected()) {
            mCommon.showToast(mContext, "DB connection is stale, refresh and retry...", Toast.LENGTH_SHORT);
            return;
        }
        String tg = Constants.GOLD;
        if (mGroup.equals(Constants.GOLD)) tg = Constants.SILVER;
        final String targetGroup = tg;

        final DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        final String club = mCommon.mClub;

        DatabaseReference dbRef = mDatabase.child(club).child(Constants.GROUPS).child(targetGroup).child(p.getName());
        final List<PointsDBEntry> points = new ArrayList<>();
        points.add(Constants.SEASON_IDX, new PointsDBEntry(p.getPointsDBEntry_season()));
        points.add(Constants.INNINGS_IDX, new PointsDBEntry(p.getPointsDBEntry_innings()));

        dbRef.setValue(points, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if (databaseError != null) {
                    mCommon.showToast(mContext, "New user data (GROUPS/group) could not be saved:" + databaseError.getMessage(),
                            Toast.LENGTH_LONG);
                } else {
                    //new user added to other group, not delete from this group.
                    mDatabase.child(club).child(Constants.GROUPS).child(mGroup).child(p.getName()).removeValue();
                    mCommon.showToast(mContext, "User " + p.getName() + " moved to " + targetGroup + ". Refresh your view.",
                            Toast.LENGTH_LONG);
                    mCommon.setDBUpdated(true);
                    mCommon.addUserMove2History(targetGroup + "/" + p.getName());
                }
            }
        });
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        final TextView player;
        final TextView innings_score;
        final TextView overall_score;
        final LinearLayout parent_layout;

        ViewHolder(View itemView) {
            super(itemView);
            player = itemView.findViewById(R.id.player);
            innings_score = itemView.findViewById(R.id.innings_score);
            overall_score = itemView.findViewById(R.id.overall_score);
            parent_layout = itemView.findViewById(R.id.parent_linearlayout);
        }
    }
}
