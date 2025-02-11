package com.example.tarswapper.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.example.tarswapper.R;

import java.util.Random;

public class Bomb {
    //Single bitmap for bomb image
    Bitmap bomb;

    //Position and speed of the bomb
    int bombX, bombY, bombVelocity;
    Random random;

    public Bomb(Context context){
        //Load bomb frame into the bitmap array
        bomb = BitmapFactory.decodeResource(context.getResources(), R.drawable.bomb);

        //Initialize random generator and reset bomb position
        random = new Random();
        resetPosition();
    }

    //Resets bomb's position to a new random location at the top of the screen
    public void resetPosition() {
        //Ensure bombX is within screen width bounds
        int width = GameView.dWidth - getBombWidth();
        bombX = width > 0 ? random.nextInt(width) : 0;

        //Set bombY to start off-screen above the visible area
        bombY = -200 + random.nextInt(600) * -1;

        //Randomize bomb falling speed between 35 and 50
        bombVelocity = 35 + random.nextInt(16);
    }

    //Retrieves the current animation frame for the bomb
    public Bitmap getBomb(){
        return bomb;
    }

    //Gets the width of the bomb
    public int getBombWidth(){
        return bomb.getWidth();
    }

    //Gets the height of the bomb
    public int getBombHeight(){
        return bomb.getHeight();
    }

}
