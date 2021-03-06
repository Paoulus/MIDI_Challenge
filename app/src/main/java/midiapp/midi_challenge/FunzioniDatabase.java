package midiapp.midi_challenge;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.support.design.widget.Snackbar;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Paolo on 12/01/2017.
 */

public class FunzioniDatabase {
    SQLiteDatabase database = null;
    DatabaseApp dbHelper = null;

    private Context internalContextDatabase = null;

    /*
        Database Schema:            idBrano INTEGER PRIMARY KEY, titolo VARCHAR NOT NULL,autore VARCHAR NOT NULL, nomeFile VARCHAR, arraySpartiti VARCHAR, difficoltà INTEGER DEFAULT -1 NOT NULL);";
        Utente(rowid,nickname,foto,punteggioMassimo,punteggioMedio)
        Brano(idBrano,titolo,autore, nomeFile, arraySpartitidifficoltà)
        relUtenteBrano(idUtente,idBrano)
     */

    public FunzioniDatabase(Context context) {
        dbHelper = new DatabaseApp(context);
        database = dbHelper.getWritableDatabase();
        internalContextDatabase = context;
    }

    //resetta il db. Usare con cautela.
    public boolean dropAllTables(){
        try {
            /*
            database.execSQL("DROP TABLE IF EXISTS utente");
            database.execSQL("DROP TABLE IF EXISTS brano");
            database.execSQL("DROP TABLE IF EXISTS relUtenteBrano");*/
            dbHelper.onUpgrade(database,0,0);
            return true;
        }
        catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }

    public long inserisci(Utente u){
        ContentValues cv = new ContentValues();
        cv.put("nickname",u.nickName);
        cv.put("foto",u.foto);
        cv.put("strumento",u.strumento);
        cv.put("punteggioMassimo",u.punteggioMassimo);
        cv.put("punteggioMedio",u.punteggioMedio);
        long newRowId = database.insert("utente","",cv);

        return newRowId;
    }

    public long inserisci(Brano b){
        ContentValues cv = new ContentValues();
        cv.put("titolo",b.getTitolo());
        cv.put("autore",b.autore);
        cv.put("nomeFile",b.getNomeFile());
        String help="";
        for (String x : b.arraySpartiti)
            help += x + ";";

        cv.put("arraySpartiti",help);
        cv.put("difficoltà",b.difficoltà);

        long result = database.insert("brano","",cv);

        return result;
    }


    public Utente trovaUtente(long id){
        String[] colums = {"idUtente","nickname","foto","strumento","punteggioMassimo","punteggioMedio"};
        Cursor res = database.query(true,"utente",colums,"idUtente = ?",new String[] {Long.toString(id)},"","","","");
        List<Brano> l = braniUtente(id);

        if (res.moveToFirst()){
            Utente tmp =  new Utente(res.getInt(0),res.getString(1),res.getString(2),res.getString(3),res.getInt(4),res.getInt(5));
            tmp.setBraniUtente(l);
            return tmp;
        }else{
            return  new Utente(-1,"","","",0,0);
        }
    }

    public Utente trovaUtente(String nome){
        String[] colums = {"idUtente","nickname","foto","strumento","punteggioMassimo","punteggioMedio"};
        Cursor res = database.query(true,"utente",colums,"nickname = ?",new String[] {nome},"","","","");

        if (res.moveToFirst()){
            return new Utente(res.getInt(0),res.getString(1),res.getString(2),res.getString(3),res.getInt(4),res.getInt(5));
        }else{
            return  new Utente(-1,"","","",0,0);
        }
    }

    public int aggiornaUtente(Utente u){
        ContentValues cv = new ContentValues();
        cv.put("nickname",u.nickName); //These Fields should be your String values of actual column names
        cv.put("foto",u.foto);
        cv.put("strumento",u.strumento);
        cv.put("punteggioMassimo",u.punteggioMassimo);
        if(u.punteggioMedio==0){
            if(braniUtente(u.idUtente).size()>0){
                for(Brano x :  braniUtente(u.idUtente))
                    u.punteggioMedio += x.difficoltà;
                u.punteggioMedio = u.punteggioMedio / braniUtente(u.idUtente).size();
            }
            else Log.d("UPDATE DB","aggiorna utente ha 0 dati");
        }
        else
            cv.put("punteggioMedio", u.punteggioMedio);
        Log.d("UPDATE DB",cv.toString());
        return database.update("utente", cv, "idUtente="+u.idUtente, null);
    }

    public int aggiornaBrano(Brano b){ // idBrano INTEGER PRIMARY KEY, titolo VARCHAR NOT NULL,autore VARCHAR, nomeFile VARCHAR, arraySpartiti VARCHAR, difficoltà INTEGER DEFAULT -1 NOT NULL
        ContentValues cv = new ContentValues(); //These Fields should be your String values of actual column names
        cv.put("titolo",b.getTitolo());
        cv.put("autore",b.autore);
        cv.put("nomeFile",b.getNomeFile());

        String help="";
        for(String x : b.arraySpartiti)
            help += x+";";

        cv.put("arraySpartiti",help);
        cv.put("difficoltà",b.getDifficoltà());

        Log.d("UPDATE DB",cv.toString());
        return database.update("brano", cv, "idBrano="+b.idBrano, null);
    }

    public Brano trovaBrano(long id){ // public Brano(long idBrano,String titolo,String autore, String nomeFile,ArrayList<String> arraySheets, int difficoltà)
        String[] colums = {"idBrano","titolo","autore","nomeFile","arraySpartiti","difficoltà"};
        Cursor res = database.query(true,"brano",colums,"idBrano = ?",new String[] {Long.toString(id)},"","","","");  //cotrollare che inserirsca tutti i campi!
        if (res.moveToFirst()){
            ArrayList<String> _arraySheets = new ArrayList<>(Arrays.asList(res.getString(4).split(";")));
            return new Brano(res.getInt(0),res.getString(1),res.getString(2),res.getString(3),_arraySheets,res.getInt(5));
        } else{
            return  new Brano(-1,"",0);
        }
    }

    public Brano trovaBrano(String titolo){  //DA AGGIORNARE!
        String[] colums = {"idBrano","titolo","nomeFile","difficoltà"};
        Cursor res = database.query(true,"brano",colums,"titolo = ?",new String[] {titolo},"","","","");

        if (res.moveToFirst()){
            return new Brano(res.getInt(0),res.getString(2),res.getInt(3));
        }else{
            return  null;
        }
    }

    public List<Brano> braniUtente(long idUtente){
        List<Brano> tmpList = new ArrayList<>();
        String selectionQuery = "SELECT DISTINCT Brano.idBrano,titolo,nomeFile,difficoltà,autovalutazione,autore,arraySpartiti FROM Brano JOIN relUtenteBrano ON Brano.idBrano = relUtenteBrano.idBrano WHERE relUtenteBrano.idUtente = ?";

        Cursor res = database.rawQuery(selectionQuery,new String[]{Long.toString(idUtente)});
        while(res.moveToNext()){
            ArrayList<String> _arraySheets = new ArrayList<>(Arrays.asList(res.getString(6).split(";")));
            tmpList.add(new Brano(res.getInt(0),res.getString(1),res.getString(2),res.getString(3),_arraySheets,res.getInt(5))); //controllare ordine inserimento campi!
        }

        return tmpList;
    }

    public List<Brano> braniUtente(String nickName){  //mai usato?
        List<Brano> tmpList = new ArrayList<>();
        String selectionQuery = "SELECT Brano.idBrano,titolo,nomeFile,difficoltà,autovalutazione,autore,arraySpartiti FROM Brano JOIN relUtenteBrano JOIN Utente WHERE nickname = ?";

        Cursor res = database.rawQuery(selectionQuery,new String[]{nickName});
        while(res.moveToNext()){
            ArrayList<String> _arraySheets = new ArrayList<>(Arrays.asList(res.getString(6).split(";")));
            tmpList.add(new Brano(res.getInt(0),res.getString(1),res.getString(2),res.getString(3),_arraySheets,res.getInt(5))); //controllare ordine inserimento campi!
        }

        return tmpList;
    }

    public long inserisciBranoPerUtente(Utente u, Brano b, int autovalutazione){

        if(trovaBrano(b.getTitolo()) == null){
            inserisci(b);
        }

        long idBranoDaInserire = trovaBrano(b.getTitolo()).getIdBrano();    //TODO: yep, it's quite ugly. Maybe we should fix this, later

        ContentValues cv = new ContentValues();
        cv.put("idUtente",u.idUtente);
        cv.put("idBrano",idBranoDaInserire);
        cv.put("autovalutazione",autovalutazione);

        return database.insert("relUtenteBrano","",cv);
    }

    public List<Utente> prendiTuttiUtenti() {
        List<Utente> tmpList = new ArrayList<>();

        String[] colums = {"idUtente","nickname","foto","strumento","punteggioMassimo","punteggioMedio"};
        Cursor res = database.query(true,"utente",colums,"",new String[] {},"","","","");

        while(res.moveToNext()){
            tmpList.add(new Utente(res.getInt(0),res.getString(1),res.getString(2),res.getString(3),res.getInt(4),res.getInt(5)));
        }

        return tmpList;
    }

    public List<Brano> prendiTuttiBrani(){
        List<Brano> tmpList = new ArrayList<>();

        String[] columns = {"idBrano","titolo","nomeFile","difficoltà"};
        Cursor res = database.query(true,"brano",columns,"",new String[] {},"","","","");

        while(res.moveToNext()){
            tmpList.add(new Brano(res.getInt(0),res.getString(2),res.getInt(3)));
        }
        return tmpList;
    }

    public long cancLinkBranoUtente(long idUtente, long idBrano){ //http://stackoverflow.com/questions/15027474/android-sqlite-deleting-a-specific-row-from-database
        String whereClause = "idUtente='"+idUtente +"' AND idBrano='" + idBrano+"'";
        return database.delete("relUtenteBrano",whereClause, null);
    }

    public long cancLinksTuttiBraniUtente(long idUtente){
        String whereClause = "idUtente='" + idUtente+"'";
        return database.delete("relUtenteBrano",whereClause, null);
    }

    public String getNomeDatabase(){
        return  dbHelper.getNOME_DB();
    }

}