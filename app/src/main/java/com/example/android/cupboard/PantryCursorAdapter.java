package com.example.android.cupboard;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.android.cupboard.data.PantryContract;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.example.android.cupboard.data.PantryContract.FoodEntry.UNITS_G;
import static com.example.android.cupboard.data.PantryContract.FoodEntry.UNITS_KG;
import static com.example.android.cupboard.data.PantryContract.FoodEntry.UNITS_L;
import static com.example.android.cupboard.data.PantryContract.FoodEntry.UNITS_ML;

/**
 * {@link PantryCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of food data as its data source. This adapter knows
 * how to create list items for each row of food data in the {@link Cursor}.
 */
public class PantryCursorAdapter extends CursorAdapter {

    public static final String LOG_TAG = PantryCursorAdapter.class.getSimpleName();
    /**
     * Constructs a new {@link PantryCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public PantryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the food data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current food item can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Find individual views that we want to modify in the list item layout
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView summaryTextView = (TextView) view.findViewById(R.id.summary);
        TextView amountTextView = (TextView) view.findViewById(R.id.amount);

        // Find the columns of food attributes that we're interested in
        int nameColumnIndex = cursor.getColumnIndex(PantryContract.FoodEntry.COLUMN_FOOD_NAME);
        int useByDateColumnIndex = cursor.getColumnIndex(PantryContract.FoodEntry.COLUMN_USE_BY_DATE);
        int amountColumnIndex = cursor.getColumnIndex(PantryContract.FoodEntry.COLUMN_FOOD_AMOUNT);
        int unitColumnIndex = cursor.getColumnIndex(PantryContract.FoodEntry.COLUMN_FOOD_AMOUNT_UNIT);

        // Read the food attributes from the Cursor for the current food item
        String foodName = cursor.getString(nameColumnIndex);
        String foodUseByDateUnformatted = cursor.getString(useByDateColumnIndex);

        // Convert use-by date from yyyy-MM-dd format to dd/MM/yy
        final SimpleDateFormat dataDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.ENGLISH);
        String foodUseByDate = null;
        try {
            Date dataDate = dataDateFormat.parse(foodUseByDateUnformatted);
            foodUseByDate = dateFormat.format(dataDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Integer foodAmount = cursor.getInt(amountColumnIndex);
        Integer foodUnit = cursor.getInt(unitColumnIndex);

        // get today's date in as a Calendar object for comparing to use-by date
        final SimpleDateFormat useByDateFormat = new SimpleDateFormat("dd/MM/yy", Locale.ENGLISH);




        // If there is no use-by date, add some text so the TextView isn't empty
        if (TextUtils.isEmpty(foodUseByDate)) {
            foodUseByDate = context.getString(R.string.no_use_by_date);
        } else {
            // Else check how many days until expiry
                Date date = new Date();
                String todaysDate = useByDateFormat.format(date);
                int daysDiff_int =  get_count_of_days(todaysDate, foodUseByDate);

                Log.e(LOG_TAG, "days diff " + Integer.toString(daysDiff_int));

                // 4 potential cases for use-by date
                // In the past: food has gone off
                // Date colour is dark red
                // Text says something like "Use-by date x days ago"
                if (daysDiff_int<0) {
                    foodUseByDate = Integer.toString(-1*daysDiff_int) + " " + context.getString(R.string.use_by_date_past);
                    summaryTextView.setTextColor(ContextCompat.getColor(context, R.color.useByDateDarkRed));
                }

                // 0-1 days in the future: food about to go off
                // Date colour is red
                // Text says "today" or "tomorrow"
                else if (daysDiff_int == 0){
                    foodUseByDate = context.getString(R.string.use_by_date_today);
                    summaryTextView.setTextColor(ContextCompat.getColor(context, R.color.useByDateRed));
                }
                else if (daysDiff_int == 1){
                    foodUseByDate = context.getString(R.string.use_by_date_tomorrow);
                    summaryTextView.setTextColor(ContextCompat.getColor(context, R.color.useByDateRed));
                }

                // >1 days but <1 week in the future
                // Date colour is amber
                // Leave text as date
                else if (daysDiff_int>1 && daysDiff_int<8){
                    summaryTextView.setTextColor(ContextCompat.getColor(context, R.color.useByDateAmber));
                }

                // >1 week in the future: no need to worry
                // Date colour grey
                // Leave text as date

        }


        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // set text for unit
        String unitString;
        switch(foodUnit){
            case UNITS_G:
                unitString = "g";
                break;
            case UNITS_KG:
                    unitString = "kg";
                    break;
            case UNITS_ML:
                    unitString = "ml";
                    break;
            case UNITS_L:
                    unitString = "l";
                    break;
             default:
                 unitString = "";
                 break;

        }

        // Update the TextViews with the attributes for the current food item
        nameTextView.setText(foodName);
        summaryTextView.setText(foodUseByDate);
        amountTextView.setText(Integer.toString(foodAmount) + unitString);

    }

    // Helper method to calculate time between 2 days
    public Integer get_count_of_days(String Created_date_String, String Expire_date_String) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());

        Date Created_convertedDate = null, Expire_CovertedDate = null, todayWithZeroTime = null;
        try {
            Created_convertedDate = dateFormat.parse(Created_date_String);
            Expire_CovertedDate = dateFormat.parse(Expire_date_String);

            Date today = new Date();

            todayWithZeroTime = dateFormat.parse(dateFormat.format(today));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Calendar c_cal = Calendar.getInstance();
        c_cal.setTime(todayWithZeroTime);
        int c_year = c_cal.get(Calendar.YEAR);
        int c_month = c_cal.get(Calendar.MONTH);
        int c_day = c_cal.get(Calendar.DAY_OF_MONTH);


        Calendar e_cal = Calendar.getInstance();
        e_cal.setTime(Expire_CovertedDate);
        int e_year = e_cal.get(Calendar.YEAR);
        int e_month = e_cal.get(Calendar.MONTH);
        int e_day = e_cal.get(Calendar.DAY_OF_MONTH);

        Calendar date1 = Calendar.getInstance();
        Calendar date2 = Calendar.getInstance();

        date1.clear();
        date1.set(c_year, c_month, c_day);
        date2.clear();
        date2.set(e_year, e_month, e_day);

        long diff = date2.getTimeInMillis() - date1.getTimeInMillis();

        long dayCount = diff / (24 * 60 * 60 * 1000);

        return (int) dayCount;
    }

}