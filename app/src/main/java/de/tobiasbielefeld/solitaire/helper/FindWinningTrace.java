package de.tobiasbielefeld.solitaire.helper;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import de.tobiasbielefeld.solitaire.classes.Card;
import de.tobiasbielefeld.solitaire.classes.Stack;

import static de.tobiasbielefeld.solitaire.SharedData.autoWin;
import static de.tobiasbielefeld.solitaire.SharedData.currentGame;
import static de.tobiasbielefeld.solitaire.SharedData.logText;


/**
 * Created by tobias on 20.03.18.
 */

public class FindWinningTrace {

    private boolean foundWinningTrace = false;

    public class State{

        public ReducedStack[] stacks;
        public ReducedCard[] cards;
        public List<Entry> trace;

        public State(Card[] normalCards, Stack[] normalStacks){
            cards = new ReducedCard[normalCards.length];
            stacks = new ReducedStack[normalStacks.length];

            for (int i = 0; i < normalCards.length; i++){
                cards[i] = new ReducedCard(normalCards[i]);
            }

            for (int i = 0; i < normalStacks.length; i++){
                stacks[i] = new ReducedStack(normalStacks[i]);
            }

            trace = new ArrayList<>();
        }

        public State(State original){
            this.cards = original.cards.clone();
            this.stacks = original.stacks.clone();
            this.trace = new ArrayList<>(original.trace);
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
        }

        public class ReducedStack{
            public ArrayList<ReducedCard> currentCards = new ArrayList<>();                                    //the array of cards on the stack
            private int id;                                                                             //id: 0 to 6 tableau. 7 to 10 foundations. 11 and 12 discard and Main stack

            ReducedStack(Stack stack){
                id = stack.getId();

                for (Card card : stack.currentCards){
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
            }

            public int getId(){
                return id;
            }

            public boolean isEmpty(){
                return currentCards.size()==0;
            }
        }

        public class Entry {
            int cardId;
            int stackId;

            Entry(int cardId, int stackId) {
                this.cardId = cardId;
                this.stackId = stackId;
            }

            int getCardId() {
                return cardId;
            }

            int getStackId() {
                return stackId;
            }
        }

        public State deepCopy(){
            return new State(this);
        }

        public void addTrace(int cardId, int stackId){
            trace.add(new Entry(cardId,stackId));
        }
    }

    public void initiate(Stack[] normalStacks, Card[] normalCards){
        foundWinningTrace = false;
        run(new State(normalCards,normalStacks));
    }

    public void run(State state) {
        final State newState = state.deepCopy();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run () {
                runTest(newState);
            }
        });
    }

    private void runTest(State state){
        /*logText("running test...");

        logText(state.stacks[7].getSize() + " " + state.stacks[8].getSize()  + " " + state.stacks[9].getSize()  + " " + state.stacks[10].getSize());
        logText("size: " + state.trace.size());*/

        if (foundWinningTrace){
            return;
        }

        //if (currentGame.winTest(state)) {
        if (state.trace.size()>0) {
            foundWinningTrace = true;
            returnWinningTrace(state);
        }

        boolean foundCardToMove = false;

        for (int i=0;i<7;i++){


            for (int j=0;j<state.stacks[i].getSize();j++){
                State.ReducedCard cardToMove = state.stacks[i].getCard(j);

                if (cardToMove.isUp && currentGame.addCardToMovementGameTest(state,cardToMove)){

                    for (int k=0;k<state.stacks.length;k++){
                        State.ReducedStack destination = state.stacks[k];

                        if (i!=k && currentGame.cardTest(destination,cardToMove)){
                            foundCardToMove = true;
                            moveToStack(state, cardToMove.getId(), destination.getId());
                        }
                    }


                    break;
                }
            }

        }

        /*if (!foundCardToMove) {

            for (int i = 11; i < 15; i++) {


                for (int j = 0; j < state.stacks[i].getSize(); j++) {
                    State.ReducedCard cardToMove = state.stacks[i].getCard(j);

                    //if (currentGame.addCardToMovementGameTest(state, cardToMove)) {

                        for (int k = 0; k < state.stacks.length; k++) {
                            State.ReducedStack destination = state.stacks[k];

                            if (i != k && currentGame.cardTest(destination, cardToMove)) {
                                moveToStack(state, cardToMove.getId(), destination.getId());
                            }
                        }


                    //}
                }

            }
        }*/

    }

    public void returnWinningTrace(State state){
        logText("Winning trace found");
        autoWin.setTrace(state);
        autoWin.start();
    }

    public void moveToStack(State state, int cardId, int stackId) {
        state.addTrace(cardId,stackId);

        State.ReducedStack origin = state.stacks[state.cards[cardId].getStackId()];

        if (origin.getId() >10 ) {
            State.ReducedCard card = state.cards[cardId];
            card.removeFromCurrentStack();
            state.stacks[stackId].addCard(card);
        } else {
            for (int i = state.cards[cardId].getIndexOnStack(); i < origin.getSize(); i++) {
                State.ReducedCard card = origin.getCard(i);
                card.removeFromCurrentStack();
                state.stacks[stackId].addCard(card);
            }

            if (origin.getSize() > 0 && origin.getId() <= currentGame.getLastTableauId() && !origin.getTopCard().isUp()) {
                origin.getTopCard().flipUp();
            }
        }

        //handlerTestAfterMove.sendEmptyMessageDelayed(0, 100);

        /*if (origin.getSize() > 0 && origin.getId() <= currentGame.getLastTableauId() && !origin.getTopCard().isUp()) {
            origin.getTopCard().flipUp();
        }*/

        run(state);
    }

}
