/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.livorno.comune.smscert;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
//import mp.app1.R;

/**
 *
 * @author MPisu
 */
public class ListaUsi extends Activity {

    /**
     * Called when the activity is first created.
     */
    private UsocertDbAdapter dbHelper;
    private SimpleCursorAdapter dataAdapter;
    Intent resultIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lista_usi);

        dbHelper = new UsocertDbAdapter(this);
        dbHelper.open();

        //Clean all data
        dbHelper.deleteAllUsi();
        //Add some data
        dbHelper.insertUsi();

        //Generate ListView from SQLite Database
        displayListView();

    }

    private void displayListView() {


        Cursor cursor = dbHelper.fetchAllUsi();

        // The desired columns to be bound
        String[] columns = new String[]{
            UsocertDbAdapter.KEY_CODE,
            UsocertDbAdapter.KEY_USO};

        // the XML defined views which the data will be bound to
        int[] to = new int[]{
            R.id.code,
            R.id.uso
        };

        // create the adapter using the cursor pointing to the desired data
        //as well as the layout information
        dataAdapter = new SimpleCursorAdapter(
                this, R.layout.usi_info,
                cursor,
                columns,
                to,
                0);

        ListView lv = (ListView) findViewById(R.id.listView1);
        // Assign adapter to ListView
        lv.setAdapter(dataAdapter);


        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> listView, View view,
                    int position, long id) {
                // Get the cursor, positioned to the corresponding row in the result set
                Cursor cursor = (Cursor) listView.getItemAtPosition(position);

                // Get the state's capital from this row in the database.
                String uso =
                        cursor.getString(cursor.getColumnIndexOrThrow("uso"));
                String code =
                        cursor.getString(cursor.getColumnIndexOrThrow("code"));
                //Toast.makeText(getApplicationContext(),uso, Toast.LENGTH_SHORT).show();
                resultIntent = getIntent();
                resultIntent.putExtra("USO", uso);
                resultIntent.putExtra("CODICE", code);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();

            }
        });

        EditText myFilter = (EditText) findViewById(R.id.myFilter);
        myFilter.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start,
                    int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                    int before, int count) {
                dataAdapter.getFilter().filter(s.toString());
            }
        });

        dataAdapter.setFilterQueryProvider(new FilterQueryProvider() {

            public Cursor runQuery(CharSequence constraint) {
                return dbHelper.fetchUsiByuso(constraint.toString());
            }
        });

    }
}
