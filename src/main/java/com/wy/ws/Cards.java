package com.wy.ws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Cards {

    private boolean customized;

    private ArrayList<Integer> randomPool;

    private List<Card> pickedList;

    private static Random random = new Random();

    public Cards(String numbers) {
        this(false, numbers);
    }

    public Cards(boolean customized, String numbers) {
        this.customized = customized;
        pickedList = new ArrayList<>();
        randomPool = new ArrayList<>();
        if (numbers != null) {
            randomPool.addAll(
                    Arrays.stream(numbers.split(","))
                            .map(t -> Integer.parseInt(t))
                            .collect(Collectors.toList()));
        }
    }

    public Card pick(String cardKey) {
        if (randomPool.isEmpty()) {
            throw new IllegalStateException();
        }
        for (Card card : pickedList) {
            if (card.getKey().equals(cardKey)) {
                throw new IllegalStateException("Duplicated request for the key " + cardKey);
            }
        }

        int bound = randomPool.size();
        int index = random.nextInt(bound);
        Integer number = randomPool.remove(index);
        Card card = new Card(cardKey, String.valueOf(number));
        pickedList.add(card);

        return card;
    }

    public List<Card> getPickedList() {
        return pickedList;
    }

}
