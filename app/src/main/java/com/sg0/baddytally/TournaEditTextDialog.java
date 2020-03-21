package com.sg0.baddytally;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;


public class TournaEditTextDialog extends Dialog implements View.OnClickListener {
    static final String TAG = "TournaEditTextDialog";
    final public Activity parentActivity;
    final private SharedData mCommon;
    final private CallbackRoutine mCB;
    public Dialog d;
    public Button enter, cancel;
    private String mTitle, mKey;
    private String mT1;  //1st text
    private String mT2;  //2nd text
    private int mFirstFocus;
    private Object mObj;

    public TournaEditTextDialog(final Activity a, final CallbackRoutine cb) {
        super(a);
        this.parentActivity = a;
        mCommon = SharedData.getInstance();
        this.mCB = cb;
    }

    public void setContents(final Object in, final String key, final String title,
                            final String t1, final String t2, final int firstFocus) {
        mObj = in;
        mKey = key;
        mTitle = title;
        mT1 = t1;
        mT2 = t2;
        mFirstFocus = firstFocus;
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
        ((TextInputLayout)findViewById(R.id.et_layout_1)).setHint(mT1);
        ((TextInputLayout)findViewById(R.id.et_layout_2)).setHint(mT2);
        switch (mFirstFocus) {
            case 1: findViewById(R.id.et_layout_1).requestFocus();
                break;
            case 2: findViewById(R.id.et_layout_2).requestFocus();
                break;
        }
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

