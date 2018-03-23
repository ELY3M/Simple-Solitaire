/*
 * Copyright (C) 2016  Tobias Bielefeld
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * If you want to contact me, send me an e-mail at tobias.bielefeld@gmail.com
 */

package de.tobiasbielefeld.solitaire.helper;

import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;

import de.tobiasbielefeld.solitaire.R;
import de.tobiasbielefeld.solitaire.SharedData;
import de.tobiasbielefeld.solitaire.classes.Card;
import de.tobiasbielefeld.solitaire.classes.CardAndStack;
import de.tobiasbielefeld.solitaire.classes.Stack;
import de.tobiasbielefeld.solitaire.games.Pyramid;
import de.tobiasbielefeld.solitaire.ui.GameManager;

import static de.tobiasbielefeld.solitaire.SharedData.animate;
import static de.tobiasbielefeld.solitaire.SharedData.autoComplete;
import static de.tobiasbielefeld.solitaire.SharedData.autoWin;
import static de.tobiasbielefeld.solitaire.SharedData.cards;
import static de.tobiasbielefeld.solitaire.SharedData.currentGame;
import static de.tobiasbielefeld.solitaire.SharedData.gameLogic;
import static de.tobiasbielefeld.solitaire.SharedData.moveToStack;
import static de.tobiasbielefeld.solitaire.SharedData.movingCards;
import static de.tobiasbielefeld.solitaire.SharedData.showToast;
import static de.tobiasbielefeld.solitaire.SharedData.stacks;

public class AutoWin {

    public HandlerAutoWin handlerWin = new HandlerAutoWin();
    private boolean isRunning = false;                                                                  //shows if the autocomplete is still running

    private final static int DELTA_TIME = 100;
    private final static int DELTA_TIME_SHORT = 20;

    private boolean testAfterMove = false;
    private boolean emptyMainStack = false;

    public void reset() {
        isRunning = false;
    }

    public void start() {
        isRunning = true;
        handlerWin.sendEmptyMessage(0);
    }

    public void handleMessage(){
        if (gameLogic.hasWon()){
            return;
        }

        if (animate.cardIsAnimating()) {
            handlerWin.sendEmptyMessageDelayed(0, DELTA_TIME_SHORT);
        }

        else if (testAfterMove) {
            currentGame.testAfterMove();
            testAfterMove = false;
            handlerWin.sendEmptyMessageDelayed(0, DELTA_TIME_SHORT);

        } else if (isRunning) {

            CardAndStack cardAndStack;

            cardAndStack = currentGame.hintTest();

            if (cardAndStack == null && currentGame.hasMainStack()){
                if (currentGame.getMainStack().isEmpty()){

                    if (emptyMainStack){
                        reset();
                        return;
                    }

                    emptyMainStack = true;
                }
                currentGame.onMainStackTouch();
                handlerWin.sendEmptyMessageDelayed(0, DELTA_TIME);

            } else if (currentGame.autoCompleteStartTest()){
                reset();
                autoComplete.start();
            }
            else if (cardAndStack != null) {
                emptyMainStack  = false;
                movingCards.reset();

                if (currentGame instanceof Pyramid){    //TODO manage this in another way
                    currentGame.cardTest(cardAndStack.getStack(),cardAndStack.getCard());
                }

                movingCards.add(cardAndStack.getCard(), 0, 0);
                movingCards.moveToDestination(cardAndStack.getStack());

                testAfterMove = true;

                handlerWin.sendEmptyMessageDelayed(0, DELTA_TIME);
            } else {
                reset();
            }
        }
    }


    public static class HandlerAutoWin extends Handler {

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            autoWin.handleMessage();
        }
    }
}
