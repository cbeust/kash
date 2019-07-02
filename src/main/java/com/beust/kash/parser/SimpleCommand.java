package com.beust.kash.parser;

import java.util.List;

import static com.beust.kash.parser.KashParser.joinToString;

public class SimpleCommand {
    public final List<Word> content;
    private List<String> words = null;
    public final String input;
    public final String output;
    public final String before;

    public SimpleCommand(List<Word> content, String input, String output, String before) {
        this.content = content; this.input = input; this.output = output; this.before = before;
    }

    public SimpleCommand(List<Word> content, String input, String output) {
        this(content, input, output, null);
    }

    /**
     * The content after transformation.
     */
    public void setWords(List<String> words) { this.words = words; }
    public List<String> getWords() { return words; }

    @Override public boolean equals(Object other) {
        SimpleCommand sc = (SimpleCommand) other;
        return content.equals((sc.content)) &&
                KashParser.eq(input, sc.input) &&
                KashParser.eq(output, sc.output) &&
                KashParser.eq(before, sc.before);
    }

    @Override public String toString() {
        return "[SimpleCommand " + joinToString(content) + "]";
    }
}

