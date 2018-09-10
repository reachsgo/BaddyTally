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
    //private ArrayList<String> mPlayers = new ArrayList<>();
    //private ArrayList<String> mInnings_scores = new ArrayList<>();
    //private ArrayList<String> mOverall_scores = new ArrayList<>();

    //public RecyclerViewAdapter(Context mContext, ArrayList<String> players, ArrayList<String> innings_scores, ArrayList<String> overall_scores) {
    public RecyclerViewAdapter(Context context, String group, ArrayList<PlayerData> players) {
        Context mContext = context;
        this.mPlayers = players;
        String mGroup = group;
        //this.mInnings_scores = innings_scores;
        //this.mOverall_scores = overall_scores;
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
        //Log.d(TAG, "onCreateViewHolder");
        View v = (View) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_listitem, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //Log.d(TAG, "onBindViewHolder:"+Integer.toString(position));

        //holder.innings_score.setText(mInnings_scores.get(position));
        //holder.overall_score.setText(mOverall_scores.get(position));
        /*if(position == mPlayers.size()-1) {
            //holder.player.setText(Html.fromHtml("<b>"+mPlayers.get(position)+"</b>", Html.FROM_HTML_MODE_COMPACT));
            holder.player.setTypeface(null, Typeface.BOLD);
            holder.innings_score.setTypeface(null, Typeface.BOLD);
            holder.overall_score.setTypeface(null, Typeface.BOLD);
            //holder.player.setText("Player");
            //holder.innings_score.setText("This\nRound");
            //holder.overall_score.setText("Overall");
        }*/
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

            TextView player;
            TextView innings_score;
            TextView overall_score;
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
