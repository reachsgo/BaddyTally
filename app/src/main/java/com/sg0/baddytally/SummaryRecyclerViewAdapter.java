package com.sg0.baddytally;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class SummaryRecyclerViewAdapter extends RecyclerView.Adapter<SummaryRecyclerViewAdapter.ViewHolder>{
    private static final String TAG = "SummaryRecyclerViewAdapter";
    private ArrayList<GameJournal> mGameJournal;

    public SummaryRecyclerViewAdapter(Context context, String group, ArrayList<GameJournal> journal) {
        Context mContext = context;
        this.mGameJournal = journal;
    }

    public void setGameJournal(ArrayList<GameJournal> gameJournal) {
        this.mGameJournal = gameJournal;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Log.d(TAG, "onCreateViewHolder");
        View v = (View) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_summary_listitem, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.journalEntry.setText(mGameJournal.get(position).toJournalEntry());
        holder.journalEntryUser.setText(mGameJournal.get(position).getmUser());
    }

    @Override
    public int getItemCount() {
        return mGameJournal.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
            TextView journalEntry;
            TextView journalEntryUser;
            ViewHolder(View itemView) {
                super(itemView);
                journalEntry = itemView.findViewById(R.id.journalEntry);
                journalEntryUser = itemView.findViewById(R.id.journalEntryUser);
            }
        }
}
