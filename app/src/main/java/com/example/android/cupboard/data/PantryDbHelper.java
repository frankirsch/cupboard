package com.example.android.cupboard.data;

import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.content.Context;
import android.util.Log;

import com.example.android.cupboard.data.PantryContract.FoodEntry;

/**
 * Created by Franziska on 20/03/2017.
 */

public class PantryDbHelper extends SQLiteOpenHelper {
    public static final String LOG_TAG = PantryDbHelper.class.getSimpleName();

    /* name of database file */
    private static final String DATABASE_NAME = "pantry.db";

    /* db version */
    private static final int DATABASE_VERSION = 1;

    /* constructs a new instance of {@link PantryDbHelper}
    * @param context of the app
     */
    public PantryDbHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /* first creation of db */
    @Override
    public void onCreate(SQLiteDatabase db){
        String SQL_CREATE_PETS_TABLE = "CREATE TABLE " + PantryContract.FoodEntry.TABLE_NAME + " ("
                + FoodEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + FoodEntry.COLUMN_FOOD_NAME + " TEXT NOT NULL, "
                + FoodEntry.COLUMN_USE_BY_DATE + " TEXT, "
                + FoodEntry.COLUMN_FOOD_AMOUNT + " INTEGER NOT NULL DEFAULT 0, "
                + FoodEntry.COLUMN_FOOD_AMOUNT_UNIT + " INTEGER NOT NULL DEFAULT 0);";
        Log.v(LOG_TAG,SQL_CREATE_PETS_TABLE);
        db.execSQL(SQL_CREATE_PETS_TABLE);
    }



    /* when db needs to be upgraded */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        // nothing yet
    }
}
