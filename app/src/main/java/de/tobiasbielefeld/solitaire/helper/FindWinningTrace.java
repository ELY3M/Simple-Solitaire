package de.tobiasbielefeld.solitaire.helper;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;

import de.tobiasbielefeld.solitaire.classes.Card;
import de.tobiasbielefeld.solitaire.classes.Stack;
import de.tobiasbielefeld.solitaire.classes.State;
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


    private static int MAX_TIME_MILLIS = 5000;

    private long maxTime;
    private int runCounter = 1;
    private boolean isRunning = false;
    private int currentId = 0;
    private long stackSize = 1;

    private java.util.Stack<State> stateStack = new java.util.Stack<>();


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
        stateStack.clear();
        stateStack.push(new State(normalCards,normalStacks, currentId));
        stackSize = 1;
        run();

        HandlerBreak handlerBreak = new HandlerBreak(currentId);
        handlerBreak.sendEmptyMessageDelayed(0, MAX_TIME_MILLIS);
    }

    public void run() {



        AsyncTask.execute(new Runnable() {
            @Override
            public void run () {



                while (isRunning) {

                    logText(""+stackSize);
                    //runCounter++;
                    //runTest(state);
                    //decrementRunCounter();

                    if (stateStack.isEmpty()){
                        gameLogic.setWinnableText("Cannot be won");
                        return;
                    }//*/


                    State state = stateStack.pop();

                    if (state.id != currentId){
                        return;
                    }
                    stackSize --;




                    if (System.currentTimeMillis() > maxTime){
                        return;
                    }

                    if (currentGame.autoCompleteStartTest(state)) {
                        returnWinningTrace(state);
                        return;
                    }


                    if (currentGame.hasMainStack()) {

                        boolean movedACard = moveCard(state);


                        if (movedACard) {
                            state.alreadyFlipped = false;
                        }

                        if (!movedACard && !state.alreadyFlipped){

                            State newState = state.deepCopy();

                            int result = currentGame.onMainStackTouch(newState);

                            stateStack.push(newState);
                            stackSize ++;



                            if (result == 2 ) {
                                newState.alreadyFlipped = true;
                            }
                        }//*/



                      /*  if (moveCard(state)) {
                            state.alreadyFlipped = false;
                        }


                        //State newState = state.deepCopy();
                        int result = currentGame.onMainStackTouch(state);

                        //stateStack.push(newState);
                        //stackSize ++;

                        if (result == 2 ) {
                            if (state.alreadyFlipped){
                                //gameLogic.setWinnableText("Cannot be won");
                                //stateStack.pop();
                                //return;
                            } else{
                                state.alreadyFlipped = true;
                            }
                        }*/



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

        boolean found = false;

        for (int i=0;i<state.stacks.length;i++){

            //do not check cards on the foundation stack
            if (currentGame.foundationStacksContain(i)){
                continue;
            }

            for (int j=0;j<state.stacks[i].getSize();j++){
                State.ReducedCard cardToMove = state.stacks[i].getCard(j);

                if (cardToMove.isUp() && currentGame.addCardToMovementGameTest(cardToMove,state.stacks)){

                    for (int k=0;k < stacks.length;k++){
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
                           /* else if (alreadyMoved(state,cardToMove.getId(),k)){
                                continue;
                            }//*/

                            int size = state.stacks[i].getSize() - j;

                            int[] cardsToMove = new int[size];

                            for (int l = 0; l < size; l++) {
                                cardsToMove[l] = cardToMove.getStack().getCard(j + l).getId();
                            }


                            State newState = state.deepCopy();

                            moveToStack(newState, k, cardsToMove);

                            stateStack.push(newState);
                            stackSize ++;

                            found = true;

                            //return true;
                        }
                    }
                }
            }
        }

        return found;
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

        state.addTrace(cardIds[0], destinationId, state.cards[cardIds[0]].getStackId());

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
