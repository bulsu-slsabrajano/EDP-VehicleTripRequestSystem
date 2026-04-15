package com.project.util;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.util.HashMap;
import java.util.Map;

//equivalent ng cardlayout
public class CardPane extends StackPane {

    private final Map<String, Node> cards = new HashMap<>();
    private String current = null;

    public void addCard(String name, Node card) {
        cards.put(name, card);
        getChildren().add(card);
        card.setVisible(false);
        card.setManaged(false);
    }

    public void show(String name) {
        cards.values().forEach(n -> { n.setVisible(false); n.setManaged(false); });
        Node card = cards.get(name);
        if (card != null) {
            card.setVisible(true);
            card.setManaged(true);
            current = name;
        }
    }

    public String getCurrent() { return current; }
}