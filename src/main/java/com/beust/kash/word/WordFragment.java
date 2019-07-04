package com.beust.kash.word;

public class WordFragment {
    public final boolean isWord;
    public final String word;

    public WordFragment(String word, boolean isWord) {
        this.word = word;
        this.isWord = isWord;
    }

    @Override
    public String toString() {
        return "[Fragment " + (isWord ? "" : "$") + word + "]";
    }

    @Override
    public boolean equals(Object o) {
        WordFragment other = (WordFragment) o;
        return word.equals(other.word) && isWord == other.isWord;
    }
}
