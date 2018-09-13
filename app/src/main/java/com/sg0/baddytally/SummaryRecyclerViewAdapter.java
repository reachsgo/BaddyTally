package com.sg0.baddytally;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class SummaryRecyclerViewAdapter extends RecyclerView.Adapter<SummaryRecyclerViewAdapter.ViewHolder> {
    private static final String TAG = "SummaryRecyclerAdapter";
    private final ArrayList<String> mGameJournalKeys;
    private final Context mContext;
    private final String mGroup;
    private ArrayList<GameJournalDBEntry> mGameJournalDBEntry;

    public SummaryRecyclerViewAdapter(Context context, String group, ArrayList<String> keys, ArrayList<GameJournalDBEntry> journal) {
        this.mContext = context;
        this.mGroup = group;
        this.mGameJournalKeys = keys;
        this.mGameJournalDBEntry = journal;
    }

    public void setGameJournal(ArrayList<GameJournalDBEntry> gameJournalDBEntry) {
        this.mGameJournalDBEntry = gameJournalDBEntry;
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
        holder.journalEntry.setText(mGameJournalDBEntry.get(position).toJournalEntry());
        holder.journalEntryUser.setText(mGameJournalDBEntry.get(position).getmU());
        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SharedData.getInstance().isRoot()) showOptions(view, holder);
            }
        });

    }

    @Override
    public int getItemCount() {
        return mGameJournalDBEntry.size();
    }

    private void showOptions(View view, final ViewHolder holder) {
        final int position = holder.getAdapterPosition();
        final String[] options = {"delete"};
        PopupMenu popup = new PopupMenu(mContext, view);
        popup.getMenuInflater().inflate(R.menu.summary_popup_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                //menuItem.getTitle() => "Delete?", There is only one option now
                //Toast.makeText(mContext, "Deleting: " + mGameJournalKeys.get(position) + " => " + mGameJournalDBEntry.get(position).toReadableString(), Toast.LENGTH_LONG).show();
                deleteGameJournal(position);

                //the current list shown is not accurate after the deletion.
                mGameJournalKeys.remove(position);
                mGameJournalDBEntry.remove(position);
                notifyDataSetChanged();
                /* //Another option is to restart the activity, but then DB is read again. Above is more efficient.
                Activity activity = (Activity)mContext;
                activity.finish();
                activity.startActivity(activity.getIntent()); */
                return true;
            }
        });
        popup.show();//showing popup menu
    }

    private void deleteGameJournal(final int position) {
        GameJournalDBEntry jEntry = mGameJournalDBEntry.get(position);
        String jKey = mGameJournalKeys.get(position);
        DatabaseReference mClubDBRef = FirebaseDatabase.getInstance().getReference().child(SharedData.getInstance().mClub);
        Log.v(TAG, "Deleting key=" + jKey + " jEntry=" + jEntry.toReadableString());
        if (jEntry.getmW1().isEmpty()) {
            Toast.makeText(mContext, "winner name is empty!", Toast.LENGTH_LONG).show();
            return;
        }
        boolean singles = Constants.SINGLES.equals(jEntry.getmGT()); //game type

        //delete score from club/innings/group/player
        //DatabaseReference dbRef_winner1 = mClubDBRef.child(SharedData.getInstance().mInnings).child(mGroup).child(jEntry.getmW1());
        //dbRef_winner1.addListenerForSingleValueEvent(new UpdateScores(mContext, singles, jEntry.getmW1(), dbRef_winner1, true,true));
        //delete score from club/GROUPS/group/player
        DatabaseReference dbRef_winner1 = mClubDBRef.child(Constants.GROUPS).child(mGroup).child(jEntry.getmW1());
        dbRef_winner1.addListenerForSingleValueEvent(new UpdateScores(mContext, singles, true, dbRef_winner1, true, true));
        DatabaseReference dbRef_loser1 = mClubDBRef.child(Constants.GROUPS).child(mGroup).child(jEntry.getmL1());
        dbRef_loser1.addListenerForSingleValueEvent(new UpdateScores(mContext, singles, false, dbRef_loser1, true, true));
        if (!singles) {
            DatabaseReference dbRef_winner2 = mClubDBRef.child(Constants.GROUPS).child(mGroup).child(jEntry.getmW2());
            dbRef_winner2.addListenerForSingleValueEvent(new UpdateScores(mContext, singles, true, dbRef_winner2, true, true));
            DatabaseReference dbRef_loser2 = mClubDBRef.child(Constants.GROUPS).child(mGroup).child(jEntry.getmL1());
            dbRef_loser2.addListenerForSingleValueEvent(new UpdateScores(mContext, singles, false, dbRef_loser2, true, true));
            /*
            //delete score from club/innings/group/player
            DatabaseReference dbRef_winner2 = mClubDBRef.child(SharedData.getInstance().mInnings).child(mGroup).child(jEntry.getmW2());
            dbRef_winner2.addListenerForSingleValueEvent(new UpdateScores(mContext, singles, jEntry.getmW2(), dbRef_winner2, true, true));
            //delete score from club/GROUPS/group/player
            dbRef_winner2 = mClubDBRef.child(Constants.GROUPS).child(mGroup).child(jEntry.getmW2());
            dbRef_winner2.addListenerForSingleValueEvent(new UpdateScores(mContext, singles, jEntry.getmW2(), dbRef_winner2, true, false));
            */
        }

        //delete journal entry from club/JOURNAL/innings/round/group/
        mClubDBRef.child(Constants.JOURNAL).child(SharedData.getInstance().mInnings)
                .child(SharedData.getInstance().mRoundName).child(mGroup).child(jKey).setValue(null);
        Log.i(TAG, "DELETED jEntry: " + jEntry.toReadableString());
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
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
