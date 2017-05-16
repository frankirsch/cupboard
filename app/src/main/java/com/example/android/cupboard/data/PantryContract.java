package com.example.android.cupboard.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;


/**
 * API Contract for Cupboard app
 */
public final class PantryContract {

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private PantryContract() {}

    /**
     * Content authority: use package name for app.
     */
    public static final String CONTENT_AUTHORITY = "com.example.android.cupboard";

    /**
     * Create base for all URIs to contact the app using {@link #CONTENT_AUTHORITY}
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * Possible path, accesses pantry db.
     */
    public static final String PATH_FOOD = "pantry";


    /**
     * Inner class defines constants for food table. Each entry is one food item.
     */
    public static final class FoodEntry implements BaseColumns {

        /** The content URI to access the food data in the provider */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_FOOD);

        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of food items
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_FOOD;

        /**
         * The MIME type of the {@link #CONTENT_URI} for a single food item
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_FOOD;


        /** Name of database table for food items */
        public final static String TABLE_NAME = "pantry";

        /**
         * Unique ID number for the food (only for use in the database table).
         *
         * Type: INTEGER
         */
        public final static String _ID = BaseColumns._ID;

        /**
         * Name of the food item.
         *
         * Type: TEXT
         */
        public final static String COLUMN_FOOD_NAME ="name";

        /**
         * Use-by date of the food item.
         *
         * Type: INT
         */
        public final static String COLUMN_USE_BY_DATE = "usebydate";

        /**
         * Amount of food item
         *
         * Type: INTEGER
         */
        public final static String COLUMN_FOOD_AMOUNT = "amount";

        /**
         * Units for the amount of food.
         *
         * The only possible values are {@link #UNITS_UNKNOWN}, {@link #UNITS_G},
         * {@link #UNITS_KG}, {@link #UNITS_ML}, or {@link #UNITS_L}.
         *
         * Type: INTEGER
         */
        public final static String COLUMN_FOOD_AMOUNT_UNIT = "units";

        /**
         * Possible values for the units of amount of food
         */
        public static final int UNITS_UNKNOWN = 0;
        public static final int UNITS_G = 1;
        public static final int UNITS_KG = 2;
        public static final int UNITS_ML = 3;
        public static final int UNITS_L = 4;



        /**
         * Returns whether or not the given unit is valid.
         */
        public static boolean isValidUnit(int unit) {
            if (unit == UNITS_UNKNOWN || unit == UNITS_G || unit == UNITS_KG || unit == UNITS_ML || unit == UNITS_L) {
                return true;
            }
            return false;
        }

        // TODO: check date is valid
    }

}