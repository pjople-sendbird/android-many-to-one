package com.example.manytoonetest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button butLoginAsUser = (Button) findViewById(R.id.butLoginAsUser);
        butLoginAsUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startChatActivity(1);
            }
        });

        Button butLoginAsRepresentative = (Button) findViewById(R.id.butLoginAsRepresentative);
        butLoginAsRepresentative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startChatActivity(2);
            }
        });
    }

    private void startChatActivity(int role) {
        EditText editUserId = (EditText) findViewById(R.id.editUserId);
        if (role == 1) {
            if (editUserId.getText().toString().length() == 0) {
                return;
            }
        }
        Intent i = new Intent(this, ChatActivity.class);
        i.putExtra("user_id", editUserId.getText().toString().trim());
        i.putExtra("role", role);
        startActivity(i);
    }

}