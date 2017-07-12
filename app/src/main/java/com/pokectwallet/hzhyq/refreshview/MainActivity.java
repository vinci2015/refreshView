package com.pokectwallet.hzhyq.refreshview;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pokectwallet.hzhyq.refreshview.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements RefreshView.RefreshListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding binding;
    private List<String> s;
    private MyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        s = new ArrayList<>();
        s.add("aaa");
        s.add("bbb");
        s.add("ccc");
        binding.refreshView.setRefreshListener(this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyAdapter();
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    public void onFresh() {
        binding.getRoot().postDelayed(new Runnable() {
            @Override
            public void run() {
                s.add("ddd");
                s.add("eee");
                s.add("fff");
                binding.refreshView.finishRefresh(true);
                adapter.notifyItemRangeChanged(3,3);
            }
        }, 2000);
    }

    public void ToTest(View view){
        startActivity(new Intent(MainActivity.this,TestActivity.class));
    }

    /*@Override
    public void onRefresh() {
        binding.getRoot().postDelayed(new Runnable() {
            @Override
            public void run() {
                s.add("ddd");
                s.add("eee");
                s.add("fff");
                binding.refreshView.finishRefresh(true);
                adapter.notifyItemRangeChanged(3,3);
            }
        }, 2000);
    }*/

    class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;

        public MyViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.text);
        }
    }

    class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {

        public MyAdapter() {
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MyViewHolder(LayoutInflater.from(MainActivity.this).inflate(R.layout.item, parent, false));
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            holder.textView.setText("name " + s.get(position));
        }

        @Override
        public int getItemCount() {
            return s.size();
        }
    }
}
