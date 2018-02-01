package com.example.zth.two.activity;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;


import com.example.zth.two.R;
import com.example.zth.two.adapter.FileListAdapter;
import com.example.zth.two.record.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by HXL on 16/8/11.
 */
public class ListActivity extends Activity {
    ListView listView;
    List<File> list = new ArrayList<>();
    FileListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        listView = (ListView) findViewById(R.id.listView);
        if("pcm".equals(getIntent().getStringExtra("type"))){
            list= FileUtils.getPcmFiles();
        }else{
            list=FileUtils.getWavFiles();
        }

        adapter = new FileListAdapter(this, list);
        listView.setAdapter(adapter);

    }
}
