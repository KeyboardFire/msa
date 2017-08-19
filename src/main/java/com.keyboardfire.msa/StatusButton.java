package com.keyboardfire.msa;

import android.support.v7.widget.AppCompatTextView;
import android.content.Context;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import android.view.Gravity;
import android.os.AsyncTask;

@android.annotation.SuppressLint("ViewConstructor")
public class StatusButton extends AppCompatTextView {

    int id, status;
    static int size;

    public StatusButton(Context context, int id, int status) {
        super(context);
        this.id = id;
        setGravity(Gravity.CENTER);
        setStatus(status, false);
        size = getResources().getDimensionPixelSize(R.dimen.buttonsize);
    }

    private void setStatus(int status, boolean updateServer) {
        if (status < -1) status = -1;
        if (status > 1) status = 1;
        this.status = status;

        switch (status) {
            case -1: // todo
                setText("T");
                setBackgroundColor(0xff4a646d);
                //setBackgroundColor(0xff7cafc2);
                setTextColor(0xffaac4cd);
                break;
            case 0: // in progress
                setText("P");
                setBackgroundColor(0xff887150);
                //setBackgroundColor(0xffab4642);
                setTextColor(0xffe8d1b0);
                break;
            case 1: // completed
                setText("C");
                setBackgroundColor(0xff5d6742);
                //setBackgroundColor(0xffa1b56c);
                setTextColor(0xffbdc7a2);
                break;
            default:
                android.util.Log.e("MSA", "weird status?");
        }

        if (updateServer) {
            new UpdateStatusTask().execute(id, status);
        }
    }

    private class UpdateStatusTask extends AsyncTask<Integer, Void, Void> {

        @Override protected Void doInBackground(Integer... vals) {
            int id = vals[0], status = vals[1];
            Net.doPOST("https://sjs.myschoolapp.com/api/assignment2/assignmentstatusupdate?format=json&assigngmentIndexId=" + id + "&assignmentStatus=" + status, "{\"assignmentIndexId\":" + id + ",\"assignmentStatus\":" + status + "}");
            return null;
        }

    }

    @Override protected void onMeasure(int wms, int hms) {
        setMeasuredDimension(size, size);
    }

    @Override public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            if (ev.getX() < 0) {
                setStatus((status - 2) % 3 + 1, true);
            } else if (ev.getY() >= 0 && ev.getY() <= size) {
                setStatus((status + 2) % 3 - 1, true);
            }
        }
        return true;
    }

}
