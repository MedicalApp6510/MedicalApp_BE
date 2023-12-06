package edu.northeastern.cs5500project;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import edu.northeastern.cs5500project.User_Auth.Login;

public class MainActivity extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_front_page);

        Intent intent = new Intent(MainActivity.this, final_front_page.class);
        startActivity(intent);
    }
}