package com.sg0.baddytally;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    private static final String TAG = "RecyclerViewAdapter";
    private Context mContext;
    private ArrayList<PlayerData> mPlayers;
    private String mBgColor;
    private boolean descending;

    public RecyclerViewAdapter(Context context, String group, ArrayList<PlayerData> players) {
        mContext = context;
        this.mPlayers = players;
        String mGroup = group;
        descending = false;
    }

    public void setPlayers(ArrayList<PlayerData> players) {
        this.mPlayers = players;
        sortPlayers();
    }

    public void setBgColor(String color) {
        mBgColor = color;
    }

    public void sortPlayers() {
        SharedData.getInstance().sortPlayers(mPlayers, Constants.INNINGS_IDX, false, mContext, false);
        /*
        Collections.sort(mPlayers, new Comparator<PlayerData>() {
            @Override
            public int compare(PlayerData p1, PlayerData p2) {
                return Integer.valueOf(p2.getPointsInt_innings()).compareTo(p1.getPointsInt_innings());  //descending order
                //return Integer.valueOf(p1.getPointsInt_innings()).compareTo(p2.getPointsInt_innings()); //ascending order
            }
        }); */
        Log.d(TAG, "SGO sortPlayers: Sorted mPlayers: " + Integer.toString(mPlayers.size()));
    }

    public void sortOnSeason(){
        descending = ! descending;  //change the sort order
        SharedData.getInstance().sortPlayers(mPlayers, Constants.SEASON_IDX, descending, mContext, false);
        notifyDataSetChanged();
    }

    public void sortOnInnings(){
        descending = ! descending;  //change the sort order
        SharedData.getInstance().sortPlayers(mPlayers, Constants.INNINGS_IDX, descending, mContext, false);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_listitem, parent, false);
        return new ViewHolder(v);
    }


    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.player.setText(mPlayers.get(position).getName());
        holder.innings_score.setText(mPlayers.get(position).getPtsFormat_innings());
        holder.overall_score.setText(mPlayers.get(position).getPtsFormat_season());
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
        //Log.d(TAG, "getItemCount: "+Integer.toString(mPlayers.size()));
        return mPlayers.size();
    }

    private void showAlert(final View view, final ViewHolder holder) {
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
        TextView titleView = (TextView) dialog.findViewById(alertTitle);  //android.R.id.title is also not working
        if (titleView != null) {
            titleView.setGravity(Gravity.CENTER);   //this is not working
        }
    }

    private void showOptions(final View view, final ViewHolder holder) {
        PopupMenu popup = new PopupMenu(mContext, view);
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
                //PlayerData pData = new PlayerData(mPlayers.get(position).getGroup(), mPlayers.get(position).getName(), list);
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

                /*
                final DatabaseReference childDBRef = mClubDBRef.child(SharedData.getInstance().mInnings)
                                                              .child(mPlayers.get(position).getGroup())
                                                              .child(mPlayers.get(position).getName());
                childDBRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Integer currentScoreInDB = dataSnapshot.getValue(Integer.class);
                        if(null==currentScoreInDB) return;
                        if(currentScoreInDB.toString().equals(mPlayers.get(position).getPoints_innings())) {
                            //Current Value in DB is same as that is in the list. This makes sure that the mPlayers list is not stale!
                            childDBRef.setValue(score);
                            Toast.makeText(mContext, "Updated innings score to " + score + " for " + mPlayers.get(position).toPrintString(), Toast.LENGTH_LONG).show();
                            mPlayers.get(position).setPts_innings(score);
                            notifyDataSetChanged();
                        } else {
                            Toast.makeText(mContext, "DB not in sync with local data, Try refreshing...", Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(mContext, "DB error while fetching innings score, Try refreshing...", Toast.LENGTH_LONG).show();
                    }
                });
                break;
            case Constants.SEASON:
                //update DB attribute club/GROUPS/group/player
                final DatabaseReference childDBRef = mClubDBRef.child(Constants.GROUPS)
                        .child(mPlayers.get(position).getGroup())
                        .child(mPlayers.get(position).getName());
                childDBRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        GenericTypeIndicator<List<PointsDBEntry>> genericTypeIndicator = new GenericTypeIndicator<List<PointsDBEntry>>() {};
                        List<PointsDBEntry> list = dataSnapshot.getValue(genericTypeIndicator );
                        if(null==list || list.size() != 2) return;
                        Log.v(TAG, "FETCH: player:" + dataSnapshot.getKey());
                        //PlayerData pData = new PlayerData(mPlayers.get(position).getGroup(), mPlayers.get(position).getName(), list);
                        Log.v(TAG, "FETCH: player innings data:" + list.get(Constants.INNINGS_IDX).toString());
                        if(Integer.toString(list.get(Constants.INNINGS_IDX).getPts()).equals(mPlayers.get(position).getPoints_innings())) {
                final DatabaseReference seasonDBRef = mClubDBRef.child(Constants.GROUPS)
                        .child(mPlayers.get(position).getGroup())
                        .child(mPlayers.get(position).getName());
                seasonDBRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Integer currentScoreInDB = dataSnapshot.getValue(Integer.class);
                        if(null==currentScoreInDB) return;
                        if(currentScoreInDB.toString().equals(mPlayers.get(position).getPoints_season())) {
                            seasonDBRef.setValue(score);
                            Toast.makeText(mContext, "Updated season score to " + score + " for " + mPlayers.get(position).toPrintString(), Toast.LENGTH_LONG).show();
                            mPlayers.get(position).setPts_season(score);
                            notifyDataSetChanged();
                        } else {
                            Toast.makeText(mContext, "DB not in sync with local data, Try refreshing...", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(mContext, "DB error while fetching season core, Try refreshing...", Toast.LENGTH_LONG).show();
                    }
                });
                break;
        }*/

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
