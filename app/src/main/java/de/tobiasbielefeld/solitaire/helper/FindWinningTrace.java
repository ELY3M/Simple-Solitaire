package de.tobiasbielefeld.solitaire.helper;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;

import de.tobiasbielefeld.solitaire.classes.Card;
import de.tobiasbielefeld.solitaire.classes.Stack;

import static de.tobiasbielefeld.solitaire.SharedData.currentGame;
import static de.tobiasbielefeld.solitaire.SharedData.findWinningTrace;
import static de.tobiasbielefeld.solitaire.SharedData.gameLogic;
import static de.tobiasbielefeld.solitaire.SharedData.logText;
import static de.tobiasbielefeld.solitaire.helper.FindWinningTrace.testMode2.SAME_VALUE;
import static de.tobiasbielefeld.solitaire.helper.FindWinningTrace.testMode2.SAME_VALUE_AND_COLOR;
import static de.tobiasbielefeld.solitaire.helper.FindWinningTrace.testMode2.SAME_VALUE_AND_FAMILY;

/**
 * Created by tobias on 20.03.18.
 */

public class FindWinningTrace {

    private static int TRACE_MAX_LENGTH = 20;
    private static int MAX_TIME_MILLIS = 5000;
    private static int CARDS_MAX_SIZE;

    private volatile long maxTime;
    private volatile int runCounter = 0;
    private volatile boolean isRunning = false;
    private volatile int currentId = 0;

    public class State{

        public ReducedStack[] stacks;
        public ReducedCard[] cards;
        public List<Entry> trace;
        public boolean mainStackAlreadyFlipped = false;
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

            for (int i = 0; i < original.cards.length; i++){
                cards[i] = new ReducedCard(original.cards[i]);
            }

            for (int i = 0; i < original.stacks.length; i++){
                stacks[i] = new ReducedStack(original.stacks[i]);
            }

            this.trace = new ArrayList<>(original.trace);

            this.mainStackAlreadyFlipped = original.mainStackAlreadyFlipped;

            this.id = original.id;
        }

        public class Entry {
            int card;
            int stack;

            Entry(int cardId, int stackId) {
                card = cardId;
                stack = stackId;
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
                currentCards.remove(currentCards.indexOf(card));
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

        public void addTrace(int cardId, int stackId){
            if (trace.size() == TRACE_MAX_LENGTH){
                trace.remove(0);
            }

            trace.add(new Entry(cardId,stackId));
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
            }
        }
    }

    public boolean isRunning(){
        return isRunning;
    }


    public void decrementRunCounter(){
        runCounter --;

        if (runCounter==0 && isRunning){
            gameLogic.setWinnableText("Cannot be won");
            isRunning = false;
        }
    }

    public void returnWinningTrace(){
        gameLogic.setWinnableText("Winnable");
        isRunning = false;
        //logText("Winning trace found");
        //autoWin.setTrace(state);
        //autoWin.start();
    }

    public void initiate(Stack[] normalStacks, Card[] normalCards){

        //gameLogic.setWinnableText("Calculating...",false);
        //runCounter = 0;

        CARDS_MAX_SIZE = normalCards.length;
        isRunning = true;
        maxTime = System.currentTimeMillis() + MAX_TIME_MILLIS;
        currentId ++;
        run(new State(normalCards,normalStacks, currentId));

        HandlerBreak handlerBreak = new HandlerBreak(currentId);
        handlerBreak.sendEmptyMessageDelayed(0, MAX_TIME_MILLIS);
    }

    public void run(final State state) {
        runCounter ++;

        AsyncTask.execute(new Runnable() {
            @Override
            public void run () {
                runTest(state);
                decrementRunCounter();
            }
        });
    }

    private void runTest(State state){

        boolean foundCardToMove = false;

        if (!isRunning || System.currentTimeMillis() > maxTime || currentId != state.id){
            return;
        }

        /*if (state.trace.size() == TRACE_MAX_LENGTH) {
            State.Entry topTrace = state.trace.get(TRACE_MAX_LENGTH-1);

            for (int j=TRACE_MAX_LENGTH-2;j>=0;j--){
                State.Entry bottomTrace = state.trace.get(j);

                if (topTrace.card == bottomTrace.card && topTrace.stack == bottomTrace.stack){
                    decrementRunCounter();
                    return;
                }
            }
        }*/

        //logText("size: " + currentId + " " + state.id);
        //logText("" + runCounter);
        //logState(state);

        if (currentGame.autoCompleteStartTest(state)) {
            isRunning = false;
            returnWinningTrace();
            return;
        }

        for (int i=0;i<state.stacks.length;i++){

            //do not check cards on the foundation stack
            if (currentGame.foundationStacksContain(i)){
                continue;
            }

            //for (int j=0;j<state.stacks[i].getSize();j++){
            for (int j=state.stacks[i].getSize()-1;j>=0;j--){
                State.ReducedCard cardToMove = state.stacks[i].getCard(j);

                if (cardToMove.isUp() && currentGame.addCardToMovementGameTest(state, cardToMove)){

                    for (int k=state.stacks.length-1;k>=0;k--){
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
                            else if (i <= currentGame.getLastTableauId() && sameCardOnOtherStack(cardToMove,state.stacks[k],SAME_VALUE_AND_COLOR)) {
                                continue;
                            }

                            foundCardToMove = true;

                            int size = state.stacks[i].getSize() - j;

                            List<State.ReducedCard> cardsToMove = new ArrayList<>(size);

                            for (int l = j; l < j + size; l++) {
                                cardsToMove.add( cardToMove.getStack().getCard(l));
                            }

                            //if the moving card is on the tableau, flip the card below it up
                            if (j>0 && currentGame.tableauStacksContain(i)) {
                                state.stacks[i].getCard(j-1).flipUp();
                            }

                            //mark this, so flipping the main stack over will be possible again
                            state.mainStackAlreadyFlipped = false;

                            moveToStack(state, cardsToMove, destination);
                        }
                    }
                }
            }
        }

        if (!foundCardToMove) {
            if (currentGame.hasMainStack() && !(getMainStack(state).isEmpty() && state.mainStackAlreadyFlipped)){

                int event = currentGame.onMainStackTouch(state);

                if (event==2){
                    state.mainStackAlreadyFlipped = true;
                }
            }
        }
    }

    private State.ReducedStack getMainStack(State state){
        return state.stacks[currentGame.getMainStackId()];
    }

    public void moveToStack(State state, State.ReducedCard card, State.ReducedStack destination) {
        List<State.ReducedCard> cards = new ArrayList<>(CARDS_MAX_SIZE);
        cards.add(card);

        moveToStack(state, cards, destination);
    }

    public void moveToStack(State state, List<State.ReducedCard> cards,  State.ReducedStack destination) {

        final State newState = state.deepCopy();

        List<State.ReducedCard> newCards = new ArrayList<>(CARDS_MAX_SIZE);
        State.ReducedStack newDestination = newState.stacks[destination.getId()];

        for (int i=0;i<cards.size();i++){
            newCards.add(newState.cards[cards.get(i).getId()]);
        }

        newState.addTrace(newCards.get(0).getId(),newDestination.getId());

        for (int i = 0; i < newCards.size(); i++) {
            if (newCards.get(i).getStack() != newDestination) {
                newCards.get(i).removeFromCurrentStack();
                newDestination.addCard(newCards.get(i));
            }
        }

        run(newState);
    }

    public void logState(State state){
        logText("############################################");
        logText("############### Loggin state ###############");
        logText("############################################");

        for (int i=0;i<state.stacks.length;i++) {
            StringBuilder data = new StringBuilder();

            for (int j = 0; j < state.stacks[i].getSize(); j++) {
                data.append(Integer.toString(state.stacks[i].currentCards.get(j).getValue())).append(" ");
            }

            logText("Stack " + i + ": " + data);
        }


        if (state.trace.size()>0) {
            logText("");
            logText("Last trace: " + state.cards[state.trace.get(state.trace.size() - 1).card].value + "->" + state.trace.get(state.trace.size() - 1).stack);
        }
    }

    protected boolean sameCardOnOtherStack(State.ReducedCard card, State.ReducedStack otherStack, testMode2 mode) {
        State.ReducedStack origin = card.getStack();

        if (card.getIndexOnStack() > 0 && origin.getCard(card.getIndexOnStack() - 1).isUp() && otherStack.getSize() > 0) {
            State.ReducedCard cardBelow = origin.getCard(card.getIndexOnStack() - 1);

            if (mode == SAME_VALUE_AND_COLOR) {
                if (cardBelow.getValue() == otherStack.getTopCard().getValue() && cardBelow.getColor() % 2 == otherStack.getTopCard().getColor() % 2) {
                    return true;
                }
            } else if (mode == SAME_VALUE_AND_FAMILY) {
                if (cardBelow.getValue() == otherStack.getTopCard().getValue() && cardBelow.getColor() == otherStack.getTopCard().getColor()) {
                    return true;
                }
            } else if (mode == SAME_VALUE) {
                if (cardBelow.getValue() == otherStack.getTopCard().getValue()) {
                    return true;
                }
            }
        }

        return false;
    }

    protected enum testMode2 {
        SAME_VALUE_AND_COLOR, SAME_VALUE_AND_FAMILY, SAME_VALUE
    }

}
