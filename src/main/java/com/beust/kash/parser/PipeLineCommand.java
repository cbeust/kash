package com.beust.kash.parser;

import java.util.List;

public class PipeLineCommand extends AbstractCommand<List<Command>> {
    public final String precededBy;
    public PipeLineCommand(List<Command> content, String precededBy) {
        super(content);
        this.precededBy = precededBy;
    }
    @Override public boolean equals(Object other) {
        PipeLineCommand cl = (PipeLineCommand) other;
        return KashParser.eq(precededBy, cl.precededBy) && content.equals(cl.content);
    }
}
