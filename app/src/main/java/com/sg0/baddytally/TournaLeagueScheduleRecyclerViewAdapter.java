package com.sg0.baddytally;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class TournaLeagueScheduleRecyclerViewAdapter extends RecyclerView.Adapter<TournaLeagueScheduleRecyclerViewAdapter.ViewHolder> {
    private static final String TAG = "TournaLeagSchedRVAdapt";
    private final Context mContext;
    private String mTourna;
    //private MatchInfo mMInfo;
    private final SharedData mCommon;
    private ArrayList<MatchInfo> msInfoList;
    private boolean reverseOrder;


    TournaLeagueScheduleRecyclerViewAdapter(Context context) {
        this.mContext = context;
        mCommon = SharedData.getInstance();
        msInfoList = null;
        reverseOrder = false;
    }

    void setMatch(final String tournament, final ArrayList<MatchInfo> matchInfos) {
        this.mTourna = tournament;
        //Log.i(TAG, "setMatch: " + mTourna);
        this.msInfoList = new ArrayList<>(matchInfos);
    }

    private boolean InvalidString (final String in) {
        if(in==null) return true;
        return in.isEmpty(); //true if empty string
    }

    class StringDateComparator implements Comparator<MatchInfo>
    {
        public int compare(MatchInfo lhs, MatchInfo rhs)
        {
            int retVal = 0;
            try {
                if (InvalidString(lhs.getDate()) && InvalidString(rhs.getDate()))
                    return 0;
                else if (InvalidString(lhs.getDate()))
                    retVal = 1;
                else if (InvalidString(rhs.getDate()))
                    retVal = -1;
                else {
                    retVal = DateFormat.getDateInstance(DateFormat.MEDIUM).parse(lhs.getDate())
                            .compareTo(DateFormat.getDateInstance(DateFormat.MEDIUM).parse(rhs.getDate()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception in StringDateComparator: " + e.getMessage());
            }
            if(reverseOrder) retVal = -retVal;
            return retVal;
        }
    }


    void sortOnDate(){
        if(msInfoList==null) return;
        reverseOrder = !reverseOrder;  //change the sort order
        Collections.sort(msInfoList, new StringDateComparator());
        //Log.d(TAG, "sortOnDate: " + msInfoList.toString());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tourna_leag_schedule_listitem, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        if(msInfoList==null || msInfoList.size()==0) return;
        //Log.d(TAG, "onBindViewHolder: pos=" + position);
        //MatchInfo key is expected to be in numerical order starting from 1.
        MatchInfo mSelectedMatch = msInfoList.get(position);
        if(mSelectedMatch==null) return;
        String key = mSelectedMatch.key;
        //Log.i(TAG, "onBindViewHolder: " + TournaUtil.getMSKeyStrFromKey(key) + " => " +
        //        mSelectedMatch.T1 + Constants.TEAM_DELIM2 + mSelectedMatch.T2);
        holder.matchId.setText(TournaUtil.getMSKeyStrFromKey(key));
        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(mCommon.getStyleString(mSelectedMatch.T1, Typeface.BOLD));
        sb.append("\n");
        sb.append(mCommon.getStyleString(Constants.TEAM_DELIM2, Typeface.ITALIC));
        sb.append("\n");
        sb.append(mCommon.getStyleString(mSelectedMatch.T2, Typeface.BOLD));
        holder.matchDesc.setText(sb);
        if(mSelectedMatch.getDone()) {
            holder.matchDate.setText(mCommon.getStrikethroughString(mSelectedMatch.getDate()));
        } else {
            holder.matchDate.setText(mSelectedMatch.getDate());
        }
        holder.parentLayout.setDividerPadding(100);  //Padding value in pixels that will be applied to each end

    }

    @Override
    public int getItemCount() {
        if(msInfoList==null) return 0;
        else return msInfoList.size();
    }

    private int getIdxToMatch(final String key){
        for (int i = 0; i < msInfoList.size(); i++) {
            //key in DB is just the integer, not with prefix like "MS".
            if(key.equals(msInfoList.get(i).key)) {
                return i;
            }
        }
        return 0; //will never reach here. key always match
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView matchId;
        final TextView matchDesc;
        final TextView matchDate;
        final Button matchScheduleButton;
        final LinearLayout parentLayout;

        ViewHolder(View itemView) {
            super(itemView);
            matchId = itemView.findViewById(R.id.match_id);
            matchDesc = itemView.findViewById(R.id.match_desc);
            matchDate = itemView.findViewById(R.id.match_date);
            matchScheduleButton = itemView.findViewById(R.id.match_schedule_button);
            parentLayout = itemView.findViewById(R.id.header_ll);


            DatePickerDialog.OnDateSetListener datePickerListener = new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                    Calendar newDate = Calendar.getInstance();
                    newDate.set(year, monthOfYear, dayOfMonth);
                    String date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(newDate.getTime());
                    final String mId = TournaUtil.getKeyFromMSKeyStr(matchId.getText().toString());
                    if(mId.isEmpty()) return;
                    //Log.d(TAG, mId + ": onDateSet: date=" + date);

                    final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                            .child(mCommon.mClub).child(Constants.TOURNA)
                            .child(mTourna).child(Constants.MATCHES).child(Constants.META)
                            .child(mId).child(Constants.INFO)
                            .child(Constants.MATCHDATE);
                    dbRef.setValue(date);

                    //since the list can be sorted on date, index is not equal to the match key.
                    int matchIdx = getIdxToMatch(mId);
                    MatchInfo mInfo = msInfoList.get(matchIdx);
                    mInfo.setDate(date);
                    //Log.i(TAG, mId + ": onDateSet: mInfo=" + mInfo.toString());
                    msInfoList.set(matchIdx, mInfo);
                    TournaLeagueScheduleRecyclerViewAdapter.this.notifyDataSetChanged();
                }
            };
            final Calendar newCalendar = Calendar.getInstance();
            final DatePickerDialog datePickerDialog = new DatePickerDialog(mContext,
                    datePickerListener,
                    newCalendar.get(Calendar.YEAR),
                    newCalendar.get(Calendar.MONTH),
                    newCalendar.get(Calendar.DAY_OF_MONTH));


            matchScheduleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Log.d(TAG, "onClick: " + matchDesc.getText());
                    if(mCommon.isAdminPlus()) {

                        final String curDate = matchDate.getText().toString();
                        if(curDate.isEmpty()) {
                            //Date for this match is not scheduled. No need to ask for confirmation
                            datePickerDialog.show();
                            return;
                        }

                        //Else, ask for user confirmation.
                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(mContext);
                        alertBuilder.setTitle("Schedule");
                        StringBuilder sb = new StringBuilder();
                        final String mId = TournaUtil.getKeyFromMSKeyStr(matchId.getText().toString());
                        if(mId.isEmpty()) return;
                        //since the list can be sorted on date, index is not equal to the match key.
                        int matchIdx = getIdxToMatch(mId);
                        MatchInfo mInfo = msInfoList.get(matchIdx);
                        if(mInfo.getDone()) {
                            sb.append("Match already PLAYED on\n");
                            sb.append(curDate);
                            sb.append(".\n\nNothing to schedule here!");
                            alertBuilder.setMessage(mCommon.getColorString(sb.toString(),Color.RED));
                        } else {
                            sb.append("Match is already scheduled for\n");
                            sb.append(curDate);
                            sb.append(".\n\n");
                            sb.append("Are you sure you want to change the date?");
                            alertBuilder.setMessage(sb.toString());
                            alertBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    datePickerDialog.show();
                                }
                            });
                        }

                        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });
                        alertBuilder.show();


                    } else {
                        Toast.makeText(mContext, "You don't have permission to do this!" ,
                                Toast.LENGTH_SHORT).show();
                    }

                } //onClick
            });
        }
    }
}
