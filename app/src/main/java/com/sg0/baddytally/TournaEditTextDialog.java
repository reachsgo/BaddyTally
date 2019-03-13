package com.sg0.baddytally;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.telecom.Call;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TournaEditTextDialog extends Dialog implements View.OnClickListener {
    static final String TAG = "TournaEditTextDialog";
    final public Activity parentActivity;
    final private SharedData mCommon;
    final private CallbackRoutine mCB;
    public Dialog d;
    public Button enter, cancel;
    private String mTitle, mKey;
    private String mT1, mET1;  //1st text and edit text
    private String mT2, mET2;  //2nd text and edit text
    private Object mObj;

    public TournaEditTextDialog(final Activity a, final CallbackRoutine cb) {
        super(a);
        this.parentActivity = a;
        mCommon = SharedData.getInstance();
        this.mCB = cb;
    }

    public void setContents(final Object in, final String key, final String title,
                            final String t1, final String et1,
                            final String t2, final String et2) {
        mObj = in;
        mKey = key;
        mTitle = title;
        mT1 = t1;
        mET1 = et1;
        mT2 = t2;
        mET2 = et2;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.tourna_edittext_dialog);
        Log.d(TAG, "onCreate:");

        enter = findViewById(R.id.enter_button);
        enter.setOnClickListener(this);

        cancel = findViewById(R.id.cancel_button);
        cancel.setOnClickListener(this);


    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart:");
        ((TextView)findViewById(R.id.t_title)).setText(mTitle);
        ((TextView)findViewById(R.id.t_1)).setText(mT1);
        ((EditText)findViewById(R.id.et_1)).setText("");
        //text has to be empty for hint to take effect. Without emptying text of edittext,
        //hint will not be set when invoked a second time.
        ((EditText)findViewById(R.id.et_1)).setHint(mET1);
        ((TextView)findViewById(R.id.t_2)).setText(mT2);
        ((EditText)findViewById(R.id.et_2)).setText("");
        ((EditText)findViewById(R.id.et_2)).setHint(mET2);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.cancel_button:
                mCB.callback(mKey,null);
                break;
            case R.id.enter_button:
                ArrayList<String> outList = new ArrayList<>(3);
                outList.add((String)mObj);
                outList.add(((EditText)findViewById(R.id.et_1)).getText().toString());
                outList.add(((EditText)findViewById(R.id.et_2)).getText().toString());
                mCB.callback(mKey, outList);
                break;
            default:
                break;
        }
        dismiss();
    }
} //end of TournaDialogClass

