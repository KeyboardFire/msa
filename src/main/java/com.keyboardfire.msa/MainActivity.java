package com.keyboardfire.msa;

import android.text.Html;
import android.text.Spanned;
import android.app.Activity;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.webkit.WebView;
import android.view.View;
import android.view.Window;
import android.view.Gravity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.os.Build;
import android.graphics.Typeface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.content.ContextCompat;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Calendar;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

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

    TableRow longDescRow = null;
    int longDescIndex = 0;

    String creds;
    HashMap<Integer, String> classes;

    public String token;

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
            token = m.group(1);
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

    public int color(int id) {
        return ContextCompat.getColor(MainActivity.this, id);
    }

    @SuppressWarnings("deprecation")
    private Spanned fromHtml(String s) {
        if (Build.VERSION.SDK_INT >= 24) {
            return Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(s);
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

            int index = 0;
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
                desc.setOnClickListener(new DescClickListener(++index,
                            a.long_description));
                desc.setText(fromHtml(a.short_description));
                desc.setPadding(MainActivity.PADDING, 0, MainActivity.PADDING, 0);
                tr.addView(desc);

                TextView due = new TextView(MainActivity.this);
                String[] parts = a.date_due.split("/");
                due.setText(String.format(Locale.US, "%02d-%02d",
                            Integer.parseInt(parts[0]),
                            Integer.parseInt(parts[1])));
                due.setPadding(MainActivity.PADDING, 0, MainActivity.PADDING, 0);
                tr.addView(due);

                StatusButton sb = new StatusButton(MainActivity.this,
                        a.assignment_index_id, a.assignment_status, token);
                tr.addView(sb);

                tl.addView(tr);
            }
        }

    }

    private class DescClickListener implements View.OnClickListener {
        int index;
        String desc;
        public DescClickListener(int index, String desc) {
            this.index = index;
            this.desc = desc != null && !desc.isEmpty() ? desc :
                "<i>(no long description)</i>";
        }
        @Override public void onClick(View v) {
            if (longDescRow != null) {
                tl.removeView(longDescRow);
                if (longDescIndex == index) {
                    longDescRow = null;
                    return;
                }
            }
            longDescRow = new TableRow(MainActivity.this);
            longDescIndex = index;
            TextView dummy = new TextView(MainActivity.this);
            longDescRow.addView(dummy);
            WebView content = new WebView(MainActivity.this);
            content.getSettings().setDefaultFontSize(
                    (int)(dummy.getTextSize() /
                        getResources().getDisplayMetrics().scaledDensity));
            content.loadData("<html><head><style>*{color:#d8d8d8}</style></head><body>" +
                    desc + "</body></html>", "text/html", null);
            content.setPadding(MainActivity.PADDING, 0, MainActivity.PADDING, 0);
            content.setBackgroundColor(0);
            content.setVerticalScrollBarEnabled(false);
            content.setHorizontalScrollBarEnabled(false);
            TableRow.LayoutParams params = new TableRow.LayoutParams();
            params.span = 3;
            longDescRow.addView(content, 1, params);
            longDescRow.setBackgroundColor(color(R.color.grey1));
            tl.addView(longDescRow, index);
        }
    }

}
