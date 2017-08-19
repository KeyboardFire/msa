package com.keyboardfire.msa;

import android.app.Activity;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.view.View;
import android.view.Window;
import android.view.Gravity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.graphics.Typeface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.widget.SwipeRefreshLayout;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Calendar;
import java.util.Arrays;
import java.util.HashMap;

import com.google.gson.Gson;

public class MainActivity extends Activity
    implements SwipeRefreshLayout.OnRefreshListener {

    final static int PADDING = 20;
    public static final String EXTRA_USERID = "com.keyboardfire.msa.USERID";
    public static final String EXTRA_CLASSES = "com.keyboardfire.msa.CLASSES";
    static final int CLASSES_INTENT_CODE = 123;

    public static Gson gson;

    int userid;

    TableLayout tl;
    SwipeRefreshLayout srl;

    String creds;
    HashMap<Integer, String> classes;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        Net.setup();
        gson = new Gson();

        tl = (TableLayout) findViewById(R.id.main_table);
        srl = (SwipeRefreshLayout) findViewById(R.id.srl);
        srl.setOnRefreshListener(this);

        SharedPreferences prefs = getSharedPreferences("data", 0);
        creds = prefs.getString("credentials", null);
        classes = ClassData.deserialize(prefs.getString("classes", ""));

        if (creds == null) {
            new LoginDialog().show(getFragmentManager(), "login");
        } else {
            new GetAssignmentsTask().execute();
        }
    }

    public void setCredentials(String creds) {
        this.creds = creds;

        SharedPreferences prefs = getSharedPreferences("data", 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("credentials", creds);
        editor.apply();

        new GetAssignmentsTask().execute();
    }

    @Override public void onRefresh() {
        new GetAssignmentsTask().execute();
    }

    private boolean login() {
        Pattern p = Pattern.compile("__Ajax.*value=\"([^\"]+)");
        Matcher m = p.matcher(Net.doGET("https://sjs.myschoolapp.com/app"));
        if (m.find()) {
            String token = m.group(1);
            String postdata = Net.doPOST("https://sjs.myschoolapp.com/api/SignIn", "{\"From\":\"\"," + creds + ",\"remember\":true,\"InterfaceSource\":\"WebApp\"}", token);
            if (!postdata.contains("LoginSuccessful\":true")) {
                SharedPreferences prefs = getSharedPreferences("data", 0);
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove("credentials");
                editor.apply();
                new LoginDialog().show(getFragmentManager(), "login");
                return false;
            }
            Pattern p2 = Pattern.compile("CurrentUserForExpired\":(\\d+)");
            Matcher m2 = p2.matcher(postdata);
            m2.find();
            userid = Integer.parseInt(m2.group(1));
        }
        return true;
    }

    private void editClasses() {
        Intent intent = new Intent(MainActivity.this, ClassesActivity.class);
        intent.putExtra(EXTRA_USERID, userid);
        intent.putExtra(EXTRA_CLASSES, ClassData.serialize(classes));
        startActivityForResult(intent, CLASSES_INTENT_CODE);
    }

    @Override public void onActivityResult(int code, int underscore, Intent res) {
        super.onActivityResult(code, underscore, res);
        switch (code) {
            case CLASSES_INTENT_CODE:
                String classStr = res.getStringExtra(EXTRA_CLASSES);
                classes = ClassData.deserialize(classStr);
                SharedPreferences prefs = getSharedPreferences("data", 0);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("classes", classStr);
                editor.apply();
                new GetAssignmentsTask().execute();
                break;
        }
    }

    private class GetAssignmentsTask extends AsyncTask<Void, Void, Assignment[]> {

        @Override protected void onPreExecute() {
            tl.removeAllViews();
        }

        @Override protected Assignment[] doInBackground(Void... underscore) {
            if (!login()) return null;

            if (classes.size() == 0) {
                editClasses();
                return null;
            }

            Calendar now = Calendar.getInstance();
            String fmt = (now.get(Calendar.MONTH) + 1) + "%2F" +
                now.get(Calendar.DAY_OF_MONTH) + "%2F" +
                now.get(Calendar.YEAR);
            return gson.fromJson(Net.doGET("https://sjs.myschoolapp.com/api/DataDirect/AssignmentCenterAssignments/?format=json&filter=2&dateStart=" + fmt + "&dateEnd=" + fmt + "&persona=2&statusList=&sectionList="), Assignment[].class);
        }

        @Override protected void onPostExecute(Assignment[] assignments) {
            srl.setRefreshing(false);
            if (assignments == null) return;

            Arrays.sort(assignments);

            for (final Assignment a : assignments) {
                TableRow tr = new TableRow(MainActivity.this);
                tr.setGravity(Gravity.CENTER_VERTICAL);

                TextView classId = new TextView(MainActivity.this);
                String text = classes.containsKey(a.section_id) ?
                    classes.get(a.section_id) :
                    classes.containsKey(a.section_id + 1) ?
                    classes.get(a.section_id + 1) : "??";
                classId.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        editClasses();
                    }
                });
                classId.setText(text.substring(text.length() - 2));
                classId.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
                tr.addView(classId);

                TextView desc = new TextView(MainActivity.this);
                desc.setText(a.short_description);
                desc.setPadding(MainActivity.PADDING, 0, MainActivity.PADDING, 0);
                tr.addView(desc);

                TextView due = new TextView(MainActivity.this);
                String[] parts = a.date_due.split("/");
                due.setText(String.format("%02d-%02d",
                            Integer.parseInt(parts[0]),
                            Integer.parseInt(parts[1])));
                due.setPadding(MainActivity.PADDING, 0, MainActivity.PADDING, 0);
                tr.addView(due);

                StatusButton sb = new StatusButton(MainActivity.this, a.assignment_index_id,
                        a.assignment_status);
                tr.addView(sb);

                tl.addView(tr);
            }
        }

    }

}
