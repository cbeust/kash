package com.beust.kash.parser;

import java.util.List;

import static com.beust.kash.parser.KashParser.joinToString;

/**
 * @author Cedric Beust <cedric@beust.com>
 * @since 07 02, 2019
 */
public class SimpleCmd {
    public final List<String> content;
    public final String surroundedBy;

    public SimpleCmd(List<String> content, String surroundedBy) {
        this.content = content;
        this.surroundedBy = surroundedBy;
    }

    public SimpleCmd(List<String> content) {
        this(content, null);
    }

    @Override
    public String toString() {
        return surroundedBy == null
                ? joinToString(content)
                : surroundedBy + content + surroundedBy;
    }
}
