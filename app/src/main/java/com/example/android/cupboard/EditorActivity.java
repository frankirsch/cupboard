package com.example.android.cupboard;

/**
 * Created by Franziska on 25/04/2017.
 */

/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.cupboard.data.PantryContract;
import com.example.android.cupboard.data.PantryContract.FoodEntry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Allows user to create a new food item or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, DatePickerDialog.OnDateSetListener {

    /** Identifier for the food data loader */
    private static final int EXISTING_FOOD_LOADER = 0;

    /** Content URI for the existing food (null if it's a new food) */
    private Uri mCurrentFoodUri;

    /** EditText field to enter the food's name */
    private EditText mNameEditText;

    /** EditText field to enter the food's use-by date */
    private EditText mUseByDateEditText;

    /** EditText field to enter amount of food */
    private EditText mAmountEditText;

    /** EditText field to select units for the food */
    private Spinner mUnitsSpinner;

    /**
     * Units of the amount of food. The possible valid values are in the PantryContract.java file:
     * {@link PantryContract.FoodEntry#UNITS_UNKNOWN}, {@link FoodEntry#UNITS_G},
     * {@link FoodEntry#UNITS_KG}, {@link FoodEntry#UNITS_ML}, or {@link FoodEntry#UNITS_L}.
     */
    private int mUnit = PantryContract.FoodEntry.UNITS_UNKNOWN;

    /** Boolean flag that keeps track of whether the food item has been edited (true) or not (false) */
    private boolean mFoodHasChanged = false;

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mFoodHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mFoodHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new food item or editing an existing one.
        Intent intent = getIntent();
        mCurrentFoodUri = intent.getData();

        // If the intent DOES NOT contain a food item content URI, then we know that we are
        // creating a new food.
        if (mCurrentFoodUri == null) {
            // This is a new food, so change the app bar to say "Add item"
            setTitle(getString(R.string.editor_activity_title_new_food));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a food item that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing food item, so change app bar to say "Edit item"
            setTitle(getString(R.string.editor_activity_title_edit_food));

            // Initialize a loader to read the food data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_FOOD_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_food_name);
        mUseByDateEditText = (EditText) findViewById(R.id.edit_use_by_date);
        mAmountEditText = (EditText) findViewById(R.id.edit_amount);
        mUnitsSpinner = (Spinner) findViewById(R.id.edit_amount_units);

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mNameEditText.setOnTouchListener(mTouchListener);
        mUseByDateEditText.setOnTouchListener(mTouchListener);
        mAmountEditText.setOnTouchListener(mTouchListener);
        mUnitsSpinner.setOnTouchListener(mTouchListener);

        setupSpinner();
    }

    /**
     * Setup the dropdown spinner that allows the user to select the units for the amount of food.
     */
    private void setupSpinner() {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter unitsSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_units_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        unitsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mUnitsSpinner.setAdapter(unitsSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mUnitsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.units_g))) {
                        mUnit = FoodEntry.UNITS_G;
                    } else if (selection.equals(getString(R.string.units_kg))) {
                        mUnit = FoodEntry.UNITS_KG;
                    } else if (selection.equals(getString(R.string.units_ml))) {
                        mUnit = FoodEntry.UNITS_ML;
                    } else if (selection.equals(getString(R.string.units_l))) {
                        mUnit = FoodEntry.UNITS_L;
                    } else {
                        mUnit = FoodEntry.UNITS_UNKNOWN;
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mUnit = FoodEntry.UNITS_UNKNOWN;
            }
        });
    }

    /**
     * Get user input from editor and save food item into database.
     */
    private void saveFood() throws ParseException {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String useByDateString = mUseByDateEditText.getText().toString().trim();
        String amountString = mAmountEditText.getText().toString().trim();

        // Check if this is supposed to be a new food
        // and check if all the fields in the editor are blank
        if (mCurrentFoodUri == null &&
                TextUtils.isEmpty(nameString) && TextUtils.isEmpty(useByDateString) &&
                TextUtils.isEmpty(amountString) && mUnit == FoodEntry.UNITS_UNKNOWN) {
            // Since no fields were modified, we can return early without creating a new food item.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            return;
        }

        // Create a ContentValues object where column names are the keys,
        // and food attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(FoodEntry.COLUMN_FOOD_NAME, nameString);

        // Convert use-by date from dd/mm/yy format to yyyy-mm-dd format
        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.ENGLISH);

        Date inputDate = dateFormat.parse(useByDateString);

        final SimpleDateFormat dataDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

        String outputDate = dataDateFormat.format(inputDate);

        values.put(PantryContract.FoodEntry.COLUMN_USE_BY_DATE, outputDate);

        int amount = 0;
        if (!TextUtils.isEmpty(amountString)) {
            amount = Integer.parseInt(amountString);
        }

        values.put(FoodEntry.COLUMN_FOOD_AMOUNT, amount);

        values.put(FoodEntry.COLUMN_FOOD_AMOUNT_UNIT, mUnit);

        // Determine if this is a new or existing food item by checking if mCurrentFoodUri is null or not
        if (mCurrentFoodUri == null) {
            // This is a NEW food item, so insert a new food into the provider,
            // returning the content URI for the new food item.
            Uri newUri = getContentResolver().insert(FoodEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_food_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_food_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Otherwise this is an existing food item, so update the food with content URI: mCurrentFoodUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentFoodUri will already identify the correct row in the database that
            // we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentFoodUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_food_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_food_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new food item, hide the "Delete" menu item.
        if (mCurrentFoodUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save food item to database
                try {
                    saveFood();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                // Exit activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the food item hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mFoodHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the food item hasn't changed, continue with handling back button press
        if (!mFoodHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all food attributes, define a projection that contains
        // all columns from the pantry table
        String[] projection = {
                FoodEntry._ID,
                PantryContract.FoodEntry.COLUMN_FOOD_NAME,
                PantryContract.FoodEntry.COLUMN_USE_BY_DATE,
                PantryContract.FoodEntry.COLUMN_FOOD_AMOUNT,
                PantryContract.FoodEntry.COLUMN_FOOD_AMOUNT_UNIT};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentFoodUri,         // Query the content URI for the current food item
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of food attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(FoodEntry.COLUMN_FOOD_NAME);
            int useByDateColumnIndex = cursor.getColumnIndex(FoodEntry.COLUMN_USE_BY_DATE);
            int amountColumnIndex = cursor.getColumnIndex(FoodEntry.COLUMN_FOOD_AMOUNT);
            int unitColumnIndex = cursor.getColumnIndex(FoodEntry.COLUMN_FOOD_AMOUNT_UNIT);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            String useByDateUnformatted = cursor.getString(useByDateColumnIndex);

            // Convert use-by date from yyyy-MM-dd format to dd/MM/yy
            final SimpleDateFormat dataDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.ENGLISH);
            String useByDate = null;
            try {
                Date dataDate = dataDateFormat.parse(useByDateUnformatted);
                useByDate = dateFormat.format(dataDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            int units = cursor.getInt(unitColumnIndex);
            int amount = cursor.getInt(amountColumnIndex);

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mUseByDateEditText.setText(useByDate);
            mAmountEditText.setText(Integer.toString(amount));

            // Gender is a dropdown spinner, so map the constant value from the database
            // into one of the dropdown options (0 is Unknown, 1 is grams, 2 is kg, 3 is ml, 4 is l).
            // Then call setSelection() so that option is displayed on screen as the current selection.
            switch (units) {
                case FoodEntry.UNITS_G:
                    mUnitsSpinner.setSelection(1);
                    break;
                case FoodEntry.UNITS_KG:
                    mUnitsSpinner.setSelection(2);
                    break;
                case FoodEntry.UNITS_ML:
                    mUnitsSpinner.setSelection(3);
                    break;
                case FoodEntry.UNITS_L:
                    mUnitsSpinner.setSelection(4);
                    break;
                default:
                    mUnitsSpinner.setSelection(0);
                    break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mUseByDateEditText.setText("");
        mAmountEditText.setText("");
        mUnitsSpinner.setSelection(0); // Select "Unknown" unit
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the food item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Prompt the user to confirm that they want to delete this food.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the food item.
                deleteFood();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the food item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the food item in the database.
     */
    private void deleteFood() {
        // Only perform the delete if this is an existing food item.
        if (mCurrentFoodUri != null) {
            // Call the ContentResolver to delete the food item at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentFoodUri
            // content URI already identifies the food item that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentFoodUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_food_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_food_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        finish();
    }

    /**
     * This callback method, call DatePickerFragment class,
     * DatePickerFragment class returns calendar view.
     * @param view
     */
    public void datePicker(View view){

        DatePickerFragment fragment = new DatePickerFragment();
        fragment.show(getSupportFragmentManager(), "date");

    }


    /**
     * To set date on TextView
     * @param calendar
     */
    private void setDate(final Calendar calendar) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.ENGLISH);
        ((TextView) findViewById(R.id.edit_use_by_date)).setText(dateFormat.format(calendar.getTime()));

    }

    /**
     * To receive a callback when the user sets the date.
     * @param view
     * @param year
     * @param month
     * @param day
     */
    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        Calendar cal = new GregorianCalendar(year, month, day);
        setDate(cal);
    }

    /**
     * Create a DatePickerFragment class that extends DialogFragment.
     * Define the onCreateDialog() method to return an instance of DatePickerDialog
     */
    public static class DatePickerFragment extends DialogFragment {


        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);


            DatePickerDialog dpd = new DatePickerDialog(getActivity(),(DatePickerDialog.OnDateSetListener)
                    getActivity(), year, month, day);

            //Get the DatePicker instance from DatePickerDialog
            DatePicker dp = dpd.getDatePicker();
            //Set the DatePicker minimum date selection to current date
            dp.setMinDate(c.getTimeInMillis());//get the current day

            return dpd;
        }

    }
}