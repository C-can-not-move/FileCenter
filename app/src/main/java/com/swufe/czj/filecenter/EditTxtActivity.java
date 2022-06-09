package com.swufe.czj.filecenter;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public class EditTxtActivity extends Activity implements View.OnClickListener {
    //显示打开的文本内容
    private EditText txtEditText;
    //显示打开的文件名
    private TextView txtTextTitle;
    //“保存”按钮
    private Button txtSavaButton;
    //"取消"
    private Button txtCancleButton;
    private String txtTitle;
    private String txtData;
    private String txtPath;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_txt);
        //初始化页面
        initContentView();
        //获得文件路径
        txtPath = getIntent().getStringExtra("path");
        //获得文件名
        txtTitle = getIntent().getStringExtra("title");
        //获得数据
        txtData = getIntent().getStringExtra("data");

        txtTextTitle.setText(txtTitle);
        txtEditText.setText(txtData);
    }

    /**
     *组件初始化
     */
    private void initContentView(){
        txtEditText = (EditText) findViewById(R.id.EditTextDetail);
        txtTextTitle = (TextView) findViewById(R.id.TextViewTitle);
        txtSavaButton = (Button) findViewById(R.id.ButtonRefer);
        txtCancleButton = (Button) findViewById(R.id.ButtonBack);

        txtSavaButton.setOnClickListener(this);
        txtCancleButton.setOnClickListener(this);
    }

    /**
     * 事件监听
     * @param view
     */
    @Override
    public void onClick(View view) {
        if(view.getId() == txtSavaButton.getId()){
            saveTxt();
        }else if(view.getId() == txtCancleButton.getId()){
            EditTxtActivity.this.finish();
        }
    }
    /**
     * 保存
     */
    private void saveTxt(){
        try{
            //获得内容
            String newData = txtEditText.getText().toString();
            BufferedWriter mBW = new BufferedWriter(new FileWriter(new File(txtPath)));
            mBW.write(newData,0,newData.length());
            mBW.newLine();
            mBW.close();
            Toast.makeText(EditTxtActivity.this,"保存成功！",Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(EditTxtActivity.this,"保存文件出现了错误！",Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        this.finish();
    }
}
