package com.beust.kash.parser;

/**
 * @author Cedric Beust <cedric@refresh.io>
 * @since 07 02, 2019
 */
public class Word {
    public final String content;
    public final String surroundedBy;
    public Word(String content, String surroundedBy) {
        this.content = content;
        this.surroundedBy = surroundedBy;
    }
    @Override
    public String toString() {
        return surroundedBy == null
                ? content
                : surroundedBy + content + surroundedBy;
    }
}
