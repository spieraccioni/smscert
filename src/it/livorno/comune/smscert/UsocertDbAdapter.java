/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.livorno.comune.smscert;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
 

/**
 *
 * @author MPisu
 */
public class UsocertDbAdapter {
 
 public static final String KEY_ROWID = "_id";
 public static final String KEY_CODE = "code";
 public static final String KEY_USO = "uso";
 
 
 private static final String TAG = "UsocertDbAdapter";
 private DatabaseHelper mDbHelper;
 private SQLiteDatabase mDb;
 
 private static final String DATABASE_NAME = "livdemog";
 private static final String SQLITE_TABLE = "usocert";
 private static final int DATABASE_VERSION = 1;
 
 private final Context mCtx;
 
 private static final String DATABASE_CREATE =
  "CREATE TABLE if not exists " + SQLITE_TABLE + " (" +
  KEY_ROWID + " integer PRIMARY KEY autoincrement," +
  KEY_CODE + "," +
  KEY_USO + "," +
  " UNIQUE (" + KEY_CODE +"));";
 
 private static class DatabaseHelper extends SQLiteOpenHelper {
 
  DatabaseHelper(Context context) {
   super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }
 
 
  @Override
  public void onCreate(SQLiteDatabase db) {
   Log.w(TAG, DATABASE_CREATE);
   db.execSQL(DATABASE_CREATE);
  }
 
  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
   Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
     + newVersion + ", which will destroy all old data");
   db.execSQL("DROP TABLE IF EXISTS " + SQLITE_TABLE);
   onCreate(db);
  }
 }
 
 public UsocertDbAdapter(Context ctx) {
  this.mCtx = ctx;
 }
 
 public UsocertDbAdapter open() throws SQLException {
  mDbHelper = new DatabaseHelper(mCtx);
  mDb = mDbHelper.getWritableDatabase();
  return this;
 }
 
 public void close() {
  if (mDbHelper != null) {
   mDbHelper.close();
  }
 }
 
 public long createUso(String code, String uso) {
 
  ContentValues initialValues = new ContentValues();
  initialValues.put(KEY_CODE, code);
  initialValues.put(KEY_USO, uso);
  return mDb.insert(SQLITE_TABLE, null, initialValues);
  
 }
 
 public boolean deleteAllUsi() {
 
  int doneDelete = 0;
  doneDelete = mDb.delete(SQLITE_TABLE, null , null);
  Log.w(TAG, Integer.toString(doneDelete));
  return doneDelete > 0;
 
 }
 
 public Cursor fetchUsiByuso(String inputText) throws SQLException {
  Log.w(TAG, inputText);
  Cursor mCursor = null;
  if (inputText == null  ||  inputText.length () == 0)  {
   mCursor = mDb.query(SQLITE_TABLE, new String[] {KEY_ROWID,
     KEY_CODE, KEY_USO},
     null, null, null, null, null);
 
  }
  else {
   mCursor = mDb.query(true, SQLITE_TABLE, new String[] {KEY_ROWID,
     KEY_CODE, KEY_USO},
     KEY_USO + " like '%" + inputText + "%'", null,
     null, null, null, null);
  }
  if (mCursor != null) {
   mCursor.moveToFirst();
  }
  return mCursor;
 
 }
 
 public Cursor fetchAllUsi() {
 
  Cursor mCursor = mDb.query(SQLITE_TABLE, new String[] {KEY_ROWID,
    KEY_CODE, KEY_USO},
    null, null, null, null,null);
 
  if (mCursor != null) {
   mCursor.moveToFirst();
  }
  return mCursor;
 }
 
 public void insertUsi() {
 
  createUso("BOLL","Ad uso in BOLLO - (MARCA APPOSTA DAL CITTADINO)");
  createUso("18","Assegni familiari");    
  createUso("60","Abbonamento trasporto persone");
  createUso("22","Affidamento e adozione"); 
  createUso("47","Assicurazioni varie");
  createUso("46","Libretto di risparmio");
  createUso("53","Lavoro per matrimonio, nascita, morte");
  createUso("61","Certificato casellario giudiziale");
  createUso("57","Divorzio");
  createUso("41","Equo canone");
  createUso("65","Volontariato");
  createUso("38","Cooperative edilizie");
  createUso("66","patrocinio gratuito");
  createUso("50","Mutui per l'agricoltura");
  createUso("59","Procedimenti penali e disciplinari");
  createUso("90","Pubblicazioni matrimonio");
  createUso("44","Ricongiunzione carriera");
  createUso("13","Scuola dell'obbligo privata");
  createUso("54","Materne, nido, borse studio private");
  createUso("14","Scuole superiori serali private");
  createUso("56","Tutela minori interdetti");
  createUso("45","UMA prodotti petroliferi agricoltura");
  createUso("95","Promozione sportiva");
  createUso("9","Controversie in materia di lavoro");

 }
}
