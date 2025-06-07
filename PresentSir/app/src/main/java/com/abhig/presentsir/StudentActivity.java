package com.abhig.presentsir;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import java.util.ArrayList;

public class StudentActivity extends AppCompatActivity {
    Toolbar toolbar;
    private String className;
    private String subjectName;
    private int position;
    private RecyclerView recyclerView;
    private StudentAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<StudentItem>  studentItems = new ArrayList<>();
    private DbHelper dbHelper;
    private int cid;
    MyCalender myCalender ;
    TextView subTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);

        dbHelper= new DbHelper(this);
        myCalender = new MyCalender();
        Intent intent = getIntent();
        className = intent.getStringExtra("className");
        subjectName = intent.getStringExtra("subjectName");
        position = intent.getIntExtra("position",-1);
        cid = intent.getIntExtra("cid",-1);

        setToolbar();
        loadData();
        recyclerView =findViewById(R.id.student_recyler);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter=new StudentAdapter(this,studentItems);
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(this::changeStatus);
        loadStatusData();

    }

    private void loadData() {
        Cursor cursor =dbHelper.getStudentTable(cid);
        studentItems.clear();
        while (cursor.moveToNext()){
            long sid = cursor.getLong(cursor.getColumnIndex(DbHelper.S_ID));
            int roll = cursor.getInt(cursor.getColumnIndex(DbHelper.STUDENT_ROLL_KEY));
            String name = cursor.getString(cursor.getColumnIndex(DbHelper.STUDENT_NAME_KEY));
            studentItems.add(new StudentItem(sid,roll,name));
        }
        cursor.close();
    }

    private void changeStatus(int position) {
        String status = studentItems.get(position).getStatus();
        if (status.equals("P")) status= "A";
        else status="P";

        studentItems.get(position).setStatus(status);
        adapter.notifyItemChanged(position);
    }

    private void setToolbar() {
        toolbar = findViewById(R.id.toolbar);
        subTitle = toolbar.findViewById(R.id.subtitle_toolbar);
        TextView title = findViewById(R.id.title_toolbar);
        ImageButton back = findViewById(R.id.back);
        ImageButton  save = findViewById(R.id.save);
        save.setOnClickListener(v->saveStatus());

        title.setText(className);
        subTitle.setText(subjectName+" | "+myCalender.getDate() );

        back.setOnClickListener(v-> onBackPressed());
        toolbar.inflateMenu(R.menu.student_menu);
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);
    }

    private void saveStatus() {
        for(StudentItem studentItem : studentItems){
            String status = studentItem.getStatus();
            if(status != "P") status = "A";
            long value = dbHelper.addStatus(studentItem.getSid(),cid,myCalender.getDate(),status);

            if(value == -1){
                dbHelper.updateStatus(studentItem.getSid(),myCalender.getDate(),status);
            }
        }
    }
    private void loadStatusData(){
        for(StudentItem studentItem : studentItems){
            String status = dbHelper.getStatus(studentItem.getSid(),myCalender.getDate());
            if (status!=null){
                studentItem.setStatus(status);
            }else {
                studentItem.setStatus(" ");
            }
            adapter.notifyDataSetChanged();

        }
    }

    private boolean onMenuItemClick(MenuItem menuItem){
        if(menuItem.getItemId()==R.id.add_student){
            showAddStudentDialog();
        } else if(menuItem.getItemId()==R.id.show_Calender){
            showCalender();
        }else if(menuItem.getItemId()==R.id.show_attendance_sheet){
            openSheetList();
        }
        return true;
    }

    private void openSheetList() {
        long[] idArray = new long[studentItems.size()];
        String[] nameArray = new String[studentItems.size()];
        int[] rollArray = new int[studentItems.size()];

        for(int i=0;i<idArray.length;i++){
               idArray[i] = studentItems.get(i).getSid();
        }

        for(int i=0;i<rollArray.length;i++){
            rollArray[i] = studentItems.get(i).getRoll();
        }

        for(int i=0;i<nameArray.length;i++){
            nameArray[i] = studentItems.get(i).getName();
        }
        Intent intent = new Intent(this, SheetListActivity2.class);
        intent.putExtra("cid",cid);
        intent.putExtra("idArray",idArray);
        intent.putExtra("rollArray",rollArray);
        intent.putExtra("nameArray",nameArray);
        startActivity(intent);
    }

    private void showCalender() {
        myCalender.show(getSupportFragmentManager(),"");
        myCalender.OnCalenderOkClickListener(this::onCalenderOkClicked);
    }
    private void onCalenderOkClicked(int year, int month, int day){
        myCalender.setDate(year,month,day);
        subTitle.setText(subjectName+" | "+myCalender.getDate() );
        loadStatusData();

    }

    private void showAddStudentDialog() {
        MyDialog dialog =new MyDialog();
        dialog.show(getSupportFragmentManager(),MyDialog.STUDENT_ADD_DIALOG);
        dialog.setListener((roll,name)-> addStudent(roll,name));

    }

    private void addStudent(String roll_string, String name) {
        int roll = Integer.parseInt(roll_string);
        long sid = dbHelper.addStudent(cid, Integer.valueOf(roll),name);
        StudentItem studentItem = new StudentItem(sid,roll,name);
        studentItems.add(studentItem);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch ((item.getItemId())){
            case 0:
                showUpdateStudentDialog(item.getGroupId());
            case 1:
                deleteStudent(item.getGroupId());
        }
        return super.onContextItemSelected(item);
    }

    private void showUpdateStudentDialog(int position) {
        MyDialog dialog = new MyDialog(studentItems.get(position).getRoll(),studentItems.get(position).getName());
        dialog.show(getSupportFragmentManager(),MyDialog.STUDENT_UPDATE_DIALOG);
        dialog.setListener((roll_string,name)->updateStudent(position,name));

    }

    private void updateStudent(int position, String name) {
        dbHelper.updateStudent(studentItems.get(position).getSid(),name);
        studentItems.get(position).setName(name);
        adapter.notifyItemChanged(position);
    }

    private void deleteStudent(int position) {
        dbHelper.deleteStudent(studentItems.get(position).getSid());
        studentItems.remove(position);
        adapter.notifyItemRemoved(position);
    }
}
