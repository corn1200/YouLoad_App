package com.example.intentfilter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

public class FirstFragment extends Fragment {
    // Store instance variables
    private int img;

    // newInstance constructor for creating fragment with arguments
    public static FirstFragment newInstance(int img) {
        FirstFragment fragment = new FirstFragment();
        Bundle args = new Bundle();
        args.putInt("someInt", img);
        fragment.setArguments(args);
        return fragment;
    }

    // Store instance variables based on arguments passed
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        img = getArguments().getInt("someInt", 0);
    }

    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_1, container, false);
//        EditText tvLabel = view.findViewById(R.id.editText);
//        tvLabel.setText(img + " -- ");
        return view;
    }
}
