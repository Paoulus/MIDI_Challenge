package midiapp.midi_challenge;

import android.util.Log;

import com.pdrogfer.mididroid.MidiFile;
import com.pdrogfer.mididroid.MidiTrack;
import com.pdrogfer.mididroid.event.MidiEvent;
import com.pdrogfer.mididroid.event.NoteOn;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by lucabazzanella on 18/12/16.
 * //ogni nota letta aggunge un punteggio da aggiungere a quello totale
 *
 * “La musica è una pratica occulta dell’aritmetica, nella quale l’anima non sa di calcolare” (Gottfried Wilhelm von Leibniz)
 *
 //punteggio = velocita * armonia * timeSignature (
 //velocita = deltaTick^3 //più la velocità è alta più il punteggio aumenta -> Esponenzialmente
 //armonia = sommatoria( nota[]) //quante note sono suonate contemporaneanemnte
 //nota = intervallo (se diatonico) oppure intervallo*2 (se non diatonico)
 //                                                     per fare questo mi avvalgo di un vettore di note precendenti per trovare la tonalità corrente
 //                                                     ATTENZIONE a cambio direzione
 //Time signature = [4/4 , 2/2 , 7/8 ...] ogniuno di questo inserisce un moltiplicatore di difficolta
 */

public class AlgoritmoMidi {
    MidiTrack midiTrack;
    private  int nTraccia;
    private static ArrayList <NoteOn> ln = new ArrayList<NoteOn>();  // la lista sarebbe meglio se dinamica in base al delta time
    private static int sizeOfLN=30;
    //static int [] tonalita = {1,3,5,6,8,10,11};                      // scala maggiore %12
    static int [] contNoteMod12 = new int[11];
    private static long punteggio = 0;
    int contatorenNoteTotale=0;
    int contatoreAppoggiature =0;
    int contatoreAccordi =0;
    int contatoreEventsNotNote = 0;
    long bestPuntTemp = 0;
    long worstPuntTemp = 0;

    AlgoritmoMidi(MidiFile x, int nTracca){
        resetValori();
        midiTrack = x.getTracks().get(nTracca);
        this.nTraccia = nTracca;
    } //seleziona la traccia 0 del file midi, è quella contentente la traccia da analizzare

    public ArrayList<String> calcolaAlgoritmo(){
        resetValori();
        Iterator<MidiEvent> it = midiTrack.getEvents().iterator();
        ArrayList<String> outPut = new ArrayList<>();
        long puntTemp = 0;
        while(it.hasNext()) {   //per ogni nota nella traccia
            MidiEvent E = it.next();
            if(E.getClass().equals(NoteOn.class) &&   ((NoteOn)E).getVelocity()!=0 ) {  // per scremare le note "fantasma" con dinamica 0
                NoteOn EveNota = (NoteOn)E;                                             //int dinamica = EveNota.getVelocity();   //useless?
                int nota = EveNota.getNoteValue();
                long when = EveNota.getTick();
                long durata = EveNota.getDelta();

                if(nota<12  || nota >108) { //troppo alta o bassa non conteggiabile
                    contatoreEventsNotNote++;
                    continue;
                }
                if(ln.size()>sizeOfLN) ln.clear();      //ogni X note viene resettato il contatore, deve (in più) cancellare dimanicaente in base a nota.tick
                ln.add(EveNota);                        //aggiungo la nota nel vettore temporaneo
                contatorenNoteTotale++;
                contNoteMod12[EveNota.getNoteValue()%11]++;

                //Log.println(Log.ASSERT," Algoritmo midi","Nota: "+nota+" \t- "+convIntStrNota(EveNota.getNoteValue())+ " \t\t tick: "+EveNota.getTick()+" \t\t delta: "+EveNota.getDelta()); //DEBUG

                puntTemp +=  punteggioVelocita() * punteggioMelArm();
                if(puntTemp>bestPuntTemp)  {
                    bestPuntTemp = puntTemp;
                    if(!outPut.contains("Fraseggio difficile a: " + when/1000+" sec.")) { //se non è gia presente
                        outPut.add("Fraseggio difficile a: " + when / 1000 + " sec.");
                        worstPuntTemp = 0;
                    }
                }
                if( (puntTemp*2) < worstPuntTemp){      //un contatore di punteggio "basso" evita che il bestPuntTempo sia per forza crescente
                    bestPuntTemp = Math.round(puntTemp/2);
                }
                int pma = punteggioMelArm();
                double pv = punteggioVelocita();

                punteggio +=  pv * pma;
                Log.println(Log.ASSERT," Algoritmo midi","Punt Vel: "+pv+ " \t\t\t Punt MelArm: "+pma+" \t\t\t totale: "+ punteggio); //DEBUG
                puntTemp = 0;
            }
            else contatoreEventsNotNote++;
        }

        punteggio *= calcolaBiasTonalità();
        punteggio /= 1000*1000;            //miniaturizzazione

        Log.println(Log.ASSERT,"Algoritmo midi","==================================");
        Log.println(Log.ASSERT,"Algoritmo midi","==========  Conclusioni   ========");
        Log.println(Log.ASSERT,"Algoritmo midi","==================================");
        Log.println(Log.ASSERT,"Algoritmo midi","punteggio pre raffinazione: "+punteggio);
        Log.println(Log.ASSERT,"Algoritmo midi","Bias tonalita: "+calcolaBiasTonalità());
        if(contatoreEventsNotNote > midiTrack.getEventCount()/4){
            String msg = "Alto numero di eventi != note: "+contatoreEventsNotNote + " su eventi totali:"+ midiTrack.getEventCount();
            Log.println(Log.ASSERT,"Algoritmo midi",msg);
            outPut.add(msg);
        }
        //outPut.add("Punteggio totale realizzato: \t"+ Long.toString(punteggio));
        Log.println(Log.ASSERT,"Algoritmo midi","numero di note in totale: "+contatorenNoteTotale);
        Log.println(Log.ASSERT,"Algoritmo midi","numero di appoggiature in totale: "+contatoreAppoggiature);
        Log.println(Log.ASSERT,"Algoritmo midi","numero di accordi in totale: "+contatoreAccordi);
        Log.println(Log.ASSERT,"Algoritmo midi","Punteggio totale realizzato: 	"+ Long.toString(punteggio));
        return outPut;
    }

    String convIntStrNota(int i){
        String[] note = {"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};
        String result = note[(i)%12];
        String octave = Integer.toString((i/12)-2);
        return result+octave;
    }

    /**
     * calcola la dispersione di note rispetto alla tonalità -> applicato in post su tutta la traccia -> più c'è dispersione più è difficile
     * aggiunge 0.08 points per ogni nota del vettore contNoteMod12 che è conteggiata più di un undicesimo del totale, cioè che raggiunge un quantitativo minimo per
     * essere parte della tonalità di impianto (circa 7 note, ma possibile anche 6 o 8) ->diatonica
     * @return punteggio dispersione    output aspettato:
         *                              0.09*6 ->pentatonica allargata
         *                              0.09*7 ->tonalità/modi "classiche" basate su may/min/arm/mel
         *                              0.09*8 ->scale dim half/whole o whole/half
         *                              >0.09*9 -> cambi di tonalità e/o atonale
     */
    private double calcolaBiasTonalità(){
        Double p = 0.0;
        for(int i = 0;i<11;i++){
            if(contNoteMod12[i]>= contatorenNoteTotale/11){
                //Log.println(Log.ASSERT,"Calc tonalita","Nota diatonica: "+convIntStrNota(contNoteMod12[i]));  //DEBUG
                p +=0.09;
            }
        } p = BigDecimal.valueOf(p).setScale(2, RoundingMode.HALF_UP).doubleValue();
        //Log.println(Log.ASSERT,"Calc tonalita","Dispersione Tonalità: "+p);   //DEBUG
        return p;
    }

    /**
     *  Per calcolare la velocità viene dedotto da noteOn.tick rispetto alla precendente:
     *  -> in primis viene confrontanta la velocità con dei valori static
     *  -> in base alla velocita della nota, viene applicato un bonus al punteggio (più è bassa la velocita più è basso il moltiplicatore che verrà moltiplicato al resto dell'algoritmo)
     *  ->
     * @return punteggio di velocità di esecuzione della nota proecessata, ne restituisce un valore compreso fra { 0.01 e 1.00 }
     */
    private double punteggioVelocita(){
        int arrotondamentoValue = 100;
        double punteggio;
        NoteOn nota = ln.get(ln.size()-1);
        if(nota.getDelta()>0) {
            punteggio = (1.00) / nota.getDelta();  //DIVISIONE DOUBLE INTERO! CAST LONG A DOUBLE
            punteggio = Math.floor(punteggio * arrotondamentoValue) / arrotondamentoValue;
            if(punteggio <0.01) punteggio = 0.01;
        }
        else punteggio = 0.01;
        return punteggio;
    }

    /**
     *  Questo metodo analizza l'ultima nota processata, la confronta con il vettore ln -> note recenti:
     *      -> assegna un punto per ogni semitono che c'è di differenza fra la nota precendente e quella successiva
     *      ->se la nota viene suonata contemporaneamente -o quasi- a quella precendente (o precedenti) viene dato un bonus di 0.2x per ogni voce aggiunta
     * @return punteggio melodico + armonico della nota processata
     */
    private int punteggioMelArm(){
        int points =0;
        int salto = 0;
        float moltAcco =1;
        int vociAccordo = 0;

        NoteOn notaCorrente =ln.get(ln.size()-1);

        if(ln.size()>1) { //se il vettore non contiene  più di una nota
            NoteOn notaPrec = ln.get(ln.size() - 2);
            if(notaCorrente.getNoteValue() > notaPrec.getNoteValue())
                salto += (notaCorrente.getNoteValue() - notaPrec.getNoteValue())%11;    //il salto, sia ascendente che discendente
            else                                                                        // può dare max 11 punti (poi si va nell'ottava successiva)
                salto += (notaPrec.getNoteValue() - notaCorrente.getNoteValue())%11;

            for( int i =0; i < ln.size();i++) {
                if ((notaCorrente.getTick() - ln.get(i).getTick()) < 10) {  //se c'è meno di 5ms fra la nota corrente e la precendente c'è un possibile accordo/appoggio
                    moltAcco += 0.1;
                    contatoreAppoggiature++;
                    vociAccordo++;
                    if (vociAccordo >= 3) {
                        moltAcco += 0.1;
                        break;
                    }
                } else vociAccordo = 0;
            }
        }
        if(vociAccordo>=3) {   //aggiorno contatore accordi e lo notifico nel log e/o output
            contatoreAccordi++;
            //Log.println(Log.ASSERT,"Algoritmo midi","Trovato un accordo a: "+notaCorrente.getTick());
        }
        points =  Float.floatToIntBits(salto*moltAcco);
        return points;
    }

    public long getPunteggioFinale(){
        return punteggio;
    }

    private void resetValori(){
        nTraccia = -1;
        if(ln!=null) ln.clear();
        else
            ln = new ArrayList<NoteOn>();  // la lista sarebbe meglio se dinamica in base al delta time
        sizeOfLN=30;
        contNoteMod12 = new int[11];
        punteggio = 0;
        contatorenNoteTotale=0;
        contatoreAppoggiature =0;
        contatoreAccordi =0;
        contatoreEventsNotNote = 0;
        bestPuntTemp = 0;
    }
}

/*private void aggiornaTonalita(){    //confronta l'ultima nota inserita con il vettore ton e dopo il vettore last, e decide se una nota è diatonica, non, o se è avvenuto un cambio ton
    NoteOn lastNota = ln.get(ln.size());
    boolean diatonica = true;
    for(int i=0;i<7;i++)   {} //controllo se diatonica
        //if(lastNota.getNoteValue()%12 == tonalita[i]) diatonica = false;

    if(!diatonica)          //Se NON è diatonica procedo con un controllo per vedere se è necessario un cambio tonalità
        for(int i=ln.size()-1; i>0; i--){ //for dall'ultimo al primo
            if(lastNota.getNoteValue()%12 == ln.get(i).getNoteValue()%12) { //se è la stessa nota vuol dire che la nota diatonica è stata ripetuta, quindi la ton va aggiornata
                Log.println(Log.ASSERT,"Evento refreshTonalita","Cambio tonalità a: "+lastNota.getDelta()/1000);
            }
        }
}*/