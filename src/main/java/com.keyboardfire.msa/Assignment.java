package com.keyboardfire.msa;

public class Assignment implements Comparable<Assignment> {
    public String groupname;
    public int section_id;
    public int assignment_id;
    public String short_description;
    public long date_assignedTicks;
    public String date_assigned;
    public long date_dueTicks;
    public String date_due;
    public long drop_box_late_timeTicks;
    public String drop_box_late_time;
    public String long_description;
    public int assignment_index_id;
    public String assignment_type;
    public boolean inc_grade_book;
    public boolean publish_grade;
    public int enroll_count;
    public int graded_count;
    public int drop_box_id;
    public boolean drop_box_ind;
    public boolean has_link;
    public boolean has_download;
    public int assignment_status;
    public boolean assessment_ind;
    public int assessment_id;
    public boolean assessment_locked;
    public boolean show_report;
    public boolean has_grade;
    public long local_nowTicks;
    public String local_now;
    public boolean major;
    public boolean lti_ind;
    public boolean lti_config_ind;
    public String lti_provider_name;
    public boolean discussion_ind;
    public boolean share_discussion;
    public boolean show_discussion_ind;
    public boolean allow_discussion_attachment;
    public int rubric_id;
    public boolean exempt_ind;
    public boolean incomplete_ind;
    public boolean late_ind;
    public boolean missing_ind;
    @Override public int compareTo(Assignment a) {
        return ((Long)this.date_dueTicks).compareTo(a.date_dueTicks);
    }
}
