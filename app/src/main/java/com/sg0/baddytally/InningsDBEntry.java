package com.sg0.baddytally;

public class InningsDBEntry {
    String name;
    boolean current;
    String round;

    public InningsDBEntry() {}

    public InningsDBEntry(String name, boolean current, String round) {
        this.name = name;
        this.current = current;
        this.round = round;
    }

    public String getName() {
        return name;
    }

    public boolean isCurrent() {
        return current;
    }

    public String getRound() {
        return round;
    }

    @Override
    public String toString() {
        return "InningsEntry{" +
                "name='" + name + '\'' +
                ", current=" + current +
                ", round='" + round + '\'' +
                '}';
    }
}
