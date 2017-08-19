package com.keyboardfire.msa;

import android.app.Activity;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.view.Window;
import android.view.Gravity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.graphics.Typeface;
import android.content.SharedPreferences;
import android.support.v4.widget.SwipeRefreshLayout;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Calendar;
import java.util.Arrays;

import com.google.gson.Gson;

public class MainActivity extends Activity
    implements SwipeRefreshLayout.OnRefreshListener {

    final static int PADDING = 20;

    TableLayout tl;
    SwipeRefreshLayout srl;

    String creds;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        Net.setup();

        tl = (TableLayout) findViewById(R.id.main_table);
        srl = (SwipeRefreshLayout) findViewById(R.id.srl);
        srl.setOnRefreshListener(this);

        SharedPreferences prefs = getSharedPreferences("credentials", 0);
        creds = prefs.getString("credentials", null);

        if (creds == null) {
            new LoginDialog().show(getFragmentManager(), "login");
        } else {
            new GetAssignmentsTask().execute();
        }
    }

    public void setCredentials(String creds) {
        this.creds = creds;

        SharedPreferences prefs = getSharedPreferences("credentials", 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("credentials", creds);
        editor.apply();

        new GetAssignmentsTask().execute();
    }

    @Override public void onRefresh() {
        tl.removeAllViews();
        new GetAssignmentsTask().execute();
    }

    private class GetAssignmentsTask extends AsyncTask<Void, Void, Assignment[]> {

        @Override protected Assignment[] doInBackground(Void... underscore) {
            Pattern p = Pattern.compile("__Ajax.*value=\"([^\"]+)");
            Matcher m = p.matcher(Net.doGET("https://sjs.myschoolapp.com/app"));
            if (m.find()) {
                String token = m.group(1);
                if (!Net.doPOST("https://sjs.myschoolapp.com/api/SignIn", "{\"From\":\"\"," + creds + ",\"remember\":true,\"InterfaceSource\":\"WebApp\"}", token).contains("LoginSuccessful\":true")) {
                    SharedPreferences prefs = getSharedPreferences("credentials", 0);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("credentials");
                    editor.apply();
                    new LoginDialog().show(getFragmentManager(), "login");
                    return null;
                }
            }

            Calendar now = Calendar.getInstance();
            String fmt = (now.get(Calendar.MONTH) + 1) + "%2F" +
                now.get(Calendar.DAY_OF_MONTH) + "%2F" +
                now.get(Calendar.YEAR);
            Gson gson = new Gson();
            return gson.fromJson(Net.doGET("https://sjs.myschoolapp.com/api/DataDirect/AssignmentCenterAssignments/?format=json&filter=2&dateStart=" + fmt + "&dateEnd=" + fmt + "&persona=2&statusList=&sectionList="), Assignment[].class);
        }

        @Override protected void onPostExecute(Assignment[] assignments) {
            srl.setRefreshing(false);
            if (assignments == null) return;

            Arrays.sort(assignments);

            for (Assignment a : assignments) {
                TableRow tr = new TableRow(MainActivity.this);
                tr.setGravity(Gravity.CENTER_VERTICAL);

                TextView classId = new TextView(MainActivity.this);
                switch (a.section_id) {
                    case 85872226:
                    case 85872227: // english
                        classId.setText("en");
                        break;
                    case 85872284:
                    case 85872285: // spanish
                        classId.setText("es");
                        break;
                    case 85872107: // diffeq
                        classId.setText("df");
                        break;
                    case 85872108: // linalg
                        classId.setText("ln");
                        break;
                    case 85872029:
                    case 85872030: // physics
                        classId.setText("ph");
                        break;
                    case 85871949:
                    case 85871950: // history
                        classId.setText("uh");
                        break;
                    case 85872151: // multi
                        classId.setText("mv");
                        break;
                    case 85872109: // pde
                        classId.setText("pd");
                        break;
                    default:
                        classId.setText("??");
                }
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
