package com.example.android.cupboard;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.cupboard.data.PantryContract;
import com.example.android.cupboard.data.PantryContract.FoodEntry;

import static com.example.android.cupboard.data.PantryContract.FoodEntry.COLUMN_FOOD_AMOUNT;
import static com.example.android.cupboard.data.PantryContract.FoodEntry.COLUMN_USE_BY_DATE;
import static com.example.android.cupboard.data.PantryContract.FoodEntry._ID;

/**
 * Displays list of food items stored in the app.
 */
public class PantryActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /** Identifier for food data loader */
    private static final int FOOD_LOADER = 0;

    /** Adapter for the ListView */
    PantryCursorAdapter mCursorAdapter;

    /** SwipeRefreshLayout allows user to update use-by dates by swiping down **/
    SwipeRefreshLayout swipeContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantry);

        // Lookup the swipe container view
        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);

        // Find the ListView which will be populated with the food data
        final ListView foodListView = (ListView) findViewById(R.id.list);

        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                                                @Override
                                                public void onRefresh() {
                                                    // Refresh foodListView with new dates/times
                                                    foodListView.setAdapter(mCursorAdapter);
                                                    // Now call setRefreshing(false) to signal
                                                    // refresh has finished
                                                    swipeContainer.setRefreshing(false);
                                                }
                                            });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(R.color.colorPrimary,
                R.color.colorAccent);


        // Set up FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PantryActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        foodListView.setEmptyView(emptyView);

        // Set up an Adapter to create a list item for each row of food data in the Cursor.
        // There is no food data yet (until the loader finishes) so pass in null for the Cursor.
        mCursorAdapter = new PantryCursorAdapter(this, null);
        foodListView.setAdapter(mCursorAdapter);

        // Setup item click listener
        foodListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                // Create new intent to go to the {@link EditorActivity}
                Intent intent = new Intent(PantryActivity.this, EditorActivity.class);

                // Form the content URI that represents the specific food item that was clicked on
                // by appending 'id' (passed as input in this method) onto {@link FoodEntry.CONTENT_URI}
                Uri currentFoodUri = ContentUris.withAppendedId(PantryContract.FoodEntry.CONTENT_URI, id);

                // Set URI on the data field of the intent
                intent.setData(currentFoodUri);

                // Launch the {@link EditorActivity} to display data for the current food item
                startActivity(intent);


            }
        });

        // Kick off the loader
        getLoaderManager().initLoader(FOOD_LOADER, null, this);
    }

    /**
     * Helper method to insert hardcoded pet data into the database. For debugging purposes only.
     */
    private void insertFood() {
        // Create a ContentValues object where column names are the keys and
        // sample Nandos details are attributes
        ContentValues values = new ContentValues();
        values.put(FoodEntry.COLUMN_FOOD_NAME, "Nandos");
        values.put(COLUMN_USE_BY_DATE, "20/04/15");
        values.put(FoodEntry.COLUMN_FOOD_AMOUNT, 100);
        values.put(FoodEntry.COLUMN_FOOD_AMOUNT_UNIT, FoodEntry.UNITS_G);

        // Insert a new row for Nandos into the provider using the ContentResolver.
        // Use the {@link FoodEntry#CONTENT_URI} to indicate that we want to insert
        // into the pets database table.
        // Receive the new content URI that will allow us to access the Nandos data in the future.
        Uri newUri = getContentResolver().insert(FoodEntry.CONTENT_URI, values);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Open settings menu
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertFood();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                showDeleteConfirmationDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Helper method to delete all pets in the database.
     */
    private void deleteAllFood() {
        int rowsDeleted = getContentResolver().delete(FoodEntry.CONTENT_URI, null, null);
        Log.v("PantryActivity", rowsDeleted + " rows deleted from food database");
    }

    /**
     * Prompt the user to confirm that they want to delete this pet.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
                deleteAllFood();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Get preferences for ordering food items
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String orderBy = sharedPrefs.getString(
                getString(R.string.settings_order_by_key),
                getString(R.string.settings_order_by_default)
        );

        String orderParm = _ID + " DESC";
        String orderByDate = getString(R.string.settings_order_by_most_recent_value);

        if (orderBy.equals(orderByDate)){
            orderParm = "date(" + COLUMN_USE_BY_DATE + ") ASC";
        }


        // Define a projection that specifies the columns from the table we care about.
        String[] projection = {
                PantryContract.FoodEntry._ID,
                PantryContract.FoodEntry.COLUMN_FOOD_NAME,
                COLUMN_USE_BY_DATE,
                PantryContract.FoodEntry.COLUMN_FOOD_AMOUNT,
                PantryContract.FoodEntry.COLUMN_FOOD_AMOUNT_UNIT};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                FoodEntry.CONTENT_URI,   // Provider content URI to query
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                orderParm);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update {@link PantryCursorAdapter} with this new cursor containing updated pet data
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Callback called when the data needs to be deleted
        mCursorAdapter.swapCursor(null);
    }


}

