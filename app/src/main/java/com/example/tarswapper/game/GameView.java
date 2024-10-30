package com.example.tarswapper.game;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.tarswapper.MainActivity;
import com.example.tarswapper.R;

import java.util.ArrayList;
import java.util.Random;

public class GameView extends View {

    //Bitmap objects for game images
    Bitmap ground, background, padoru;
    Rect rectGround, rectBackground;

    //Handler for periodically refreshing the screen
    Handler handler;
    Runnable runnable;
    Context context;

    //Time interval in milliseconds for screen refresh
    final long UPDATE_MILLIS = 30;

    //Paint objects for drawing text and health bar
    Paint textPaint = new Paint();
    Paint healthPaint = new Paint();

    //Variables for text size, points, life, and screen dimensions
    int TEXT_SIZE = 80;
    int points = 0;
    int life = 3;
    static int dWidth, dHeight;

    //Random object to generate random positions
    Random random;

    //Variables to track Padoru character's position
    float padoruX, padoruY;
    float oldX;         //To store the X-coordinate on touch down
    float oldPadoruX;   //To store Padoru’s initial position on touch down

    //List to manage bombs
    ArrayList<Bomb> bombs;

    //Vibrator
    Vibrator vibrator;

    //Play Background music
    MediaPlayer mediaPlayer;


    //Initialization and Settings
    public GameView(Context context) {
        super(context);
        this.context = context;

        //Prevent back navigation when this fragment is visible
        ((MainActivity) context).setBackPressPrevented(true);

        //Initialize bitmaps for the background, ground, and Padoru character
        background = BitmapFactory.decodeResource(getResources(), R.drawable.game_bg);
        ground = BitmapFactory.decodeResource(getResources(), R.drawable.game_ground);
        padoru = BitmapFactory.decodeResource(getResources(), R.drawable.padoru);

        //Set screen width and height using the Display object
        Display display = ((Activity) getContext()).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        dWidth = size.x;
        dHeight = size.y;

        //Define the size of the background and ground areas
        rectBackground = new Rect(0, 0, dWidth, dHeight);
        rectGround = new Rect(0, dHeight - ground.getHeight(), dWidth, dHeight);

        //Set up handler for refreshing the screen every 30 milliseconds
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                invalidate(); // Redraw the view
            }
        };

        //Initialize textPaint for displaying score
        textPaint.setColor(Color.rgb(255, 255, 255));
        textPaint.setTextSize(TEXT_SIZE);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTypeface(ResourcesCompat.getFont(context, R.font.poppins_bold));

        //Initialize healthPaint for drawing health bar
        healthPaint.setColor(Color.GREEN);
        random = new Random();

        //Set Padoru initial position
        padoruX = dWidth / 2 - padoru.getWidth() / 2;
        padoruY = dHeight - ground.getHeight() - padoru.getHeight();

        //Initialize list for bombs
        bombs = new ArrayList<>();

        //Add initial bombs
        for (int i = 0; i < 3; i++) {
            Bomb bomb = new Bomb(context);
            bombs.add(bomb);
        }

        //Add Vibrator
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        //Play Music
        playMusic(context);
    }


    //Collisions between bombs and Padoru are handled in onDraw(), and a health bar visualizes the remaining life.
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //Draw the background and ground
        canvas.drawBitmap(background, null, rectBackground, null);
        canvas.drawBitmap(ground, null, rectGround, null);

        //Draw the Padoru character
        canvas.drawBitmap(padoru, padoruX, padoruY, null);

        //Draw bombs and move them downwards
        for (int i = 0; i < bombs.size(); i++) {
            canvas.drawBitmap(bombs.get(i).getBomb(), bombs.get(i).bombX, bombs.get(i).bombY, null);
            //Move bomb downward based on velocity
            bombs.get(i).bombY += bombs.get(i).bombVelocity;

            //Check if bomb hits the ground
            if (bombs.get(i).bombY + bombs.get(i).getBombHeight() >= dHeight - ground.getHeight()) {
                points += 10;
                bombs.get(i).resetPosition();
            }
        }

        //Check for collisions between Padoru and bombs
        for (int i = 0; i < bombs.size(); i++) {
            if (bombs.get(i).bombX + bombs.get(i).getBombWidth() >= padoruX
                    && bombs.get(i).bombX <= padoruX + padoru.getWidth()
                    && bombs.get(i).bombY + bombs.get(i).getBombWidth() >= padoruY
                    && bombs.get(i).bombY + bombs.get(i).getBombWidth() <= padoruY + padoru.getHeight()) {

                //Reduce life and reset bomb position upon collision
                life--;
                bombs.get(i).resetPosition();

                //Vibrate when collision happen
                if (vibrator != null && vibrator.hasVibrator()) {
                    //Vibrate for 300 milliseconds
                    vibrator.vibrate(300);
                }

                //Game over condition
                if (life == 0) {
                    //Stop Music
                    stopMusic();

                    //Create a new instance of GameOverFragment
                    GameOver gameOverFragment = new GameOver();

                    //Create a Bundle to pass the points
                    Bundle bundle = new Bundle();
                    bundle.putInt("points", points);
                    gameOverFragment.setArguments(bundle);

                    //Begin the fragment transaction
                    FragmentManager fragmentManager = ((FragmentActivity) context).getSupportFragmentManager();
                    FragmentTransaction transaction = fragmentManager.beginTransaction();

                    //Replace the current fragment with the GameOverFragment
                    transaction.replace(R.id.frameLayout, gameOverFragment);
                    transaction.commit();

                    //Allow back navigation again when transitioning to GameOver
                    ((MainActivity) context).setBackPressPrevented(true);
                }
            }
        }

        //Draw the health bar based on life and the score
        canvas.drawRect(dWidth - 200, 30, dWidth - 200 + 60 * life, 80, healthPaint);
        canvas.drawText("Point: " + points, 20, TEXT_SIZE, textPaint);

        //Refresh the screen
        handler.postDelayed(runnable, UPDATE_MILLIS);

        //Change health bar display color
        if (life == 2) {
            healthPaint.setColor(Color.YELLOW);
        } else if (life == 1) {
            healthPaint.setColor(Color.RED);
        }
    }


    //Controls Padoru Movement by User
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        //Capture X and Y positions on touch
        float touchX = e.getX();
        float touchY = e.getY();

        //Allow Padoru movement only if touch is within its Y-axis
        if (touchY >= padoruY) {
            int action = e.getAction();

            //Store the initial touch position
            if (action == MotionEvent.ACTION_DOWN) {
                oldX = e.getX();
                oldPadoruX = padoruX;
            }

            //Calculate and update Padoru new X position
            if (action == MotionEvent.ACTION_MOVE) {
                float shift = oldX - touchX;
                float newPadoruX = oldPadoruX - shift;

                //Restrict Padoru movement within screen bounds
                if (newPadoruX <= 0) {
                    padoruX = 0;
                } else if (newPadoruX >= dWidth - padoru.getWidth()) {
                    padoruX = dWidth - padoru.getWidth();
                } else {
                    padoruX = newPadoruX;
                }
            }
        }

        //indicate that the touch event is handled
        return true;
    }



    //Initialize the MediaPlayer
    public void playMusic(Context context) {
        //Initialize MediaPlayer with an audio file from res/raw
        mediaPlayer = MediaPlayer.create(context, R.raw.game_music); // Ensure music.mp3 is in res/raw

        //Start playback
        mediaPlayer.start();

        //Set a listener to release resources when playback is complete
        mediaPlayer.setOnCompletionListener(mp -> releaseMediaPlayer());
    }

    //Stop and release resources
    public void stopMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            releaseMediaPlayer();
        }
    }

    //Release MediaPlayer resources
    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

}
