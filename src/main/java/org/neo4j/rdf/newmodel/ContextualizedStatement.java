package org.neo4j.rdf.newmodel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ContextualizedStatement extends Statement
{
    private final List<Context> contextList;
    
    public ContextualizedStatement( Resource subject, Uri predicate,
        Value object, Context... contexts )
    {
        super( subject, predicate, object );
        contextList = Collections.unmodifiableList( Arrays.asList( contexts ) );
    }
    
    public Iterable<Context> getContexts()
    {
        return this.contextList;
    }
}
