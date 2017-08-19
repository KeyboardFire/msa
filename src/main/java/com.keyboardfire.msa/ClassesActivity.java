package com.keyboardfire.msa;

import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.app.Activity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.graphics.Typeface;
import android.view.Window;
import android.view.Gravity;
import android.content.Intent;

import java.util.HashMap;
import java.util.HashSet;

public class ClassesActivity extends Activity {

    int userid;

    TableLayout tl;

    HashMap<Integer, String> classes;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_classes);

        tl = (TableLayout) findViewById(R.id.classes_table);

        classes = new HashMap<>();

        Intent intent = getIntent();
        userid = intent.getIntExtra(MainActivity.EXTRA_USERID, 0);
        String classesStr = intent.getStringExtra(MainActivity.EXTRA_CLASSES);
        if (classesStr.isEmpty()) {
            new GetClassesTask().execute();
        } else {
            classes = ClassData.deserialize(classesStr);
        }
    }

    @Override public void onBackPressed() {
        android.util.Log.e("msa", "hi");
        Intent res = new Intent();
        res.putExtra(MainActivity.EXTRA_CLASSES, ClassData.serialize(classes));
        setResult(Activity.RESULT_OK, res);
        finish();
        //super.onBackPressed();
    }

    private class GetClassesTask extends AsyncTask<Void, Void, ClassData[]> {

        @Override protected ClassData[] doInBackground(Void... underscore) {
            // !!! CHANGE THESE NEXT YEAR !!!
            return MainActivity.gson.fromJson(Net.doGET("https://sjs.myschoolapp.com/api/datadirect/ParentStudentUserAcademicGroupsGet?userId=" + userid + "&schoolYearLabel=2017+-+2018&memberLevel=3&persona=2&durationList=79421,79422&markingPeriodId="), ClassData[].class);
        }

        @Override protected void onPostExecute(ClassData[] classData) {
            HashSet<String> seen = new HashSet<>();
            for (final ClassData c : classData) {
                String name = c.sectionidentifier;
                int len = name.length();
                if (name.charAt(len-3) != '(' ||
                    name.charAt(len-2) < 'A' || name.charAt(len-2) > 'G' ||
                    name.charAt(len-1) != ')') continue;
                if (seen.contains(name)) continue;
                seen.add(name);

                classes.put(c.sectionid, c.sectionidentifier + "??");

                TableRow tr = new TableRow(ClassesActivity.this);
                tr.setGravity(Gravity.CENTER_VERTICAL);

                TextView className = new TextView(ClassesActivity.this);
                className.setText(c.sectionidentifier);
                className.setPadding(MainActivity.PADDING, MainActivity.PADDING,
                        MainActivity.PADDING, MainActivity.PADDING);
                tr.addView(className);

                EditText classId = new EditText(ClassesActivity.this);
                classId.setText("??");
                classId.setFilters(
                        new InputFilter[] { new InputFilter.LengthFilter(2) });
                classId.setSelectAllOnFocus(true);
                classId.addTextChangedListener(new TextWatcher() {
                    @Override public void afterTextChanged(Editable s) {
                        classes.put(c.sectionid, c.sectionidentifier + s +
                                (s.length() < 2 ? "?" : "") +
                                (s.length() < 1 ? "?" : ""));
                    }

                    @Override public void beforeTextChanged(
                            CharSequence underscore, int overscore,
                            int aroundscore, int behindscore) {}
                    @Override public void onTextChanged(
                            CharSequence underscore, int overscore,
                            int aroundscore, int behindscore) {}
                });
                classId.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
                classId.setPadding(MainActivity.PADDING, MainActivity.PADDING,
                        MainActivity.PADDING, MainActivity.PADDING);
                tr.addView(classId);

                tl.addView(tr);
            }
        }

    }

}
