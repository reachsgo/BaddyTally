package com.sg0.baddytally;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.text.SpannableStringBuilder;
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

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        holder.journalEntryUser.setText(SharedData.truncate(
                mGameJournalDBEntry.get(position).getmU(), false,0));
                //len=0, so truncate to TINYNAMELENGTH

        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAlert(view, holder);
            }
        });

        holder.parentLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (SharedData.getInstance().isAdminPlus())
                    showOptions(view, holder);
                return true;
            }
        });

    }

    @Override
    public int getItemCount() {
        return mGameJournalDBEntry.size();
    }

    private void showAlert(final View view, final ViewHolder holder) {
        int position = holder.getAdapterPosition();
        if (null == mGameJournalDBEntry.get(position)) return;
        GameJournalDBEntry jEntry = mGameJournalDBEntry.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(SharedData.getInstance().getTitleStr("Game stats:", mContext));

        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append("\n");
        sb.append(SharedData.getInstance().getStyleString(jEntry.toJournalEntry(), Typeface.ITALIC));
        sb.append("\n\nDate: "); sb.append(jEntry.getmDate());
        sb.append("\nInnings: "); sb.append(jEntry.getmIn());
        sb.append("\nEntered by: "); sb.append(jEntry.getmU());
        sb.append("\nPlayers repeated: "); sb.append((jEntry.getmGNo() > 1 ? "Yes" : "No"));
        builder.setMessage(sb);
        builder.setNeutralButton("Ok", null);
        builder.show();
    }

    private void showOptions(View view, final ViewHolder holder) {
        final int position = holder.getAdapterPosition();
        final int DELETE_IDX = 1;
        final int CANCEL_IDX = 2;
        Context wrapper = new ContextThemeWrapper(mContext, R.style.RedPopup);
        final PopupMenu popup = new PopupMenu(wrapper, view);
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

                /* //Another option is to restart the activity, but then DB is read again. Above is more efficient.
                Activity activity = (Activity)mContext;
                activity.finish();
                activity.startActivity(activity.getIntent()); */

            }
        });
        popup.show();//showing popup menu
    }

    private void deleteGameJournal(final int position) {

        if (!SharedData.getInstance().isDBConnected()) {
            Toast.makeText(mContext,
                    "DB connection is stale, refresh and retry...", Toast.LENGTH_SHORT).show();
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
