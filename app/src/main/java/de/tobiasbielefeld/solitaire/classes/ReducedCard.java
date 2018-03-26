package de.tobiasbielefeld.solitaire.classes;

/**
 * Created by tobias on 26.03.18.
 */

public class ReducedCard extends Card {

    ReducedCard(Card card){
        color = card.getColor();
        value = card.getValue();
        stack = card.getStack();
        id = card.getId();
        isUp = card.isUp();
    }

    public void setCardFront() {
    }

    public void setCardBack() {
    }

    public void setColor() {
    }

    public void setLocation(float pX, float pY) {
    }

    public void setLocationWithoutMovement(float pX, float pY) {
    }

    public void saveOldLocation() {
    }

    public void returnToOldLocation() {
    }

    public void flipUp() {
        isUp = true;
    }

    public void flipDown() {
        isUp = false;
    }

    public void flip() {
        isUp = !isUp;
    }

    public void flipWithAnim() {
        flip();
    }

}
