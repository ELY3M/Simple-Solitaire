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

import de.tobiasbielefeld.solitaire.handler.HandlerAutoMove;
import de.tobiasbielefeld.solitaire.ui.GameManager;

public class AutoMove {

    public HandlerAutoMove handlerAutoMove;
    private boolean running = false;                                                                  //shows if the autocomplete is still running

    public AutoMove(GameManager gm){
        handlerAutoMove = new HandlerAutoMove(gm);
    }
    public void reset() {
        running = false;
    }

    public void start() {
        running = true;
        handlerAutoMove.sendEmptyMessage(0);
    }

    public boolean isRunning() {
        return running;
    }
}
