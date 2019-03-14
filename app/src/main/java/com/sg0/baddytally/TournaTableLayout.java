package com.sg0.baddytally;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.print.PrintAttributes;
import android.print.pdf.PrintedPdfDocument;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.PopupMenu;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class Coordinates {
    public static final String ROUNDSTR = "R";
    public static final String ROUNDSTR_FULL = "Round";
    private static final String[] LABELS = {"Final", "SF", "QF"};
    private static final String[] LABELS_LOWER = {"Final"};
    //private static final String[] LABELS = {"Final"};
    private static final String[] LABELS_FULL = {"Final", "SemiFinal", "QuarterFinal"};
    public Integer round;
    public Integer matchId;
    public Integer X;
    public Integer Y;

    public Coordinates(Integer round, Integer matchId) {
        this.round = round;
        this.matchId = matchId;
        this.X = -1;
        this.Y = -1;
    }

    public static Coordinates getCoordinates(final String key) {
        String idx[] = key.split("-");
        if (idx.length != 2) return null;
        Integer round = Integer.valueOf(idx[0]);
        Integer matchId = Integer.valueOf(idx[1]);
        //Log.e("Tourna", "getCoordinates ("+key+") " + round + "," + matchId);
        return new Coordinates(round, matchId);
    }

    public static String getIDName(final String key, final Integer maxRoundNum, final Boolean noLabels) {
        Coordinates roundData = getCoordinates(key);
        if (roundData == null) return "ERR";
        String[] labels = LABELS;
        if (noLabels) labels = LABELS_LOWER;
        Integer reversedRoundNum = maxRoundNum - roundData.round;
        //Finals is at index 0.
        if (reversedRoundNum == 0) {
            return labels[reversedRoundNum];  //Finals
        }
        if (reversedRoundNum < labels.length) {
            return labels[reversedRoundNum] + "-" + roundData.matchId;
        }
        return ROUNDSTR + roundData.round + "-" + roundData.matchId;
    }

    public static String getIDNameLong(final String key, final Integer maxRoundNum) {
        Coordinates roundData = getCoordinates(key);
        if (roundData == null) return "ERR";
        Integer reversedRoundNum = maxRoundNum - roundData.round;
        //Finals is at index 0.
        if (reversedRoundNum == 0) {
            return LABELS_FULL[reversedRoundNum];  //Finals
        }
        if (reversedRoundNum < LABELS_FULL.length) {
            return LABELS_FULL[reversedRoundNum] + "-" + roundData.matchId;
        }
        return ROUNDSTR_FULL + roundData.round + "-" + roundData.matchId;
    }

    public Integer getRound() {
        return round;
    }

    public Integer getMatchId() {
        return matchId;
    }

    public Integer getY() {
        return Y;   //round num starts from 1
    }

    public void setY(Integer y) {
        Y = y;
    }

    public Integer getX() {
        return X;   //match id starts from 1
    }

    public void setX(Integer x) {
        X = x;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        Coordinates o = (Coordinates) obj;
        return X == o.X && Y == o.Y;
    }

    @Override
    public String toString() {
        return "(" + getX() + "," + getY() + ") r=" + getRound() + " m=" + getMatchId();
    }
}

class TournaDispMatchEntry extends TournaFixtureDBEntry {

    public String id;
    public Coordinates xy;
    private Boolean vertLineCell;
    private Boolean vertLineCentre;
    // x = Match num or row of display matrix
    // y = round or column of display matrix

    public TournaDispMatchEntry() {
        super();
        this.id = "";
        xy = new Coordinates(-1, -1);
        vertLineCell = false;
        vertLineCentre = false;
    }

    public TournaDispMatchEntry(TournaFixtureDBEntry o, String id) {
        super(o);
        this.id = id;
        xy = Coordinates.getCoordinates(id);
        vertLineCell = false;
        vertLineCentre = false;
    }

    public String getId() {
        return id;
    }

    public Boolean getVertLineCell() {
        return vertLineCell;
    }

    public void setVertLineCell() {
        this.vertLineCell = true;
    }

    public Boolean getVertLineCentre() {
        return vertLineCentre;
    }

    public void setVertLineCentre() {
        this.vertLineCentre = true;
    }

    public void setCoordinates(final Integer x, final Integer y) {
        xy.setX(x);
        xy.setY(y);
    }

    @Override
    public String toString() {
        if (vertLineCell) return "LINE";
        if (vertLineCentre) return "LINECENTRE";
        return xy.toString() + ">" + id + ":" + super.toString();
    }
}


// ============ INTERNAL CLASS ============


class TournaTable implements View.OnClickListener {
    private static final String TAG = "TournaTable";
    private static final String MATCH_INFO = "Match Info";
    private static final String ENTER_SCORE = "Enter Score";
    private static final String ENTER_WINNER = "Override Winner";
    private static final String VIEW_SCORE = "View Score";
    private static final String BYE = "(bye)";
    public TournaTable mExternal;
    private Activity mActivity;
    //public TableLayout mTable;
    private int mTableResId;
    private String mFixtureLabel;
    private String mTourna;
    private Boolean mNoLabels;
    private HashMap<String, TournaDispMatchEntry> mFixture;
    private Integer mMaxRounds;  //Round1 to RoundX (Final)
    private SparseArray<ArrayList<TournaDispMatchEntry>> mData;
    private ArrayList<TeamDBEntry> mTeams;
    private SharedData mCommon;
    private String mTournaType;
    private CustomDialogClass matchInfoDialog;
    private HandlerThread mWorker;
    private Handler mWorkerHandler;
    private Handler mMainHandler;
    private Boolean drawingInProgress;


    TournaTable(final Activity activity, final int table_resId,
                final String fixLabel, final String tourna, final String type,
                final ArrayList<TeamDBEntry> teams, final Boolean noLabels) {
        //mTable = table;
        mTableResId = table_resId;
        mActivity = activity;
        mFixtureLabel = fixLabel;
        mTourna = tourna;
        mCommon = SharedData.getInstance();
        mFixture = new HashMap<>();
        mData = new SparseArray<>();
        mTeams = teams;
        mMaxRounds = 0;
        mExternal = null;
        mNoLabels = noLabels;
        mTournaType = type;
        matchInfoDialog = null;
        drawingInProgress = false;


        Log.d(TAG, "Creating TournaTable..." + fixLabel);

        mWorker = new HandlerThread("Worker");
        mWorker.start();
        Looper looper = mWorker.getLooper();
        mWorkerHandler = new Handler(looper);
        mMainHandler = new Handler();
    }

    public String getFixtureLabel() {
        return mFixtureLabel;
    }

    public TournaTable getExternalTable() {
        return mExternal;
    }

    public void setExternalTable(final TournaTable mExternal) {
        this.mExternal = mExternal;
    }

    public void zoom(final Boolean zoomIn) {
        if (drawingInProgress) return;


    }

    void readDB() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                .child(Constants.TOURNA).child(mTourna).child(mFixtureLabel);
        Log.d(TAG, ":readDB " + mCommon.mClub + Constants.TOURNA + mTourna + mFixtureLabel);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "readDB: onDataChange:" + dataSnapshot.getKey() + dataSnapshot.toString());

                GenericTypeIndicator<Map<String, TournaFixtureDBEntry>> genericTypeIndicator =
                        new GenericTypeIndicator<Map<String, TournaFixtureDBEntry>>() {
                        };
                Map<String, TournaFixtureDBEntry> map = dataSnapshot.getValue(genericTypeIndicator);
                if (null == map) {
                    ((TournaTableLayout) mActivity).stopProgressDialog(false);
                    return;
                }
                mMaxRounds = 0;
                mFixture.clear();
                mData.clear();
                for (Map.Entry<String, TournaFixtureDBEntry> entry : map.entrySet()) {
                    String index = entry.getKey();
                    final TournaFixtureDBEntry dbEntry = entry.getValue();
                    TournaDispMatchEntry dispEntry = new TournaDispMatchEntry(dbEntry, index);
                    mFixture.put(index, dispEntry);
                    if (mTournaType.equals(Constants.SE) &&
                            dbEntry.getF() && null != dbEntry.getW() && !dbEntry.getW().isEmpty()) {
                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Snackbar.make(mActivity.findViewById(R.id.upper_header),
                                        "Tournament completed, " + dbEntry.getW() + " is the winner!",
                                        Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                        });
                    }
                }
                printData();
                loopThruFixture();

                //SGO: Filling the display rows with newly created views is a time consuming process (~1s for 27 teams DE)
                //This shows up as a lag on the screen. Thus, offload the processing of createDisplayData()
                //into a worker thread.
                mWorkerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        createDisplayData();
                    }
                });

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                ((TournaTableLayout) mActivity).stopProgressDialog(false);
                Log.w(TAG, "readDB:onCancelled", databaseError.toException());
                Toast.makeText(mActivity, "DB error while fetching fixture entry: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    TeamDBEntry getTeam(final String teamId) {
        if (mTeams == null || teamId.isEmpty()) return null;
        for (TeamDBEntry team : mTeams) {
            if (teamId.equals(team.getId())) return team;
        }
        return null;
    }

    void loopThruFixture() {
        HashMap<String, TournaDispMatchEntry> deltaMap = new HashMap<>();
        for (Map.Entry<String, TournaDispMatchEntry> entry : mFixture.entrySet()) {
            TournaDispMatchEntry mE = entry.getValue();
            if (mE == null || mE.xy == null) continue;
            if (mE.xy.getRound() > mMaxRounds) {
                mMaxRounds = mE.xy.getRound();
            }
            if (mE.isThereAWinner(true)) continue;  //there is already a winner
            Boolean changed = false;
            if (mE.getT1(true).isEmpty() && !mE.getPr1(true).isEmpty()) {
                //team not filled, but previous match link is there.
                //might be next round, see if that can be filled.
                TournaDispMatchEntry prev1 = getTheMatchFromFixture(mE.getPr1(true));
                if (null != prev1 && prev1.isThereAWinner(true)) {
                    mE.setT1(true, prev1.getW());
                    changed = true;
                }
            }
            if (mE.getT2(true).isEmpty() && !mE.getPr2(true).isEmpty()) {
                //team not filled, but previous match link is there.
                //might be next round, see if that can be filled.
                TournaDispMatchEntry prev2 = getTheMatchFromFixture(mE.getPr2(true));
                if (null != prev2 && prev2.isThereAWinner(true)) {
                    mE.setT2(true, prev2.getW());
                    changed = true;
                }
            }
            if (changed) deltaMap.put(entry.getKey(), mE);
        }
        for (Map.Entry<String, TournaDispMatchEntry> entry : deltaMap.entrySet()) {
            mFixture.put(entry.getKey(), entry.getValue());
            //Log.d(TAG, "loopThruFixture: updating.. " + entry.getKey() + " " + entry.getValue().toString());
        }
        //Log.d(TAG, "loopThruFixture: " + mMaxRounds);
        Log.d(TAG, "loopThruFixture: UPDATED=" + mFixture.toString());
    }

    void createDisplayData() {

        Log.d(TAG, "createDisplayData: start");
        for (int c = 1; c <= mMaxRounds; c++) {
            SparseArray<TournaDispMatchEntry> rMatches = getMatchesForThisRound(c);  //this from mFixture
            TournaDispMatchEntry lastNode = null;
            for (int i = 0; i < rMatches.size(); i++) {   //top to bottom matchIds
                //Matches will be returned in order, where i = key. but, safer to
                //iterate like this. get key first and then iterate.
                int key = rMatches.keyAt(i);
                insertANode(c, rMatches.get(key), lastNode);  //this is to mData.
                lastNode = rMatches.get(i);
                updateCoordinates();
            }
            //printDispData();
        }
        //printDispData();
        //Log.d(TAG, "createDisplayData: about to markVerticalLines");
        markVerticalLines();
        //Log.d(TAG, "createDisplayData: about to displayTable");
        displayTable();
        Log.d(TAG, "createDisplayData: end");
    }

    void insertANode(final int round, final TournaDispMatchEntry matchEntry, final TournaDispMatchEntry lastNode) {
        //Log.d(TAG, "  ========= insertANode: " + matchEntry.toString());
        //if(lastNode!=null) Log.d(TAG, "  ========= insertANode lastNode: " + lastNode.toString());


        /*
        If the LB occupies a lot of layout space, pdf generation (creation of bitmap) leads to OOM.
        So, try to reduce the number of rows (and hence the height of the layout).
        For example: the below cell need not be displayed. Winner is already decided and has moved onto the
        next round. Here, winner's team name may not be known, but whoever moves here from UB, moves to next
        round for sure.
            1-31 e-0: l: "fixU", m: "2-8", s: false
            f: false
            t 0: "2-8*", 1: "(bye)"
        For 54 teams (32 matches in R1), tiny display gives below layout sizes:
         UB: createPDF: Measured W=1620x H=6858
                createPDF(0) W=1620, divBy=2.0x H=0 - 4100
                createPDF(1) W=1620, divBy=2.0x H=4100 - 6858
         LB: createPDF: Measured W=2662x H=9900
                createPDF(0) W=2662, divBy=2.0x H=0 - 4100
                createPDF(1) W=2662, divBy=2.0x H=4100 - 8200
         */
        if(mMaxRounds>4 && round<mMaxRounds-1 &&  //many rounds and if these are the initial rounds
                matchEntry.isExternalLink(0) &&
                matchEntry.oneTeamGettingABye(true)) {
            //Log.d(TAG, "insertANode: skipping " + matchEntry.toString());
            return;
        }
        /*
        After the above optimization to reduce the num of rows to display, the same example above
        (54 teams, tiny display) gives below layout sizes. Thats a huge improvement.
        and the display one screen is also concise.
        UB: createPDF: Measured W=1620x H=6858
                createPDF(0) W=1620, divBy=2.0x H=0 - 4100
                createPDF(1) W=1620, divBy=2.0x H=4100 - 6858
        LB: createPDF: Measured W=2662x H=3696
                createPDF(0) W=2662, divBy=2.0x H=0 - 3696
         */

        if (mData.size() == 0) {
            //first entry
            //r=1 m=1>1-1:FixtureDBEntry{t1='UB2-1', t2='(bye)', pr1='', pr2='', W='UB2-1'}
            insertRow(0);
            insertColumn(0);
            //First entry is the "final" match entry, start from corner.
            matchEntry.xy.setX(0);
            matchEntry.xy.setY(0);
            setCell(matchEntry);
            return;
        }

        TournaDispMatchEntry prev1 = getTheMatchFromDispMap(matchEntry.getPr1(true));
        TournaDispMatchEntry prev2 = getTheMatchFromDispMap(matchEntry.getPr2(true));
        if (prev1 == null && prev2 == null) {
            //No previous nodes to link to. Could be:
            //      * first round nodes or
            //      * Bye nodes added from upper bracket to lower bracket
            if (matchEntry.xy.getRound() == 1) {
                //this is the first round
                //r=1 m=2>1-2:FixtureDBEntry{t1='UB2-4', t2='UB1-2', pr1='', pr2='', W=''}
                //r=1 m=3>1-3:FixtureDBEntry{t1='UB2-2', t2='(bye)', pr1='', pr2='', W='UB2-2'}
                Integer rowIdx = mData.size();
                insertRow(rowIdx);  //next index = size()  (last_index + 1)
                matchEntry.xy.setX(rowIdx);
                matchEntry.xy.setY(0);
                //Log.d(TAG, "insertANode: matchEntry=" + matchEntry.toString());
                setCell(matchEntry);
                return;
            } else {
                //Not first round, but still there are no previous node links.
                //This is the case where a node is added to the next round of lower bracket
                //from upper bracket.
                //3-1:FixtureDBEntry{t1='L3-1', t2='(bye)', pr1='', pr2='', W='L3-1'}
                //3-3:FixtureDBEntry{t1='3-2*', t2='(bye)', pr1='', pr2='', W='3-2*', ext='fixU_3-2'}

                if (matchEntry.oneTeamGettingABye(true)) {
                    if (lastNode == null) {  //first row
                        //Node added from UB; First seed gets a bye; to be added to the first row
                        //r=3 m=1>3-1:FixtureDBEntry{t1='UB4-1', t2='(bye)', pr1='', pr2='', W='UB4-1'}
                        insertRow(0);
                        insertColumn(matchEntry.xy.getRound() - 1);
                        //First entry is the "final" match entry, start from corner.
                        matchEntry.xy.setX(0);
                        matchEntry.xy.setY(matchEntry.xy.getRound() - 1);
                        setCell(matchEntry);
                        return;
                    } else {
                        //Node added from UB; gets a bye; to be added to the last row
                        //r=2 m=3>2-3:FixtureDBEntry{t1='UB3-1', t2='(bye)', pr1='', pr2='', W='UB3-1'}
                        //r=3 m=4>3-4:FixtureDBEntry{t1='UB3-1', t2='(bye)', pr1='', pr2='', W='UB3-1'}
                        Integer rowIdx = 0; //need to calculate the row in which this cell needs to be added.
                        //If a bye node is placed in between, it should be added below
                        //last node's row (if thats also a bye) or below last node's prev2
                        rowIdx = lastNode.xy.getX() + 1;
                        if (!lastNode.getPr2(true).isEmpty()) {
                            TournaDispMatchEntry lastNodePrev2 = getTheMatchFromDispMap(lastNode.getPr2(true));
                            if (lastNodePrev2 != null) {
                                rowIdx = lastNodePrev2.xy.getX() + 1;
                                //Log.e(TAG, "insertANode: SGO>"+ rowIdx + " lastNodePrev2=" + lastNodePrev2.toString());
                            }
                        }
                        //Integer rowIdx = mData.size();
                        insertRow(rowIdx);
                        insertColumn(matchEntry.xy.getRound() - 1);
                        //First entry is the "final" match entry, start from corner.
                        matchEntry.xy.setX(rowIdx);
                        matchEntry.xy.setY(matchEntry.xy.getRound() - 1);
                        setCell(matchEntry);
                        return;
                    }
                } else {  //not a bye
                    //r=2 m=1>2-1:FixtureDBEntry{t1='UB2-2', t2='UB2-16', pr1='', pr2='', W=''}
                    Integer rowIdx = matchEntry.xy.getMatchId() - 1;
                    insertRow(rowIdx);
                    insertColumn(matchEntry.xy.getRound() - 1);
                    //First entry is the "final" match entry, start from corner.
                    matchEntry.xy.setX(rowIdx);
                    matchEntry.xy.setY(matchEntry.xy.getRound() - 1);
                    setCell(matchEntry);
                    //Log.w(TAG, "insertANode: NEW:" + matchEntry.toString() );
                    return;
                }
            }
        }

        Integer offset = 0;
        Integer rowIdx = 0;
        Integer colIdx = 0;
        if (prev1 == null || prev2 == null) {
            //one team is from previous round, while the other one is already decided (coming from uppper bracket)
            //r=2 m=2>2-2:FixtureDBEntry{t1='UB2-2', t2='', pr1='', pr2='1-1', W=''}
            //No new row to be added
            if (prev1 == null && !matchEntry.getT1(true).isEmpty()) {
                rowIdx = prev2.xy.getX();
                colIdx = prev2.xy.getY() + 1;
            } else if (prev2 == null && !matchEntry.getT2(true).isEmpty()) {
                rowIdx = prev1.xy.getX();
                colIdx = prev1.xy.getY() + 1;
            }
            insertColumn(colIdx);  //no need to add a new row. new cell added in teh same row as the previous node.
            matchEntry.xy.setX(rowIdx);
            matchEntry.xy.setY(colIdx);
            //Log.d(TAG, "insertANode: matchEntry=" + matchEntry.toString());
            setCell(matchEntry);
        } else {
            //this cell should be at the centre row relative to its parent's rows
            //new cell's row will be after prev1 and before prev2
            //So, with addition of new cell, prev2 will move 1 row down. thus "prev2.xy.getX()+1"
            offset = (prev2.xy.getX() + 1 - prev1.xy.getX()) / 2;   //ex: between (1,4) and (4,4) = (4-1)+1 / 2 = 2  => new cell at (3,5)
            if (offset <= 0)
                offset = 1;  //offset has to be atleast 1; This is hit when 2 rows are immediate to each other.
            rowIdx = prev1.xy.getX() + offset;
            colIdx = prev1.xy.getY() + 1; //this cell should be to the right of previous one

            insertColumn(colIdx);
            insertRow(rowIdx);
            matchEntry.xy.setX(rowIdx);
            matchEntry.xy.setY(colIdx);
            //Log.d(TAG, "insertANode: matchEntry=" + matchEntry.toString());
            setCell(matchEntry);
        }
        return;

    }

    void updateCoordinates() {
        for (int i = 0; i < mData.size(); i++) {
            //below line we don't have to "key = mData.keyAt(i);" as i is the key here
            //mData has row numbers as the key.
            ArrayList<TournaDispMatchEntry> rowData = mData.get(i);
            if (rowData == null) continue;
            for (int j = 0; j < rowData.size(); j++) {
                TournaDispMatchEntry mN = rowData.get(j);
                if (mN == null) continue;
                mN.setCoordinates(i, j);
            }
        }
        //printData();
    }

    ArrayList<TournaDispMatchEntry> createNewRow() {
        if (mData.size() == 0) return new ArrayList<>();

        //create a new row, it should have existing number of columns
        ArrayList<TournaDispMatchEntry> newRow = new ArrayList<>(mData.get(0).size());
        for (int i = 0; i < mData.get(0).size(); i++) {
            newRow.add(null);
        }
        return newRow;
    }

    void insertRow(final Integer row_idx) {
        //Log.d(TAG, "insertRow: " + row_idx + " mData:" + mData.size());
        if (row_idx < 0) return;
        if (row_idx == mData.size()) {
            //append to end
            mData.put(row_idx, createNewRow());
            //Log.d(TAG, "insertRow: appending to mData, now:" + mData.size());
            //printData();
            return;
        }

        //HashMap<Integer,ArrayList<TournaMatchNode>> dataTmp = new HashMap<>();
        SparseArray<ArrayList<TournaDispMatchEntry>> dataTmp = new SparseArray<>();
        //copy all rows before the new row
        int new_idx = 0;
        for (int i = 0; i < row_idx; i++) {
            dataTmp.put(new_idx, mData.get(i));
            new_idx++;
        }
        dataTmp.put(new_idx, createNewRow());
        new_idx++;
        //copy all rows after the new row
        for (int i = row_idx; i < mData.size(); i++) {
            dataTmp.put(new_idx, mData.get(i));
            new_idx++;
        }

        mData = null;
        mData = dataTmp;
        //printData();
    }

    void insertColumn(final Integer col_idx) {
        //Log.d(TAG, "insertColumn: " + col_idx);
        if (col_idx < 0) return;
        for (int i = 0; i < mData.size(); i++) {
            ArrayList<TournaDispMatchEntry> rowData = new ArrayList<>(mData.get(i));
            if (rowData.size() > col_idx) return;  //column is already added
            rowData.add(col_idx, null);
            mData.put(i, rowData);
        }
        //printData();
    }

    void setCell(final TournaDispMatchEntry match) {
        if (match == null) return;
        //Log.d(TAG, "setCell: " + match.toString());
        ArrayList<TournaDispMatchEntry> rowData = mData.get(match.xy.getX());
        if (rowData == null) return;
        rowData.set(match.xy.getY(), match);
        //printData();
    }

    Integer getSuccessorRowId(final Integer round,
                              final TournaDispMatchEntry matchNode2) {
        //To draw the centre line, we need to know the rowId of the successor node.
        //It is not always the center row //if(i == row_idx1+(row_idx2-row_idx1)/2) lineNode.setVertLineCentre();

        SparseArray<TournaDispMatchEntry> rMatches = getMatchesForThisRoundFromDispMap(round + 1);  //get Data for next round
        if (rMatches == null) return -1;
        for (int i = 0; i < rMatches.size(); i++) {   //0 to max matchIds
            //It is imp to iterate here by fetching key first and then getting the value from key.
            //This fix was put in for the case where the returned rMatches SparseArray looked like below:
            // {1=(9,6) r=7 m=2>7-2:TournaFixtureDBEntry{T=[, ], P=[6-1, 6-2], E=null, W=null},
            //  3=(25,6) r=7 m=4>7-4:TournaFixtureDBEntry{T=[, ], P=[6-3, 6-4], E=null, W=null}}
            //As you can see here, index (i) and key are not equal.
            int key = rMatches.keyAt(i);
            TournaDispMatchEntry mE = rMatches.get(key);
            if (mE == null) continue;
            //Log.e(TAG, i+ " key=" + key + ":getSuccessorRowId: " + mE.toString());
            if (mE.getPr1(true).isEmpty() || mE.getPr2(true).isEmpty()) {
                if (mE.getPr1(true).equals(matchNode2.getId()) || mE.getPr2(true).equals(matchNode2.getId())) {
                    //Log.e(TAG, matchNode2.toString() + " getSuccessorRowId: YES: " + mE.toString());
                    return mE.xy.getX();
                }
            } else {
                //both previous links are valid. Means there are 2 previous nodes, no byes or additions from UB.
                if (mE.getPr2(true).equals(matchNode2.getId())) {
                    //Log.e(TAG, matchNode2.toString() + " getSuccessorRowId: 2 LINKS, YES: " + mE.toString());
                    return mE.xy.getX();
                }
            }
        }
        //Log.e(TAG, matchNode2.toString() + " getSuccessorRowId: NO: " + round);
        return -1;

    }

    Boolean markVerticalLine(final Integer row_idx1, final Integer row_idx2, final Integer col_idx,
                             final TournaDispMatchEntry matchNode2) {
        Integer centreLineRow = -1;
        for (int i = row_idx1; i <= row_idx2; i++) {
            ArrayList<TournaDispMatchEntry> rowData = mData.get(i);
            TournaDispMatchEntry mN = rowData.get(col_idx);
            if (mN == null) {
                if (i == getSuccessorRowId(col_idx + 1, matchNode2)) {
                    centreLineRow = i;
                }

            }
        }

        //Log.d(TAG, "markVerticalLine: " + matchNode2.toString() + " " +
        //        String.format(Locale.getDefault(), "%d-%d,%d, centre=%d",
        //       row_idx1, row_idx2, col_idx, centreLineRow));

        if (centreLineRow == -1) return false;  //no line drawn
        //continue only if a centreLine has to be drawn.
        //Centre line means that there is a successor for the 2 adjacent nodes (row_idx1, row_idx2)
        //in this column (col_idx).

        for (int i = row_idx1; i <= row_idx2; i++) {
            ArrayList<TournaDispMatchEntry> rowData = mData.get(i);
            TournaDispMatchEntry mN = rowData.get(col_idx);
            if (mN == null) {
                TournaDispMatchEntry lineNode = new TournaDispMatchEntry();
                if (i == centreLineRow) {
                    lineNode.setVertLineCentre();
                } else lineNode.setVertLineCell();
                rowData.set(col_idx, lineNode);
            }
        }
        return true;
    }

    void markVerticalLines() {
        for (int c = 1; c <= mMaxRounds; c++) {
            SparseArray<TournaDispMatchEntry> rMatches = getMatchesForThisRound(c);  //this from mFixture
            TournaDispMatchEntry firstNode = null;
            for (int i = 0; i < rMatches.size(); i++) {   //0 to max matchIds
                int key = rMatches.keyAt(i);
                TournaDispMatchEntry mE = rMatches.get(key);  //match from mFixture
                if (mE == null) continue;
                TournaDispMatchEntry m = getTheMatchFromDispMap(mE.getId()); //match from mData
                if (m == null) continue;
                if (firstNode == null) firstNode = m;
                else {
                    if (markVerticalLine(firstNode.xy.getX() + 1, m.xy.getX() - 1, c - 1, m)) {
                        //If a line was drawn, ignore the second node.
                        firstNode = null;
                    } else {
                        //Otherwise, this second node might be the first node of the next line.
                        firstNode = m;
                    }
                }
            }
        }
        updateCoordinates();
    }

    void setWinner(final TournaMatchNode match) {
        if (match == null) return;
        if (!match.getWinner().isEmpty()) return;
    }

    void displayTable() {

        for (int i = 0; i < mData.size(); i++) {
            final ArrayList<TournaDispMatchEntry> rowData = mData.get(i);
            //Log.d(TAG, "displayTable: " + i + " rowData=" + rowData.toString());
            if (rowData == null) continue;
            final TableRow row = getRowOfViews(mActivity, rowData);
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    //Addition to Table view can be done only in UI thread. So, send this
                    //back to the UI thread.
                    ((TableLayout) mActivity.findViewById(mTableResId)).addView(row);
                }
            });
        }
        //Once the table is complete, redraw the screen from UI thread.
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                redrawEverything();
            }
        });
    }

    //DISPLAY
    private TableRow getRowOfViews(final Activity activity, final ArrayList<TournaDispMatchEntry> rowData) {
        TableRow row = new TableRow(activity);
        for (int j = 0; j < rowData.size(); j++) {
            //Log.d(TAG, "displayTable: start" + j);
            TournaDispMatchEntry dE = rowData.get(j);
            if (dE == null) {
                TextView empty = new TextView(mActivity);
                //empty.setText("E");
                row.addView(empty);
            } else if (dE.getVertLineCell()) {
                View v = LayoutInflater.from(mActivity).inflate(R.layout.vertical_line_layout, null);
                row.addView(v);
            } else if (dE.getVertLineCentre()) {

                View v = LayoutInflater.from(mActivity).inflate(R.layout.vertical_line_centre_layout, null);
                row.addView(v);
            } else {
                GradientDrawable shape = new GradientDrawable();

                View v = LayoutInflater.from(mActivity).inflate(mCommon.mTable_view_resid, null);
                //dE.setWinnerString();   //say the DB wasnt updated for any reason, atleast correct here!
                String winner = dE.isThereAWinner(true) ? dE.getW() : "";
                String team1 = dE.getT1(true);
                String team2 = dE.getT2(true);

                int NAMELENGTH = 8;
                //tourna_match_info_tiny XML has:        android:textSize="9sp"
                int WINNER_TEXTSIZE = 11;  //13sp: 2 more than above

                if (mCommon.mTable_view_resid == R.layout.tourna_match_info_tiny) {
                    shape.setCornerRadius(10);
                    shape.setStroke(2, 0xFF000000);
                    shape.setColor(mActivity.getResources().getColor(R.color.colorSilver));
                    v.setBackground(shape);
                    NAMELENGTH = 5;
                    TeamDBEntry team1DBEntry = getTeam(team1);
                    TeamDBEntry team2DBEntry = getTeam(team2);
                    StringBuilder team1SB = new StringBuilder(team1);
                    StringBuilder team2SB = new StringBuilder(team2);
                    if (null != team1DBEntry) {
                        if (team1DBEntry.getP().size() > 0) {
                            team1SB.setLength(0);  //no need ot the team name
                            for (int x = 0; x < team1DBEntry.getP().size(); x++) {
                                team1SB.append(SharedData.truncate(team1DBEntry.getP().get(x), NAMELENGTH));
                                if (x < team1DBEntry.getP().size() - 1) team1SB.append("/");
                            }
                        }
                    }
                    //((TextView)v.findViewById(R.id.team1p_tv)).setText(team1SB);

                    //team2DBEntry = getTeam(team2);
                    if (null != team2DBEntry) {
                        if (team2DBEntry.getP().size() > 0) {
                            team2SB.setLength(0);  //no need ot the team name
                            for (int x = 0; x < team2DBEntry.getP().size(); x++) {
                                team2SB.append(SharedData.truncate(team2DBEntry.getP().get(x), NAMELENGTH));
                                if (x < team2DBEntry.getP().size() - 1) team2SB.append("/");
                            }
                        }
                    }

                    if (winner.equals(team1)) {
                        ((TextView) v.findViewById(R.id.team1p_tv)).setText(
                                mCommon.getBgColorString(team1SB.toString(), Color.WHITE));
                        ((TextView) v.findViewById(R.id.team1p_tv)).setTypeface(null, Typeface.BOLD);
                        ((TextView) v.findViewById(R.id.team1p_tv)).setTextSize(TypedValue.COMPLEX_UNIT_SP, WINNER_TEXTSIZE);
                        ((TextView) v.findViewById(R.id.team2p_tv)).setText(team2SB.toString());
                        //mCommon.getBgColorString(team2SB.toString(), Color.LTGRAY));
                    } else if (winner.equals(team2)) {
                        ((TextView) v.findViewById(R.id.team2p_tv)).setText(
                                mCommon.getBgColorString(team2SB.toString(), Color.WHITE));
                        ((TextView) v.findViewById(R.id.team2p_tv)).setTypeface(null, Typeface.BOLD);
                        ((TextView) v.findViewById(R.id.team2p_tv)).setTextSize(TypedValue.COMPLEX_UNIT_SP, WINNER_TEXTSIZE);
                        ((TextView) v.findViewById(R.id.team1p_tv)).setText(team1SB.toString());
                        //mCommon.getBgColorString(team1SB.toString(), Color.LTGRAY));
                    } else if (winner.equals(Constants.BYE)) {
                        ((TextView) v.findViewById(R.id.team1p_tv)).setText(Constants.BYE_DISPLAY);
                        v.findViewById(R.id.team2p_tv).setVisibility(View.GONE);
                    } else if (!winner.isEmpty()) {
                        //team1 and team2 are not set, but the winner is set.
                        //This is the case for LB matches added from UB
                        TeamDBEntry winDBEntry = getTeam(winner);
                        StringBuilder winSB = new StringBuilder(winner);
                        if (null != winDBEntry) {
                            if (winDBEntry.getP().size() > 0) {
                                winSB.setLength(0);  //no need ot the team name
                                for (int x = 0; x < winDBEntry.getP().size(); x++) {
                                    winSB.append(SharedData.truncate(winDBEntry.getP().get(x), NAMELENGTH));
                                    if (x < winDBEntry.getP().size() - 1) winSB.append("/");
                                }
                            }
                            ((TextView) v.findViewById(R.id.team1p_tv)).setText(
                                    mCommon.getBgColorString(winSB.toString(), Color.WHITE));
                            ((TextView) v.findViewById(R.id.team1p_tv)).setTypeface(null, Typeface.BOLD);
                            ((TextView) v.findViewById(R.id.team1p_tv)).setTextSize(TypedValue.COMPLEX_UNIT_SP, WINNER_TEXTSIZE);
                        }
                    } else {
                            //winner is also not known. this match is yet to be played.
                            //((TextView) v.findViewById(R.id.team1p_tv)).setBackgroundColor(Color.WHITE);
                            //((TextView) v.findViewById(R.id.team2p_tv)).setBackgroundColor(Color.WHITE);
                            //if the above is done, then "v.setBackground(shape)" gets overwritten
                            ((TextView) v.findViewById(R.id.team1p_tv)).setText(team1SB.toString());
                            ((TextView) v.findViewById(R.id.team2p_tv)).setText(team2SB.toString());
                    }

                    Button btn = v.findViewById(R.id.info_btn);
                    btn.setContentDescription(dE.getId());
                    btn.setOnClickListener(this);
                    TableRow.LayoutParams params = new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.setMargins(5, 5, 5, 5);
                    row.addView(v, params);
                    continue;
                }    // end of tiny match info


                shape.setCornerRadius(20);
                shape.setStroke(3, 0xFF000000);

                TeamDBEntry team1DBEntry = getTeam(team1);
                TeamDBEntry team2DBEntry = getTeam(team2);
                StringBuilder team1SB = new StringBuilder();
                StringBuilder team2SB = new StringBuilder();
                if (!winner.isEmpty()) {
                    //v.setBackgroundColor(mActivity.getResources().getColor(R.color.colorSilver));
                    //If setBackgroundColor is done after setBackground then rounded shape is not seen,
                    //So, use setBackground or setBackgroundColor.
                    shape.setColor(mActivity.getResources().getColor(R.color.colorSilver));
                    v.setBackground(shape);
                } else {
                    //v.setBackgroundColor(mActivity.getResources().getColor(R.color.colorWhite));
                    shape.setColor(mActivity.getResources().getColor(R.color.colorWhite));
                    v.setBackground(shape);
                }

                if (dE.isBye(true)) {
                    //v.setBackgroundColor(mActivity.getResources().getColor(R.color.colorSilver));
                    shape.setColor(mActivity.getResources().getColor(R.color.colorSilver));
                    v.setBackground(shape);
                }
                //int winnerColor = mActivity.getResources().getColor(R.color.colorTealGreen);
                int winnerColor = Color.BLACK;
                String id = Coordinates.getIDName(dE.getId(), mMaxRounds, mNoLabels); //with nick name
                String cellId = id;
                if (!id.equals(dE.getId())) {
                    cellId = dE.getId() + ": " + id;
                }
                ((TextView) v.findViewById(R.id.rname_tv)).setText(cellId);


                if (team1.equals(Constants.BYE)) team1SB.append(Constants.BYE_DISPLAY);
                else if (team1.contains(Constants.DE_EXTLINK_INDICATOR)) {
                    team1SB.append(team1.replace(Constants.DE_EXTLINK_INDICATOR,
                            Constants.DE_EXTLINK_INDICATOR_DISPLAY));
                } else team1SB.append(team1);
                if (team1SB.length() > 0) {
                    if (!winner.isEmpty() && !winner.equals(Constants.BYE)
                            && winner.equals(team1)) {
                        team1SB.append(" (W)");

                        ((TextView) v.findViewById(R.id.team1_tv)).setTextColor(winnerColor);
                        ((TextView) v.findViewById(R.id.team1p_tv)).setTextColor(winnerColor);
                        ((TextView) v.findViewById(R.id.team2_tv)).setTextColor(Color.GRAY);
                        ((TextView) v.findViewById(R.id.team2p_tv)).setTextColor(Color.GRAY);
                        ((TextView) v.findViewById(R.id.vs_tv)).setTextColor(Color.GRAY);
                    }
                    ((TextView) v.findViewById(R.id.team1_tv)).setText(team1SB.toString());
                    team1SB.setLength(0);
                }


                if (team2.equals(Constants.BYE)) team2SB.append(Constants.BYE_DISPLAY);
                else if (team2.contains(Constants.DE_EXTLINK_INDICATOR)) {
                    team2SB.append(team2.replace(Constants.DE_EXTLINK_INDICATOR,
                            Constants.DE_EXTLINK_INDICATOR_DISPLAY));
                } else team2SB.append(team2);
                if (team2SB.length() > 0) {

                    if (!winner.isEmpty() && !winner.equals(Constants.BYE)
                            && winner.equals(team2)) {

                        //SpannableString spanString = new SpannableString(team2SB.toString());
                        //strikethrough of losing team doesnt show up clearly as the font is small
                        //spanString.setSpan(new StrikethroughSpan(), 0, team2SB.length(), 0);

                        team2SB.append(" (W)");
                        ((TextView) v.findViewById(R.id.team2_tv)).setTextColor(winnerColor);
                        ((TextView) v.findViewById(R.id.team2p_tv)).setTextColor(winnerColor);
                        ((TextView) v.findViewById(R.id.team1_tv)).setTextColor(Color.GRAY);
                        ((TextView) v.findViewById(R.id.team1p_tv)).setTextColor(Color.GRAY);
                        ((TextView) v.findViewById(R.id.vs_tv)).setTextColor(Color.GRAY);
                    }
                    ((TextView) v.findViewById(R.id.team2_tv)).setText(team2SB.toString());
                    team2SB.setLength(0);
                }


                if (null != team1DBEntry) {
                    if (team1DBEntry.getP().size() > 0) {
                        for (int x = 0; x < team1DBEntry.getP().size(); x++) {
                            team1SB.append(SharedData.truncate(team1DBEntry.getP().get(x), NAMELENGTH));
                            if (x < team1DBEntry.getP().size() - 1) team1SB.append("/");
                        }
                        ((TextView) v.findViewById(R.id.team1p_tv)).setText(team1SB);
                        team1SB.setLength(0);
                    } else {
                        v.findViewById(R.id.team1p_tv).setVisibility(View.GONE);
                    }
                } else {
                    v.findViewById(R.id.team1p_tv).setVisibility(View.GONE);
                }


                if (null != team2DBEntry) {
                    if (team2DBEntry.getP().size() > 0) {
                        for (int x = 0; x < team2DBEntry.getP().size(); x++) {
                            team2SB.append(SharedData.truncate(team2DBEntry.getP().get(x), NAMELENGTH));
                            if (x < team2DBEntry.getP().size() - 1) team2SB.append("/");
                        }
                        ((TextView) v.findViewById(R.id.team2p_tv)).setText(team2SB);
                        team2SB.setLength(0);
                    } else {
                        v.findViewById(R.id.team2p_tv).setVisibility(View.GONE);
                    }
                } else {
                    v.findViewById(R.id.team2p_tv).setVisibility(View.GONE);
                }

                if (!winner.isEmpty() && winner.equals(Constants.BYE)) {
                    ((TextView) v.findViewById(R.id.team1_tv)).setText(Constants.BYE_DISPLAY);
                    ((TextView) v.findViewById(R.id.team2_tv)).setText("");
                    ((TextView) v.findViewById(R.id.vs_tv)).setVisibility(View.GONE);
                }

                Button btn = v.findViewById(R.id.info_btn);
                btn.setContentDescription(dE.getId());
                btn.setOnClickListener(this);
                TableRow.LayoutParams params = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 5, 0, 5);
                params.setMarginEnd(30);
                row.addView(v, params);

            }
        }
        return row;
    }

    void printDispData() {
        for (int i = 0; i < mData.size(); i++) {
            ArrayList<TournaDispMatchEntry> rowData = mData.get(i);
            if (rowData == null) continue;
            Log.d(TAG, "printDispData:" + i + " => " + rowData.toString());
        }
    }

    public SparseArray<TournaDispMatchEntry> getMatchesForThisRound(final Integer round) {
        SparseArray<TournaDispMatchEntry> tmpArr = new SparseArray<>();
        if (round == 0) return tmpArr;
        for (Map.Entry<String, TournaDispMatchEntry> entry : mFixture.entrySet()) {
            TournaDispMatchEntry mE = entry.getValue();
            if (mE == null || mE.xy == null) continue;
            if (mE.xy.getRound() == round) {
                tmpArr.put(mE.xy.getMatchId() - 1, mE);  //MatchId starts from 1
            }
        }
        //Log.d(TAG, "getMatchesForThisRound: "+ tmpArr.toString());
        return tmpArr;
    }

    public SparseArray<TournaDispMatchEntry> getMatchesForThisRoundFromDispMap(final Integer round) {
        SparseArray<TournaDispMatchEntry> tmpArr = new SparseArray<>();
        if (round == 0) return tmpArr;
        for (int i = 0; i < mData.size(); i++) {
            ArrayList<TournaDispMatchEntry> rowData = mData.get(i);
            if (rowData == null) continue;
            for (TournaDispMatchEntry mE : rowData) {
                if (mE == null) continue;
                if (mE.xy.getRound() == round) {
                    tmpArr.put(mE.xy.getMatchId() - 1, mE);  //MatchId starts from 1
                }
            }
        }
        Log.d(TAG, round + ":getMatchesForThisRoundFromDispMap: "+ tmpArr.toString());
        return tmpArr;
    }

    public TournaDispMatchEntry getTheMatchFromDispMap(final String matchId) {
        if (matchId.isEmpty()) return null;
        for (int i = 0; i < mData.size(); i++) {
            ArrayList<TournaDispMatchEntry> rowData = mData.get(i);
            if (rowData == null) continue;
            for (TournaDispMatchEntry mE : rowData) {
                if (mE == null) continue;
                if (matchId.equals(mE.getId())) {
                    return mE;
                }
            }
        }
        //Log.w(TAG, "getTheMatchFromDispMap: Not found! " + matchId);
        return null;
    }

    public TournaDispMatchEntry getTheMatchFromFixture(final String matchId) {
        if (matchId.isEmpty()) return null;
        for (Map.Entry<String, TournaDispMatchEntry> entry : mFixture.entrySet()) {
            if (matchId.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        Log.w(TAG, "getTheMatchFromFixture: Not found! " + matchId);
        return null;
    }

    void printData() {
        Log.d(TAG, "printData:=> " + mFixture.toString());
    }

    @Override
    public void onClick(View v) {
        String id = (String) v.getContentDescription();
        TournaDispMatchEntry node = getTheMatchFromDispMap(id);
        if (node == null) return;
        //SharedData.getInstance().showToast(TournaTableLayout.this, "Button clicked! " + node.toString(), Toast.LENGTH_SHORT);
        //Log.d(TAG, "onClick: " + node.toString());
        showOptions(v, node);
    }

    void updatewinner(final View v, final TournaDispMatchEntry node) {
        if (node.getT1(true).isEmpty() || node.getT2(true).isEmpty()) {
            Toast.makeText(mActivity, "Teams not know yet.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Context wrapper = new ContextThemeWrapper(mActivity, R.style.RedPopup);
        final PopupMenu popup = new PopupMenu(wrapper, v);
        popup.getMenuInflater().inflate(R.menu.summary_popup_menu, popup.getMenu());
        popup.getMenu().clear();
        Menu pMenu = popup.getMenu();
        pMenu.add(node.getT1(true));
        pMenu.add(node.getT2(true));

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (mFixtureLabel.isEmpty()) {
                    popup.dismiss();
                    return true;
                }
                String winner = menuItem.getTitle().toString();
                Log.v(TAG, "updatewinner onMenuItemClick:" + winner);
                //dont check for winner, give an option to force write into DB
                final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                        .child(Constants.TOURNA).child(mCommon.mTournament)
                        .child(mFixtureLabel).child(node.getId());
                TournaFixtureDBEntry dbEntry = new TournaFixtureDBEntry(node);
                dbEntry.setW(winner);
                dbRef.setValue(dbEntry);
                popup.dismiss();
                Toast.makeText(mActivity, "Match " + node.getId() + " winner '" + node.getW() +
                                "' overwritten with '" + winner + "'. Refresh your screen.",
                        Toast.LENGTH_LONG).show();
                Log.w(TAG, "Match " + node.getId() + " winner " + node.getW() +
                        " overwritten with '" + winner + "'");
                return true;
            }
        });
        popup.show();//showing popup menu
        Snackbar.make(v, "You are powerful! Any changes here will overwrite the master database!", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    void showOptions(final View v, final TournaDispMatchEntry node) {
        Context wrapper = new ContextThemeWrapper(mActivity, R.style.RegularPopup);
        final PopupMenu popup = new PopupMenu(wrapper, v);
        popup.getMenuInflater().inflate(R.menu.summary_popup_menu, popup.getMenu());
        if (Build.VERSION.SDK_INT >= 23) {
            popup.setGravity(Gravity.END);
        }
        popup.getMenu().clear();
        Menu pMenu = popup.getMenu();
        pMenu.add(MATCH_INFO);
        if (mCommon.isRoot() || mCommon.isAdmin()) pMenu.add(ENTER_SCORE);
        if (mCommon.isRoot()) pMenu.add(ENTER_WINNER);
        pMenu.add(VIEW_SCORE);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                //Log.v(TAG, "onMenuItemClick[" + menuItem.getItemId()+ "] :" + menuItem.getTitle().toString());
                popup.dismiss();
                String choice = menuItem.getTitle().toString();
                //SharedData.getInstance().showToast(TournaTableLayout.this, node.getId() + " selection:" + choice, Toast.LENGTH_SHORT);
                if (choice.equals(MATCH_INFO)) showMatchInfo(node);
                else if (choice.equals(ENTER_SCORE)) {
                    //not checking for winner here. root should be able to override the current DB values. So, keep going even if there is a winner.
                    ArrayList<String> teams = new ArrayList<>();
                    teams.add(node.getT1(true));
                    teams.add(node.getT2(true));

                    TeamDBEntry team1DBEntry = getTeam(node.getT1(true));
                    TeamDBEntry team2DBEntry = getTeam(node.getT2(true));
                    if (null == team1DBEntry || null == team2DBEntry) {
                        Log.e(TAG, "showOptions:onMenuItemClick: team1DBEntry null:" + node.toString());
                        Toast.makeText(mActivity, "It's a bye or Teams not known yet!",
                                Toast.LENGTH_LONG).show();
                        return true;
                    }

                    ArrayList<String> team1Players = new ArrayList<>(team1DBEntry.getP());
                    ArrayList<String> team2Players = new ArrayList<>(team2DBEntry.getP());

                    if (node.isBye(true)) {
                        Log.w(TAG, "showOptions:onMenuItemClick: BYE: " + node.toString());
                        Toast.makeText(mActivity, "Winner has got a bye!",
                                Toast.LENGTH_LONG).show();
                        return true;
                    }

                    mCommon.wakeUpDBConnection_profile();
                    //Intent myIntent = new Intent(mActivity, LoginActivity.class);
                    Intent myIntent = new Intent(mActivity, TournaBaseEnterData.class);
                    myIntent.putExtra(Constants.TOURNATYPE, mTournaType);
                    myIntent.putExtra(Constants.MATCH, node.getId());
                    myIntent.putExtra(Constants.FIXTURE, mFixtureLabel);
                    myIntent.putStringArrayListExtra(Constants.TEAMS, teams);
                    myIntent.putStringArrayListExtra(Constants.TEAM1PLAYERS, team1Players);
                    myIntent.putStringArrayListExtra(Constants.TEAM2PLAYERS, team2Players);
                    mActivity.startActivity(myIntent);
                } else if (choice.equals(VIEW_SCORE)) {
                    //not checking for winner here. root should be able to override the current DB values. So, keep going even if there is a winner.
                    ArrayList<String> teams = new ArrayList<>();
                    teams.add(node.getT1(true));
                    teams.add(node.getT2(true));

                    TeamDBEntry team1DBEntry = getTeam(node.getT1(true));
                    TeamDBEntry team2DBEntry = getTeam(node.getT2(true));
                    if (null == team1DBEntry || null == team2DBEntry) {
                        Log.e(TAG, "showOptions:onMenuItemClick: team1DBEntry null:" + node.toString());
                        Toast.makeText(mActivity, "It's a bye or Teams not known yet!",
                                Toast.LENGTH_LONG).show();
                        return true;
                    }

                    ArrayList<String> team1Players = new ArrayList<>(team1DBEntry.getP());
                    ArrayList<String> team2Players = new ArrayList<>(team2DBEntry.getP());

                    if (node.isBye(true)) {
                        Log.w(TAG, "showOptions:onMenuItemClick: BYE: " + node.toString());
                        Toast.makeText(mActivity, "Winner has got a bye!",
                                Toast.LENGTH_LONG).show();
                        return true;
                    }

                    //Intent myIntent = new Intent(TournaTableLayout.this, TournaSeeding.class);
                    Intent myIntent = new Intent(mActivity, TournaBaseEnterData.class);
                    myIntent.putExtra(Constants.TOURNATYPE, mTournaType);
                    myIntent.putExtra(Constants.MATCH, node.getId());
                    myIntent.putExtra(Constants.FIXTURE, mFixtureLabel);
                    myIntent.putStringArrayListExtra(Constants.TEAMS, teams);
                    myIntent.putStringArrayListExtra(Constants.TEAM1PLAYERS, team1Players);
                    myIntent.putStringArrayListExtra(Constants.TEAM2PLAYERS, team2Players);
                    myIntent.putExtra(Constants.EXTRAS, Constants.VIEWONLY);
                    mActivity.startActivity(myIntent);
                } else if (choice.equals(ENTER_WINNER)) {
                    //not checking for winner here. root should be able to override the current DB values. So, keep going even if there is a winner.
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updatewinner(v, node);
                        }
                    });
                }
                return true;
            }
        });
        popup.show();//showing popup menu
    }

    void showMatchInfo(final TournaDispMatchEntry node) {
        if (null == matchInfoDialog) {
            //matchInfoDialog.dismiss();
            //matchInfoDialog = null;
            matchInfoDialog = new CustomDialogClass(mActivity);
        }
        matchInfoDialog.setData(node, mMaxRounds);
        matchInfoDialog.show();
    }

    public void redrawEverything() {
        Log.e(TAG, "  ----------- redrawEverything ------------ ");
        mActivity.findViewById(mTableResId).invalidate();
        mActivity.findViewById(mTableResId).refreshDrawableState();
        drawingInProgress = false;
        ((TournaTableLayout) mActivity).stopProgressDialog(true);
    }

    public void onResume() {
        Log.e(TAG, "  ----------- onResume ------------ ");
        ((TableLayout) mActivity.findViewById(mTableResId)).removeAllViews();
        readDB();
    }

    public void onPause() {
        if (null != matchInfoDialog) {
            matchInfoDialog.dismiss();
            matchInfoDialog = null;
        }
    }

    public void onDestroy() {
        mWorkerHandler.removeCallbacksAndMessages(null);
        mMainHandler.removeCallbacksAndMessages(null);
        mWorker.quit();
        if (null != matchInfoDialog) {
            matchInfoDialog.dismiss();
            matchInfoDialog = null;
        }
    }


    public class CustomDialogClass extends Dialog implements
            android.view.View.OnClickListener {

        private Dialog d;
        private TournaDispMatchEntry node;
        private Integer mMaxRounds;

        public CustomDialogClass(final Context a) {
            super(a);
        }

        public void setData(final TournaDispMatchEntry n,
                            Integer maxRounds) {
            this.node = n;
            this.mMaxRounds = maxRounds;

        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.tourna_match_info);
            Log.d("CustomDialogClass", "onCreate: ");

            Button btn = findViewById(R.id.ok_button);
            btn.setOnClickListener(this);
        }

        @Override
        protected void onStart() {
            super.onStart();
            Log.d("CustomDialogClass", "onStart: ");

            //nullify old data
            ((TextView) findViewById(R.id.team1_tv)).setText("--");
            ((TextView) findViewById(R.id.team2_tv)).setText("--");
            ((TextView) findViewById(R.id.team1p_tv)).setText("");
            ((TextView) findViewById(R.id.team2p_tv)).setText("");
            ((TextView) findViewById(R.id.winner_tv)).setText("--");
            findViewById(R.id.team1p_tv).setVisibility(View.VISIBLE);
            findViewById(R.id.team2p_tv).setVisibility(View.VISIBLE);

            String matchId = node.getId();
            String idStr = Coordinates.getIDNameLong(matchId, mMaxRounds);
            if (!idStr.isEmpty()) matchId += ": " + idStr;
            ((TextView) findViewById(R.id.rname_tv)).setText(matchId);

            StringBuilder sb = new StringBuilder();
            String team1 = node.getT1(true);
            if (!team1.isEmpty()) {
                TeamDBEntry tInfo = getTeam(team1);
                if (team1.equals(Constants.BYE)) sb.append(Constants.BYE_DISPLAY);
                else if (team1.contains(Constants.DE_EXTLINK_INDICATOR)) {
                    team1 = team1.replace(Constants.DE_EXTLINK_INDICATOR, Constants.DE_EXTLINK_INDICATOR_DISPLAY_LONG);
                    sb.append(team1);
                } else sb.append(team1);
                ((TextView) findViewById(R.id.team1_tv)).setText(sb.toString());

                sb.setLength(0);
                if (null != tInfo) {
                    for (int i = 0; i < tInfo.getP().size(); i++) {
                        sb.append(tInfo.getP().get(i));
                        if (i < tInfo.getP().size() - 1) sb.append(" / ");
                    }
                    ((TextView) findViewById(R.id.team1p_tv)).setText(sb.toString());
                } else {
                    findViewById(R.id.team1p_tv).setVisibility(View.GONE);
                }
            }

            sb.setLength(0);
            String team2 = node.getT2(true);
            if (!team2.isEmpty()) {
                TeamDBEntry tInfo = getTeam(team2);
                if (team2.equals(Constants.BYE)) sb.append(Constants.BYE_DISPLAY);
                else if (team2.contains(Constants.DE_EXTLINK_INDICATOR)) {
                    team2 = team2.replace(Constants.DE_EXTLINK_INDICATOR, Constants.DE_EXTLINK_INDICATOR_DISPLAY_LONG);
                    sb.append(team2);
                } else sb.append(team2);
                ((TextView) findViewById(R.id.team2_tv)).setText(sb.toString());

                sb.setLength(0);
                if (null != tInfo) {
                    for (int i = 0; i < tInfo.getP().size(); i++) {
                        sb.append(tInfo.getP().get(i));
                        if (i < tInfo.getP().size() - 1) sb.append(" / ");
                    }
                    ((TextView) findViewById(R.id.team2p_tv)).setText(sb.toString());
                } else {
                    findViewById(R.id.team2p_tv).setVisibility(View.GONE);
                }
            }

            if (node.isThereAWinner(true)) {
                String winner = node.getW() + " (Winner)";
                ((TextView) findViewById(R.id.winner_tv)).setText(winner);
            }


        }

        @Override
        public void onClick(View v) {
            //Log.d("CustomDialogClass", "dismiss: ");
            dismiss();
        }
    }
}


// ============ MAIN ACTIVITY ============


public class TournaTableLayout extends AppCompatActivity {
    private static final String TAG = "TournaTableLayout";
    private TournaTable mUpperTable;
    private TournaTable mLowerTable;
    private SharedData mCommon;
    private ArrayList<TeamDBEntry> mTeams;
    private int mUpperVisibility;
    private int mLowerVisibility;
    private String mTournaType;
    private TournaFixtureDBEntry mDeFinalsDBEntry;
    //private View mainView = null;
    private Float zoomFactor;
    private ProgressDialog progressDialog;
    private HandlerThread mWorker;
    private Handler mWorkerHandler;
    private Handler mMainHandler;
    private GestureDetector mDetector;

    public void killActivity() {
        Log.d(TAG, "killActivity: ");
        finish();
    }

    public void restartActivity() {
        recreate();
        //killActivity();
        //startActivity(getIntent());
    }

    private void setTitle(String tourna) {
        if (!TextUtils.isEmpty(tourna)) {
            Log.d(TAG, "setTitle: " + tourna);
            String tempString = Constants.APPNAME + "  " + tourna;
            SpannableString spanString = new SpannableString(tempString);
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, Constants.APPNAME.length(), 0);
            spanString.setSpan(new StyleSpan(Typeface.ITALIC), Constants.APPNAME.length(), tempString.length(), 0);
            spanString.setSpan(new RelativeSizeSpan(0.7f), Constants.APPNAME.length(), tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            getSupportActionBar().setTitle(""); //workaround for title getting truncated.
            getSupportActionBar().setTitle(spanString);
            //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tourna_table_layout);
        mCommon = SharedData.getInstance();
        setTitle(mCommon.mTournament);
        mTeams = new ArrayList<>();
        mUpperVisibility = View.VISIBLE;
        mLowerVisibility = View.VISIBLE;
        findViewById(R.id.final1).setVisibility(View.GONE);
        findViewById(R.id.final2).setVisibility(View.GONE);
        Log.d(TAG, "onCreate: ");
        mTournaType = "";
        mDeFinalsDBEntry = null;
        readDBTournaType();
        zoomFactor = 1.0f;
        progressDialog = null;

        mWorker = new HandlerThread("mainWorker");
        mWorker.start();
        Looper looper = mWorker.getLooper();
        mWorkerHandler = new Handler(looper);
        mMainHandler = new Handler();

        findViewById(R.id.tourna_table_upper).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mTournaType.equals(Constants.SE)) return false;
                //upper table is longClicked, change lower table's visibility
                if (mLowerVisibility == View.GONE) mLowerVisibility = View.VISIBLE;
                else mLowerVisibility = View.GONE;
                findViewById(R.id.horizontal_scroll_view2).setVisibility(mLowerVisibility);
                return true;
            }
        });

        findViewById(R.id.tourna_table_lower).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mTournaType.equals(Constants.SE)) return false;
                //lower table is longClicked, change upper table's visibility
                if (mUpperVisibility == View.GONE) mUpperVisibility = View.VISIBLE;
                else mUpperVisibility = View.GONE;
                findViewById(R.id.horizontal_scroll_view1).setVisibility(mUpperVisibility);
                return true;
            }
        });

        TextView final1 = findViewById(R.id.final1);
        final1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mTournaType.equals(Constants.SE)) return;
                if (null == mDeFinalsDBEntry) return;
                TeamDBEntry team1DBEntry = getTeam(mDeFinalsDBEntry.getT1(true));
                TeamDBEntry team2DBEntry = getTeam(mDeFinalsDBEntry.getT2(true));
                if (null == team1DBEntry || null == team2DBEntry) {
                    Log.e(TAG, "showOptions:onMenuItemClick: team1DBEntry null:" + mDeFinalsDBEntry.toString());
                    Toast.makeText(TournaTableLayout.this, "Teams not known yet!",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                ArrayList<String> team1Players = new ArrayList<>(team1DBEntry.getP());
                ArrayList<String> team2Players = new ArrayList<>(team2DBEntry.getP());

                mCommon.wakeUpDBConnection_profile();
                ArrayList<String> teams = new ArrayList<>(mDeFinalsDBEntry.getT());
                Intent myIntent = new Intent(TournaTableLayout.this, TournaBaseEnterData.class);
                myIntent.putExtra(Constants.TOURNATYPE, mTournaType);
                myIntent.putExtra(Constants.FIXTURE, Constants.DE_FINALS);
                myIntent.putExtra(Constants.MATCH, Constants.DE_FINALS_M1);
                myIntent.putStringArrayListExtra(Constants.TEAMS, teams);
                myIntent.putStringArrayListExtra(Constants.TEAM1PLAYERS, team1Players);
                myIntent.putStringArrayListExtra(Constants.TEAM2PLAYERS, team2Players);
                TournaTableLayout.this.startActivity(myIntent);
            }
        });

        TextView final2 = findViewById(R.id.final2);
        final2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mTournaType.equals(Constants.SE)) return;
                if (null == mDeFinalsDBEntry) return;
                TeamDBEntry team1DBEntry = getTeam(mDeFinalsDBEntry.getT1(true));
                TeamDBEntry team2DBEntry = getTeam(mDeFinalsDBEntry.getT2(true));
                if (null == team1DBEntry || null == team2DBEntry) {
                    Log.e(TAG, "showOptions:onMenuItemClick: team1DBEntry null:" + mDeFinalsDBEntry.toString());
                    Toast.makeText(TournaTableLayout.this, "Teams not known yet!",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                ArrayList<String> team1Players = new ArrayList<>(team1DBEntry.getP());
                ArrayList<String> team2Players = new ArrayList<>(team2DBEntry.getP());

                mCommon.wakeUpDBConnection_profile();
                ArrayList<String> teams = new ArrayList<>(mDeFinalsDBEntry.getT());
                Intent myIntent = new Intent(TournaTableLayout.this, TournaBaseEnterData.class);
                myIntent.putExtra(Constants.TOURNATYPE, mTournaType);
                myIntent.putExtra(Constants.FIXTURE, Constants.DE_FINALS);
                myIntent.putExtra(Constants.MATCH, Constants.DE_FINALS_M2);
                myIntent.putStringArrayListExtra(Constants.TEAMS, teams);
                myIntent.putStringArrayListExtra(Constants.TEAM1PLAYERS, team1Players);
                myIntent.putStringArrayListExtra(Constants.TEAM2PLAYERS, team2Players);
                TournaTableLayout.this.startActivity(myIntent);
            }
        });

        setUpGesture();
        mMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(TournaTableLayout.this,
                        "No connection to internet!", Toast.LENGTH_LONG).show();
                killActivity();
            }
        }, 3000);
    }


    /**
     * zooming is done from here
     */
    public void zoom(final Boolean zoomIn, Float scaleX, Float scaleY, PointF pivot) {
        Boolean changed = false;
        if (zoomIn) {
            if (mCommon.mTable_view_resid == R.layout.tourna_match_info_tiny) {
                mCommon.mTable_view_resid = R.layout.tourna_match_info_small;
                changed = true;
            } else if (mCommon.mTable_view_resid == R.layout.tourna_match_info_small) {
                mCommon.mTable_view_resid = R.layout.tourna_match_info_medium;
                changed = true;
            } else if (mCommon.mTable_view_resid == R.layout.tourna_match_info_medium) {
                mCommon.mTable_view_resid = R.layout.tourna_match_info_large;
                changed = true;
            }
        } else {
            if (mCommon.mTable_view_resid == R.layout.tourna_match_info_large) {
                mCommon.mTable_view_resid = R.layout.tourna_match_info_medium;
                changed = true;
            } else if (mCommon.mTable_view_resid == R.layout.tourna_match_info_medium) {
                mCommon.mTable_view_resid = R.layout.tourna_match_info_small;
                changed = true;
            } else if (mCommon.mTable_view_resid == R.layout.tourna_match_info_small) {
                mCommon.mTable_view_resid = R.layout.tourna_match_info_tiny;
                changed = true;
            }
        }

        if (changed) restartActivity();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.zoom_menu_main, menu);
        return true;
    }

    private void readAndDisplay() {
        //if there was a Runner to toast "no n/w connection", remove it
        mMainHandler.removeCallbacksAndMessages(null);

        startProgressDialog("Sync database", "Loading...");
        readDBDEFinals();
        if (null != mLowerTable) mLowerTable.onResume();
        if (null != mUpperTable) mUpperTable.onResume();
    }

    public void startProgressDialog(final String title, final String msg) {
        if (progressDialog != null) {
            return;
        }
        progressDialog = new ProgressDialog(TournaTableLayout.this);
        progressDialog.setTitle(title); // Setting Title
        progressDialog.setMessage(msg); // Setting Message
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style Spinner
        progressDialog.show(); // Display Progress Dialog
        progressDialog.setCancelable(false);
    }

    public void stopProgressDialog(final Boolean showTips) {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;

            if (showTips && mCommon.mCount < 3 && mTournaType.equals(Constants.DE)) {
                mCommon.showAlert(null, TournaTableLayout.this,
                        "Navigation Tips",
                        "(1) Long-press the fixture area to toggle between full screen and split screen.\n\n" +
                                "(2) Swipe left or right to move between upper and lower bracket full screens.\n\n" +
                                "(3) Press + or - buttons on top right of the screen to zoom in or out.\n\n" +
                                "(4) Press on the match to see options to view match information or score.\n");
                mCommon.mCount++;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_refresh:
                readAndDisplay();
                break;
            // action with ID action_settings was selected
            case R.id.action_export:
                exportPDF();
                break;
            case R.id.action_logout:
                SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.commit();
                mCommon.clear();
                Toast.makeText(TournaTableLayout.this, "Cache cleared!", Toast.LENGTH_SHORT)
                        .show();
                SharedData.getInstance().killActivity(this, RESULT_OK);
                Intent intent = new Intent(TournaTableLayout.this, MainSigninActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
            case R.id.action_about:
                //int versionCode = BuildConfig.VERSION_CODE;
                AlertDialog.Builder builder = new AlertDialog.Builder(TournaTableLayout.this);
                builder.setMessage("Version: " + BuildConfig.VERSION_NAME)
                        .setTitle(SharedData.getInstance().getTitleStr(Constants.APPNAME, TournaTableLayout.this))
                        .setNeutralButton("Ok", null).show();
                break;
            case R.id.zoom_in:
                zoomFactor += 0.2f;
                zoom(true, zoomFactor, zoomFactor, new PointF(0, 0));
                break;
            case R.id.zoom_out:
                zoomFactor -= 0.2f;
                zoom(false, zoomFactor, zoomFactor, new PointF(0, 0));
                break;
            default:
                break;
        }
        return true;
    }

    void readDBTournaType() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                .child(Constants.TOURNA).child(mCommon.mTournament).child(Constants.TYPE);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "readDBTournaType: onDataChange:" + dataSnapshot.getKey() + dataSnapshot.toString());
                mTournaType = dataSnapshot.getValue(String.class);
                if (null == mTournaType) mTournaType = "";
                Log.v(TAG, "readDBTournaType: " + mTournaType);
                readDBTeamInfo();
                if (mTournaType.equals(Constants.SE)) {
                    HorizontalScrollView lowerView = findViewById(R.id.horizontal_scroll_view2);
                    lowerView.setVisibility(View.GONE);
                    //findViewById(R.id.lb_btn).setVisibility(View.GONE);
                    //findViewById(R.id.lb_rl).setVisibility(View.GONE);
                    lowerView.setEnabled(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "readDBTournaType:onCancelled", databaseError.toException());
                Toast.makeText(TournaTableLayout.this, "DB error while fetching team entry: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    void readDBDEFinals() {

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                .child(Constants.TOURNA).child(mCommon.mTournament)
                .child(Constants.DE_FINALS); //<fixture>/<matchId>
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "readDBDEFinals: onDataChange: " + dataSnapshot.getKey());

                DataSnapshot matchDS1 = dataSnapshot.child(Constants.DE_FINALS_M1);
                TournaFixtureDBEntry match1 = matchDS1.getValue(TournaFixtureDBEntry.class);
                if (null == match1) return;
                DataSnapshot matchDS2 = dataSnapshot.child(Constants.DE_FINALS_M2);
                TournaFixtureDBEntry match2 = matchDS2.getValue(TournaFixtureDBEntry.class);
                findViewById(R.id.final1).setVisibility(View.VISIBLE);
                if (null == match2) mDeFinalsDBEntry = match1;
                else {
                    findViewById(R.id.final2).setVisibility(View.VISIBLE);
                    mDeFinalsDBEntry = match2;
                }
                Log.d(TAG, "readDBDEFinals: onDataChange: " + mDeFinalsDBEntry.toString());

                if (!mDeFinalsDBEntry.getT2(true).isEmpty()) {
                    if (mDeFinalsDBEntry.isThereAWinner(true) &&
                            !mDeFinalsDBEntry.getW().isEmpty()) {
                        Snackbar.make(findViewById(R.id.final1),
                                "Tournament completed, " + mDeFinalsDBEntry.getW() + " is the winner!",
                                Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        Log.d(TAG, "readDBDEFinals:" +
                                "Tournament completed, " + mDeFinalsDBEntry.getW() + " is the winner!");
                    }

                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "readDBDEFinals:onCancelled", databaseError.toException());
                Toast.makeText(TournaTableLayout.this, "DB error while reading Finals: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }


    void readDBTeamInfo() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                .child(Constants.TOURNA).child(mCommon.mTournament).child(Constants.TEAMS);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "readDBTeamInfo: onDataChange:" + dataSnapshot.getKey() + dataSnapshot.toString());
                GenericTypeIndicator<List<TeamDBEntry>> genericTypeIndicator =
                        new GenericTypeIndicator<List<TeamDBEntry>>() {
                        };
                List<TeamDBEntry> teamList = dataSnapshot.getValue(genericTypeIndicator);
                if (null == teamList) return;
                mTeams = new ArrayList<>(teamList);
                Log.v(TAG, "readDBTeamInfo: " + mTeams.toString());

                if (mTournaType.equals(Constants.SE) || mTournaType.equals(Constants.DE)) {
                    mUpperTable = new TournaTable(TournaTableLayout.this,
                            R.id.tourna_table_upper, Constants.FIXTURE_UPPER,
                            mCommon.mTournament, mTournaType,
                            mTeams, false);
                    //mUpperTable.singleElimination(); //TODO: SGO comment out
                }
                if (mTournaType.equals(Constants.DE)) {
                    mLowerTable = new TournaTable(TournaTableLayout.this,
                            R.id.tourna_table_lower, Constants.FIXTURE_LOWER,
                            mCommon.mTournament, mTournaType,
                            mTeams, true);
                    mUpperTable.setExternalTable(mLowerTable);
                    mLowerTable.setExternalTable(mUpperTable);
                    //mLowerTable.doubleElimination();  //TODO: create Fixture and write to DB from else where
                }
                readAndDisplay();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "readDBTeamInfo:onCancelled", databaseError.toException());
                Toast.makeText(TournaTableLayout.this, "DB error while fetching team entry: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    TeamDBEntry getTeam(final String teamId) {
        if (mTeams == null || teamId.isEmpty()) return null;
        for (TeamDBEntry team : mTeams) {
            if (teamId.equals(team.getId())) return team;
        }
        return null;
    }

    private void redrawEverything() {
        //Log.e(TAG, "  ----------- redrawEverything ------------ ");
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        //Coming back from TournaBaseEnterData, refresh the view, if there was
        //a DB update performed.
        if (mCommon.isDBUpdated()) {
            Log.d(TAG, "onResume: DB updated");
            readAndDisplay();
            mCommon.setDBUpdated(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
        if (null != mLowerTable) mLowerTable.onPause();
        if (null != mUpperTable) mUpperTable.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
        mWorkerHandler.removeCallbacksAndMessages(null);
        mMainHandler.removeCallbacksAndMessages(null);
        mWorker.quit();
        if (null != mLowerTable) mLowerTable.onDestroy();
        if (null != mUpperTable) mUpperTable.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d(TAG, "onBackPressed: ");
        if (null != mLowerTable) mLowerTable.onDestroy();
        if (null != mUpperTable) mUpperTable.onDestroy();

        //we could be here from TournaSeeding as well.
        SharedData.getInstance().killActivity(this, RESULT_OK);
        Intent intent = new Intent(TournaTableLayout.this, TournaLanding.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }


    /**
     * Prints the contents on the screen to a PDF file,
     * which i then saved in Documents/PDF
     */
    public void exportPDF() {

        if (mCommon.mTable_view_resid != R.layout.tourna_match_info_tiny &&
                mCommon.mTable_view_resid != R.layout.tourna_match_info_small) {
            Toast.makeText(TournaTableLayout.this, "Zoom out to generate PDF.",
                    Toast.LENGTH_LONG).show();
                    /*
                    I/art: Alloc concurrent mark sweep GC freed 27(952B) AllocSpace objects, 0(0B) LOS objects, 15% free, 87MB/103MB, paused 896us total 64.509ms
                    W/art: Throwing OutOfMemoryError "Failed to allocate a 55041780 byte allocation with 16773160 free bytes and 40MB until OOM"
                    E/AndroidRuntime: FATAL EXCEPTION: mainWorker
                    Process: com.sg0.baddytally, PID: 15625
                    java.lang.OutOfMemoryError: Failed to allocate a 55041780 byte allocation with 16773160 free bytes and 40MB until OOM
                    (that is: 52MB with 16MB free, 40MB until OOM.)
                     */
            return;
        }


        ActivityManager.MemoryInfo memoryInfo = mCommon.getAvailableMemory(this);
        if (memoryInfo.lowMemory) {
            // Note: This is device's overall memory status, not specific to this app
            Log.e(TAG, "exportPDF: device has LOW MEMORY");
            Toast.makeText(TournaTableLayout.this, "Not enough memory to generate PDF.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (isExternalStorageWritable()) {
            startProgressDialog("Export PDF", "Adding page...");
            mWorkerHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mUpperTable!=null) {
                        final Boolean ubPdf = exportPDF_work(R.id.tourna_table_upper);
                        successPDFToast(ubPdf, "Upper");
                    }
                    if(mLowerTable!=null) {
                        final Boolean lbPdf = exportPDF_work(R.id.tourna_table_lower);
                        successPDFToast(lbPdf, "Lower");
                    }
                }
            });
        } else {
            Toast.makeText(TournaTableLayout.this,
                    "External storage not writable!\nGive 'storage' app permission and try again.",
                    Toast.LENGTH_LONG).show();
        }
    }

    void successPDFToast(final Boolean retVal, final String bracket) {
        Log.d(TAG, "successPDFToast(" + bracket + "): " + retVal);
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                System.gc();   //bitmap consumes a lot of memory
                stopProgressDialog(false);
                if (retVal)
                    Toast.makeText(TournaTableLayout.this,
                            bracket + " bracket PDF is exported into '" +
                                    Environment.DIRECTORY_DOCUMENTS + "' directory.",
                            Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Checks if external storage is available for read and write
     *
     * @return boolean
     */
    private boolean isExternalStorageWritable() {
        if (isStoragePermissionGranted()) {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                return true;
            }
        }
        return false;
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                //Log.v(TAG,"Permission is granted");
                return true;
            } else {
                //Log.v(TAG,"Permission is revoked:" +Build.VERSION.SDK_INT);
                mMainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //give time for the error toast to show up
                        if (Build.VERSION.SDK_INT >= 23) {
                            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                        }
                    }
                }, 2000);

                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            //Log.v(TAG,"Permission is granted:" +Build.VERSION.SDK_INT);
            return true;
        }
    }


    // ----- Below functions are invoked on a different worker thread ------

    public Boolean exportPDF_work(final Integer tableid) {
        String filename = getFileName(tableid);
        File dir = getAlbumStorageDir("ScoreTally");
        Boolean retVal = false;
        if (null == dir) return retVal;
        File file = new File(dir, filename);
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            retVal = createPDF(outputStream, tableid);
        } catch (final IOException | OutOfMemoryError e) {
            //OOM exception caught here!
            Log.e(TAG, "exportPDF_work:" + e.getMessage());
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(TournaTableLayout.this,
                            "Layout is too big to print!\n(" +
                                    e.getMessage() + ")",
                            Toast.LENGTH_LONG).show();
                }
            });
            retVal = false;
        }
        return retVal;
    }


    /**
     * Returns a name for the file that will be created
     *
     * @return String
     */
    private String getFileName(final Integer tableid) {
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat(Constants.ROUND_DATEFORMAT, Locale.CANADA);
        String dateStr = df.format(c);
        String fname = "scoretally" + dateStr + "_" + tableid + ".pdf";
        Log.d(TAG, "getFileName: " + fname);
        return fname;
    }

    /**
     * Creates a PDF document and writes it to external storage using the
     * received FileOutputStream object
     *
     * @param outputStream a FileOutputStream object
     * @throws IOException
     */

    private Boolean createPDF(FileOutputStream outputStream, final Integer tableid) throws IOException {
        //Log.d(TAG, "createPDF: ");
        PrintedPdfDocument document = new PrintedPdfDocument(TournaTableLayout.this,
                getPrintAttributes());

        View mView = findViewById(tableid);

        float divBy = 2.0f;  //you can shrink the size of the image printed into pdf by increasing this number

        //SGO: dont go too crazy on bitmap size or you will have to deal with OOM issues
        //     while invoking Bitmap.createBitmap().
        //So, keep a limit. If the height is too big, split into multiple pages.
        //If the width is too big, shrink the picture.
        int width = mView.getWidth();
        final int HEIGHT_PAGE_LIMIT = 4100;  //4000 was splitting a little part for 16 teams DE
        final int WIDTH_PAGE_LIMIT = 4100;   //not tested
        final int HEIGHT_HARD_LIMIT = 9000;  //not tested. 9864 height lead to OOM on LG (54 teams)

        if (width > WIDTH_PAGE_LIMIT) divBy = 2.5f;
        int heightPerPage = HEIGHT_PAGE_LIMIT;

        Log.d(TAG, "createPDF: Measured W=" + mView.getMeasuredWidth() +
                "x H=" + mView.getMeasuredHeight());
        Log.d(TAG, "createPDF: W=" + mView.getWidth() +
                "x H=" + mView.getHeight());

        /*
        In testing (with NO manifest entry for android:largeHeap="true") it was found that:
        createPDF: W=2892x H=7266 --> pdf generation was a success
        createPDF: W=3132x H=8787 --> caused OOM
                   -----This was before changes to not pass around getApplicationContext()
                        After the change to pass Activity instead of app context,
                        heap usage has considerable gone down.
                        Also, a 32 team fixture is giving below dimensions for the tiny screen:
                        LB createPDF: W=1808x H=7592. This HARD LIMIT cant be 6000. Changed to 16k.
                        small screen: LB createPDF: Measured W=2308x H=10442

                        March11: OOM is still seen on LG phone for 54 teams tiny screen.
                        LB: createPDF: W=2662x H=9864
                        java.lang.OutOfMemoryError: Failed to allocate a 52515948 byte allocation with 16773336 free bytes and 42MB until OOM

                        March12: Adding android:noHistory="true" to initial layouts helped to reduce memory footprint.

        For reference: For 16 teams, tiny screen layout gives:
            UB createPDF: W=1072x H=1680
            LB createPDF: W=1602x H=2256

        if (mView.getHeight() > HEIGHT_HARD_LIMIT) {
            String bracket = "upper";
            if (tableid == R.id.tourna_table_lower)
                bracket = "lower";
            final String msgStr = "Layout too big to generate pdf for " + bracket +
                    " bracket. Zoom out and try.";
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(TournaTableLayout.this,
                            msgStr,
                            Toast.LENGTH_LONG).show();
                }
            });
            return false;
        }
        Above code is commented as OOM exception is caught now.
        */

        //initialize a bitmap with the same size as the table view.
        //Creation of bitmap is memory intensive and could lead to OOM exception if the
        //view is too big.
        /*
        Below error is seen when bitmap is created for medium/large sizes.
                > Alloc concurrent mark sweep GC freed 25(712B) AllocSpace objects, 0(0B) LOS objects, 7% free, 118MB/128MB, paused 646us total 53.466ms
                > Throwing OutOfMemoryError "Failed to allocate a 20390412 byte allocation with 9889904 free bytes and 9MB until OOM"
        19MB is a lot of memory to ask for!
        Few changes done to reduce the memory usage:
            1> reduce the background image sizes.
            2> keep no history of the initial activities (signin, selection2, tournalanding)
            3> reduce the number of rows to display (not displaying external nodes with bye as the 2nd team) in LB
         */
        Bitmap screen = Bitmap.createBitmap(mView.getWidth(), mView.getHeight(), Bitmap.Config.RGB_565);
        //Bitmap screen = Bitmap.createBitmap(mView.getWidth(), mView.getHeight(),Bitmap.Config.ARGB_8888);

        //Bind a canvas to it
        Canvas canvas = new Canvas(screen);
        canvas.drawColor(Color.WHITE);
        mView.draw(canvas);

        //Add watermark
        //createBitMap is a memory expensive process. If the image is too big,
        //forget about watermark. You might end up with OOM issues.
        if(mView.getHeight() < HEIGHT_PAGE_LIMIT)
            screen = mark(screen, "ScoreTally");

        int pageNum = 0;
        int startHeight = 0;
        int endHeight = heightPerPage;
        float scale = 0;

        //Loop if you need multiple pages
        while (startHeight < mView.getHeight()) {

            if (endHeight > mView.getHeight()) endHeight = mView.getHeight();
            Log.d(TAG, "createPDF(" + pageNum + ") W=" + width + ", divBy=" + divBy +
                    "x H=" + startHeight + " - " + endHeight);

            // start a page
            PdfDocument.Page page = document.startPage(pageNum);
            // get the pdf page canvas and measure it.
            // https://stackoverflow.com/questions/32975339/generate-pdf-from-android-using-printedpdfdocument-and-view-draw#
            // Keep in mind, the view is measured in pixels while the pdf canvas measures in points or 1/72inch.
            Canvas pageCanvas = page.getCanvas();
            float pageWidth = pageCanvas.getWidth();
            float pageHeight = pageCanvas.getHeight();


            //source rectangle to copy from
            Rect src = new Rect(0, startHeight, width, endHeight);

            //how can we fit the Rect src onto this page while maintaining aspect ratio?
            if (scale == 0) scale = Math.min(pageWidth / src.width(), pageHeight / src.height());
            //dont change the scale for the successive pages. page 1 scale should be same as page 0.

            Log.d(TAG, "createPDF: pageWidth=" + pageWidth + " pageHeight=" + pageHeight + " scale=" + scale);
            Log.d(TAG, "createPDF: src=" + src.toString());

            //calculate the destination rectangle
            float left = (pageWidth - src.width() * scale) / divBy;
            float top = (pageHeight - src.height() * scale) / divBy;
            float right = (pageWidth + src.width() * scale) / divBy;
            float bottom = (pageHeight + src.height() * scale) / divBy;
            RectF dst = new RectF(left, top, right, bottom);
            Log.d(TAG, "createPDF: dst=" + dst.toString());

            /* SGO: this below code from https://developer.android.com/reference/android/print/pdf/PrintedPdfDocument.html
                does not work for this view a scroll view. Only visible screen is printed into the pdf.

            View content = getContentView();
            getContentView().setDrawingCacheEnabled(true);
            content.draw(page.getCanvas());
            */


            //draw the selected source rectangle onto the pdf page
            pageCanvas.drawBitmap(screen, src, dst, null);

            // finish the page
            document.finishPage(page);

            pageNum++;
            startHeight += heightPerPage;
            endHeight += heightPerPage;
        }

        // write the document content
        document.writeTo(outputStream);

        //close the document
        document.close();
        document = null;
        screen.recycle();
        screen = null;

        Log.d(TAG, "createPDF: done");
        return true;
    }

    private Bitmap mark(Bitmap src, String watermark) {
        ActivityManager.MemoryInfo memoryInfo = SharedData.getInstance().getAvailableMemory( this);
        if (memoryInfo.lowMemory) return src;

        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap result = Bitmap.createBitmap(w, h, src.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(src, 0, 0, null);
        Paint paint = new Paint();
        paint.setColor(Color.LTGRAY);
        paint.setTextSize(20);
        paint.setAntiAlias(true);
        paint.setUnderlineText(false);
        canvas.drawText(watermark,  w/5, h/6, paint);
        canvas.drawText(watermark,  w/5, h/2, paint);
        canvas.drawText(watermark,  w/5, h - h/6, paint);
        return result;
    }

    private PrintAttributes getPrintAttributes() {
        PrintAttributes.Builder builder = new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A2)
                .setResolution(new PrintAttributes.Resolution("res1", "Resolution", 50, 50))
                .setMinMargins(new PrintAttributes.Margins(5, 5, 5, 5));
        PrintAttributes printAttributes = builder.build();
        return printAttributes;
    }


    private File getAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), albumName);
        if (!file.mkdirs()) {
            if (file.isDirectory()) {
                Log.d(TAG, "getAlbumStorageDir: Directory has already been created:" +
                        file.getAbsolutePath());
            } else {
                Log.d(TAG, "getAlbumStorageDir: Directory could not be created:" +
                        file.getAbsolutePath());
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(TournaTableLayout.this,
                                "File could not be created in " + Environment.DIRECTORY_DOCUMENTS,
                                Toast.LENGTH_LONG).show();
                    }
                });
                return null;
            }
        }
        return file;
    }


    void makeOtherBracketVisible() {
        if (mTournaType.equals(Constants.SE)) return;
        if (mLowerVisibility == View.VISIBLE) {
            mLowerVisibility = View.GONE;
            findViewById(R.id.horizontal_scroll_view2).setVisibility(mLowerVisibility);
            mUpperVisibility = View.VISIBLE;
            findViewById(R.id.horizontal_scroll_view1).setVisibility(mUpperVisibility);
        } else {
            //if (mUpperVisibility == View.VISIBLE) {
            mLowerVisibility = View.VISIBLE;
            findViewById(R.id.horizontal_scroll_view2).setVisibility(mLowerVisibility);
            mUpperVisibility = View.GONE;
            findViewById(R.id.horizontal_scroll_view1).setVisibility(mUpperVisibility);

        }
    }

    // ------------------- Gesture implementation ----------------------
    // Swipe to go from UB to LB and vice versa


    /*
    Below override of dispatchTouchEvent is needed for swipeLeft/Right to work
    for a scroll view.
    By default the touch listener for the scroll view get disabled and therefore scroll action
    does not happen. In order to fix this you need to override the dispatchTouchEvent method of
    the Activity and return the inherited version of this method after you're done with your own listener.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        //Log.d(TAG, "dispatchTouchEvent: ");
        mDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }


    private void setUpGesture() {
        mDetector = new GestureDetector(TournaTableLayout.this, new STGestureListener());

        findViewById(R.id.tourna_table_upper).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View v, final MotionEvent event) {
                //Log.d(TAG, "onTouch: ");
                return mDetector.onTouchEvent(event);
            }
        });

        findViewById(R.id.tourna_table_lower).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View v, final MotionEvent event) {
                //Log.d(TAG, "onTouch: ");
                return mDetector.onTouchEvent(event);
            }
        });
    }

    class STGestureListener implements GestureDetector.OnGestureListener {

        //Keeping the threshold pretty high so that simple scroll also works on the
        //UB and LB tables.
        private static final long VELOCITY_THRESHOLD = 7000;
        private static final String TAG = "TournaGesture";

        @Override
        public boolean onDown(final MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(final MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(final MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX,
                                final float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(final MotionEvent e) {
        }

        @Override
        public boolean onFling(final MotionEvent e1, final MotionEvent e2,
                               final float velocityX,
                               final float velocityY) {

            //Log.d(TAG, "onFling: velocityX=" + velocityX + " velocityY=" + velocityY);
            if (Math.abs(velocityX) < VELOCITY_THRESHOLD
                    && Math.abs(velocityY) < VELOCITY_THRESHOLD) {
                return false;//if the fling is not fast enough then it's just like drag
            }

            //if velocity in X direction is higher than velocity in Y direction,
            //then the fling is horizontal, else->vertical
            if (Math.abs(velocityX) > Math.abs(velocityY)) {
                if (velocityX >= 0) {
                    Log.i(TAG, "swipe right");
                    makeOtherBracketVisible();
                } else {//if velocityX is negative, then it's towards left
                    Log.i(TAG, "swipe left");
                    makeOtherBracketVisible();
                }
            } else {
                if (velocityY >= 0) {
                    Log.i(TAG, "swipe down");
                } else {
                    Log.i(TAG, "swipe up");
                }
            }


            return true;
        }
    }

}

