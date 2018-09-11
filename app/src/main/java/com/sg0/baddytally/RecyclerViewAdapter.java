package com.sg0.baddytally;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{
    private static final String TAG = "RecyclerViewAdapter";
    private ArrayList<PlayerData> mPlayers;

    public RecyclerViewAdapter(Context context, String group, ArrayList<PlayerData> players) {
        Context mContext = context;
        this.mPlayers = players;
        String mGroup = group;
    }

    public void setPlayers(ArrayList<PlayerData> players) {
        this.mPlayers = players;
    }

    public void setColor(String color) {
        String mBgColor = color;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_listitem, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.player.setText(mPlayers.get(position).getName());
        holder.innings_score.setText(mPlayers.get(position).getInnings_score());
        holder.overall_score.setText(mPlayers.get(position).getOverall_score());
        if((position % 2) == 0) {
            holder.player.setBackgroundColor(0x00000000); //transparent background
            holder.innings_score.setBackgroundColor(0x00000000);
            holder.overall_score.setBackgroundColor(0x00000000);
        } else {
            holder.player.setBackgroundColor(Color.parseColor("#ffffff"));
            holder.innings_score.setBackgroundColor(Color.parseColor("#ffffff"));
            holder.overall_score.setBackgroundColor(Color.parseColor("#ffffff"));
        }
    }

    @Override
    public int getItemCount() {
        //Log.d(TAG, "getItemCount: "+Integer.toString(mPlayers.size()));
        return mPlayers.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

            final TextView player;
            final TextView innings_score;
            final TextView overall_score;
            //LinearLayout parent_layout;
            ViewHolder(View itemView) {
                super(itemView);
                player = itemView.findViewById(R.id.player);
                innings_score = itemView.findViewById(R.id.innings_score);
                overall_score = itemView.findViewById(R.id.overall_score);
                //parent_layout = itemView.findViewById(R.id.parent_linearlayout);
            }
        }
}
