package de.tobiasbielefeld.solitaire.classes;

import android.graphics.PointF;
import android.graphics.RectF;
import android.widget.RelativeLayout;

/**
 * Created by tobias on 26.03.18.
 */

public class ReducedStack extends Stack {

    public boolean isOnLocation(float pX, float pY) {
        return false;
    }

    public PointF getPosition(int offset) {
        return null;
    }

    public void save() {
    }

    public void load() {
    }

    public void updateSpacing() {
    }

    public void applyArrow() {
    }

    public void setSpacingMax(int index) {
    }

    public void setSpacingMax(RelativeLayout layoutGame) {
    }

    public void mirrorStack(RelativeLayout layoutGame) {
    }

    public RectF getRect() {
        return null;
    }

    public void setSpacingDirection(Stack.SpacingDirection value) {
    }

    public void setArrow(Stack.ArrowDirection direction) {
    }

    public float getX() {
        return 0;
    }

    public void setX(float X) {
    }

    public float getY() {
        return 0;
    }

    public void setY(float Y) {
    }

    public void applyDefaultSpacing(){
    }

    public void flipTopCardUp(){
        if (getSize() > 0){
            getTopCard().flipUp();
        }
    }

    public void exchangeCard(Card oldCard, Card newCard){

    }
}
