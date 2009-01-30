package org.neo4j.rdf.fulltext;

import java.util.Map;

public interface VerificationHook
{
    public static enum Status
    {
        OK,
        NOT_LITERAL,
        WRONG_LITERAL,
        MISSING,
    }
    
    void verificationStarting( int numberOfDocumentsToVerify );
    
    Status verify( long id, String predicate, Object literal );
    
    void oneWasSkipped();
    
    void verificationCompleted( Map<Status, Integer> counts );
}
