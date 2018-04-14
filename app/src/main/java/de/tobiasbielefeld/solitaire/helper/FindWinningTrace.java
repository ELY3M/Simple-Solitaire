package de.tobiasbielefeld.solitaire.helper;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;

import de.tobiasbielefeld.solitaire.classes.Card;
import de.tobiasbielefeld.solitaire.classes.Stack;
import de.tobiasbielefeld.solitaire.games.Game;

import static de.tobiasbielefeld.solitaire.SharedData.autoWin;
import static de.tobiasbielefeld.solitaire.SharedData.currentGame;
import static de.tobiasbielefeld.solitaire.SharedData.findWinningTrace;
import static de.tobiasbielefeld.solitaire.SharedData.gameLogic;
import static de.tobiasbielefeld.solitaire.SharedData.logText;
import static de.tobiasbielefeld.solitaire.SharedData.stacks;

/**
 * Created by tobias on 20.03.18.
 */

public class FindWinningTrace {

    private static int TRACE_MAX_LENGTH = 20;
    private static int MAX_TIME_MILLIS = 5000;

    private volatile long maxTime;
    private volatile int runCounter = 1;
    private volatile boolean isRunning = false;
    private volatile int currentId = 0;

    public class State{

        public ReducedStack[] stacks;
        public ReducedCard[] cards;
        public List<Entry> trace;
        public int id;

        public State(Card[] normalCards, Stack[] normalStacks, int id){
            cards = new ReducedCard[normalCards.length];
            stacks = new ReducedStack[normalStacks.length];

            for (int i = 0; i < normalCards.length; i++){
                cards[i] = new ReducedCard(normalCards[i]);
            }

            for (int i = 0; i < normalStacks.length; i++){
                stacks[i] = new ReducedStack(normalStacks[i]);
            }

            trace = new ArrayList<>(TRACE_MAX_LENGTH);
            this.id = id;
        }

        public State(State original){
            cards = new ReducedCard[original.cards.length];
            stacks = new ReducedStack[original.stacks.length];
            trace = new ArrayList<>();

            for (int i = 0; i < original.cards.length; i++){
                cards[i] = new ReducedCard(original.cards[i]);
            }

            for (int i = 0; i < original.stacks.length; i++){
                stacks[i] = new ReducedStack(original.stacks[i]);
            }

            for (Entry entry : original.trace){
                trace.add(new Entry(entry));
            }

            //this.trace = new ArrayList<>(original.trace);

            this.id = original.id;
        }

        public class Entry {
            int card;
            int destination;
            int origin;

            Entry(int cardId, int destId, int originId) {
                card = cardId;
                destination = destId;
                origin = originId;
            }

            Entry(Entry original) {
                card = original.card;
                destination = original.destination;
                origin = original.origin;
            }
        }

        public class ReducedCard{
            private int color;                                                                          //1=clubs 2=hearts 3=Spades 4=diamonds
            private int value;                                                                          //1=ace 2,3,4,5,6,7,8,9,10, 11=joker 12=queen 13=king
            private int stackId;                                                                        //saves the stack where the card is placed
            private int id;                                                                             //internal id
            private boolean isUp;                                                                       //indicates if the card is placed upwards or backwards

            ReducedCard(Card card){
                color = card.getColor();
                value = card.getValue();
                stackId = card.getStackId();
                id = card.getId();
                isUp = card.isUp();
            }

            ReducedCard(ReducedCard card){
                color = card.getColor();
                value = card.getValue();
                stackId = card.getStackId();
                id = card.getId();
                isUp = card.isUp();
            }

            public ReducedStack getStack(){
                return stacks[stackId];
            }

            public int getStackId(){
                return stackId;
            }

            public int getId(){
                return id;
            }

            public int getIndexOnStack(){
                return stacks[stackId].currentCards.indexOf(this);
            }

            public void removeFromCurrentStack(){
                if (stackId!=-1) {
                    stacks[stackId].removeCard(this);
                    stackId = -1;
                }
            }

            public void setStack(int id){
                stackId = id;
            }

            public void flipUp(){
                isUp = true;
            }

            public boolean isUp(){
                return isUp;
            }

            public int getColor(){
                return color;
            }

            public int getValue(){
                return value;
            }

            public boolean isTopCard(){
                return stacks[stackId].currentCards.indexOf(this) == stacks[stackId].getSize()-1;
            }
        }

        public class ReducedStack{
            public ArrayList<ReducedCard> currentCards = new ArrayList<>();
            private int id;

            ReducedStack(Stack stack){
                id = stack.getId();

                for (Card card : stack.currentCards){
                    currentCards.add(cards[card.getId()]);
                }
            }

            ReducedStack(ReducedStack stack){
                id = stack.getId();

                for (ReducedCard card : stack.currentCards){
                    currentCards.add(cards[card.getId()]);
                }
            }

            public int getFirstUpCardPos() {
                for (int i = 0; i < currentCards.size(); i++) {
                    if (currentCards.get(i).isUp())
                        return i;
                }

                return -1;
            }

            public int getSize(){
                return currentCards.size();
            }

            public ReducedCard getTopCard() {
                return currentCards.get(currentCards.size() - 1);
            }

            public ReducedCard getCard(int index){
                return currentCards.get(index);
            }

            public void removeCard(ReducedCard card) {
                currentCards.remove(card);
            }

            public void removeCard(int index) {
                currentCards.remove(index);
            }

            public void addCard(ReducedCard card) {
                card.setStack(id);
                currentCards.add(card);

                if (currentGame.mainStacksContain(id)) {
                    card.isUp = false;
                } else if (currentGame.discardStacksContain(id)){
                    card.isUp = true;
                }
            }

            public int getId(){
                return id;
            }

            public boolean isEmpty(){
                return currentCards.size()==0;
            }
        }

        public State deepCopy(){
            return new State(this);
        }

        public void addTrace(int cardId, int destinationId, int originId){
            if (trace.size() == TRACE_MAX_LENGTH){
                trace.remove(0);
            }

            //counter ++;
            trace.add(new Entry(cardId,destinationId,originId));
        }
    }

    public static class HandlerBreak extends Handler {

        private int id;

        HandlerBreak(int id){
            this.id = id;
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (findWinningTrace.currentId == id && findWinningTrace.isRunning){
                    findWinningTrace.isRunning = false;
                    //logText("canceling");
            }
        }
    }

    public boolean isRunning(){
        return isRunning;
    }

    public void decrementRunCounter(){
        runCounter --;

        if (runCounter<=0 && isRunning){
            gameLogic.setWinnableText("Cannot be won");
            isRunning = false;
        }
    }

    public void returnWinningTrace(State state){
        gameLogic.setWinnableText("Winnable");
        //logTraces(state);
        //logState(state);
        isRunning = false;
        //logText("Winning trace found");
        //autoWin.setTrace(state);
        //autoWin.start();
    }

    public void initiate(Stack[] normalStacks, Card[] normalCards){

        //gameLogic.setWinnableText("Calculating...",false);
        runCounter = 0;

        isRunning = true;
        maxTime = System.currentTimeMillis() + MAX_TIME_MILLIS;
        currentId ++;
        run(new State(normalCards,normalStacks, currentId));

        HandlerBreak handlerBreak = new HandlerBreak(currentId);
        handlerBreak.sendEmptyMessageDelayed(0, MAX_TIME_MILLIS);
    }

    public void run(final State state) {


        AsyncTask.execute(new Runnable() {
            @Override
            public void run () {
                boolean alreadyFlipped = false;

                while (isRunning) {
                    //runCounter++;
                    //runTest(state);
                    //decrementRunCounter();

                    if (System.currentTimeMillis() > maxTime){
                        return;
                    }

                    if (currentGame.autoCompleteStartTest(state)) {
                        isRunning = false;
                        returnWinningTrace(state);
                        return;
                    }


                    if (currentGame.hasMainStack()) {

                        if (moveCard(state)) {
                            alreadyFlipped = false;
                        }

                        int result = currentGame.onMainStackTouch(state);

                        if (result == 2 ) {
                            if (alreadyFlipped){
                                gameLogic.setWinnableText("Cannot be won");
                                return;
                            } else{
                                alreadyFlipped = true;
                            }
                        }



                    } else {
                        moveCard(state);
                    }
                }
            }
        });
    }

    private void runTest(State state){

        if (!isRunning || System.currentTimeMillis() > maxTime || currentId != state.id){
            return;
        }

        /*if (state.trace.size() > 1) {
            State.Entry topTrace = state.trace.get(state.trace.size()-1);

            for (int j=state.trace.size()-2;j>=0;j--){
                State.Entry bottomTrace = state.trace.get(j);

                if (topTrace.card == bottomTrace.card && topTrace.origin == bottomTrace.destination && topTrace.destination == bottomTrace.origin
                        && currentGame.tableauStacksContain(bottomTrace.origin) && currentGame.tableauStacksContain(bottomTrace.destination)){
                    //logState(state);

                    //logText("Card: " + topTrace.card + " top_origin:" + topTrace.origin + " bottom_dest:" + bottomTrace.destination + " top_dest:" + topTrace.destination + " bottom_origin:" + bottomTrace.origin);

                    return;

                }
            }
        }//*/

        //logText("size: " + state.trace.size());
        //logText("" + runCounter);
        logState(state);

        /*if (state.counter == 0){
            logState(state);
        }*/

        if (currentGame.autoCompleteStartTest(state)) {
            isRunning = false;
            returnWinningTrace(state);
            return;
        }



        int maxCardFlips = state.stacks[currentGame.getMainStackId()].getSize() +1;

        if (currentGame.hasMainStack()) {

            while (!moveCard(state) && state.stacks[currentGame.getMainStackId()].getSize() != maxCardFlips) {


                currentGame.onMainStackTouch(state);

            }


        } else {
            moveCard(state);
        }


    }

    private boolean moveCard(State state){

        for (int i=0;i<state.stacks.length;i++){

            //do not check cards on the foundation stack
            if (currentGame.foundationStacksContain(i)){
                continue;
            }

            for (int j=0;j<state.stacks[i].getSize();j++){
                State.ReducedCard cardToMove = state.stacks[i].getCard(j);

                if (cardToMove.isUp() && currentGame.addCardToMovementGameTest(cardToMove,state.stacks)){

                    for (int k=01;k< stacks.length;k++){
                    //for (int k=state.stacks.length-1;k>=0;k--){
                        State.ReducedStack destination = state.stacks[k];

                        if (i!=k && currentGame.cardTest(destination,cardToMove)){

                            //if moving to foundation, but the card isn't on top of the stack (moving multiple cards to foundation)
                            if (currentGame.foundationStacksContain(k) && !cardToMove.isTopCard()){
                                continue;
                            }
                            //moving around the tableau
                            else if (currentGame.tableauStacksContain(i) && j == 0 && destination.isEmpty()){
                                continue;
                            }
                            //avoid moving cards between stacks, eg moving a nine lying on a ten moving to another then, moving it back and so on...
                            else if (currentGame.tableauStacksContain(i) && currentGame.sameCardOnOtherStack(cardToMove,state.stacks[k], Game.testMode2.SAME_VALUE_AND_COLOR)) {
                                continue;
                            }
                            //else if (alreadyMoved(state,cardToMove.getId(),k)){
                            //    continue;
                            //}//*/

                            int size = state.stacks[i].getSize() - j;

                            int[] cardsToMove = new int[size];

                            for (int l = 0; l < size; l++) {
                                cardsToMove[l] = cardToMove.getStack().getCard(j + l).getId();
                            }

                            moveToStack(state, k, cardsToMove);

                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean alreadyMoved(State state, int cardId, int destinationId){

        if (!currentGame.tableauStacksContain(cardId)) {
            return false;
        }

        for (int j=state.trace.size()-1;j>=0;j--){
            State.Entry trace = state.trace.get(j);

            if (trace.card == cardId && trace.origin == destinationId){
                return true;
            }
        }


        return false;
    }

    public void moveToStack(State state,  int destinationId, int... cardIds) {

        //final State newState = state.deepCopy();

        //state.addTrace(cardIds[0], destinationId, state.cards[cardIds[0]].getStackId());

        State.ReducedCard firstCard = state.cards[cardIds[0]];
        State.ReducedStack destination = state.stacks[destinationId];

        int indexOfFirstCard = firstCard.getIndexOnStack();


        //if the moving card is on the tableau, flip the card below it up
        if (indexOfFirstCard>0 && currentGame.tableauStacksContain(firstCard.getStackId())) {
            firstCard.getStack().getCard(indexOfFirstCard-1).flipUp();
        }


        for (int i : cardIds){
            state.cards[i].removeFromCurrentStack();
            destination.addCard( state.cards[i]);
        }


        //run(newState);
    }

    public void moveToStackInSameState(State state,  int destinationId, int... cardIds) {

        State.ReducedCard firstCard = state.cards[cardIds[0]];
        State.ReducedStack destination = state.stacks[destinationId];

        int indexOfFirstCard = firstCard.getIndexOnStack();

        //if the moving card is on the tableau, flip the card below it up
        if (indexOfFirstCard>0 && currentGame.tableauStacksContain(firstCard.getStackId())) {
            firstCard.getStack().getCard(indexOfFirstCard-1).flipUp();
        }

        for (int i : cardIds){
            state.cards[i].removeFromCurrentStack();
            destination.addCard( state.cards[i]);
        }
    }

    public void logState(State state){
        logText("############### Logging state ###############");

        for (int i=0;i<state.stacks.length;i++) {
            StringBuilder data = new StringBuilder();

            for (int j = 0; j < state.stacks[i].getSize(); j++) {
                data.append(state.stacks[i].currentCards.get(j).color).append(",").append(Integer.toString(state.stacks[i].currentCards.get(j).getValue())).append(" ");
            }

            logText("Stack " + i + ": " + data);
        }


        //logTraces(state);
        if (state.trace.size()>0) {
            logText("Last trace: " + state.cards[state.trace.get(state.trace.size() - 1).card].color + "," +state.cards[state.trace.get(state.trace.size() - 1).card].value + ": " +state.trace.get(state.trace.size() - 1).origin+ " -> " + state.trace.get(state.trace.size() - 1).destination);
        }
    }

    public void logTraces(State state){
        for (int i=0;i<state.trace.size();i++){
            logText("trace "+ i +": " + state.cards[state.trace.get(i).card].color + "," +state.cards[state.trace.get(i).card].value + ": " +state.trace.get(i).origin+ " -> " + state.trace.get(i).destination);

            }
    }
}
