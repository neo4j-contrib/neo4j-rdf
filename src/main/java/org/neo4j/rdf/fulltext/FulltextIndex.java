package org.neo4j.rdf.fulltext;

import org.neo4j.graphdb.Node;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.store.RdfStore;

/**
 * This is fulltext index which can be used to extend the search capabilities
 * of an {@link RdfStore}. The interface is very temporary and not at all
 * streamlined as you might notice. At the moment only the literals are
 * indexed.
 * 
 * The only methods which you have to use are probably just the search and end
 * methods. The search method is the method to search the index and the end
 * method must be called when a transaction is committed or rolled back.
 * The "txId" at moment is the Transaction objects hashCode() :). Not
 * very nice, but the desired functionality is reached. Data which is indexed
 * with the index or removeIndex methods are kept in memory until a call to
 * the end method. Then all that data for the transaction which was committed
 * is appended to an indexing queue.
 */
public interface FulltextIndex
{
    /**
     * Indexes a literal, or rather places it in memory in wait for a call
     * to the end method.
     * @param node the {@link Node} which holds the literal value.
     * @param predicate the predicate for the statement.
     * @param literal the literal value.
     */
    void index( Node node, Uri predicate, Object literal );
    
    /**
     * Removes a literal from the index, or rather places it in memory in wait
     * for a call to the end method.
     * @param node the {@link Node} which holds the literal value.
     * @param predicate the predicate for the statement.
     * @param literal the literal value to remove.
     */
    void removeIndex( Node node, Uri predicate, Object literal );
    
    /**
     * Clears the index so that a reindex can be made if necessary.
     */
    void clear();
    
    /**
     * Committs or rolls back the literals for the current transaction id
     * (the Transaction objects hashCode() value).
     * @param commit wether to actually commit.
     */
    void end( boolean commit );
    
    /**
     * Committs or rolls back the literals for the given transaction id
     * @param txId the transaction id.
     * @param commit wether to actually commit.
     */
    void end( int txId, boolean commit );
    
    /**
     * Searches the index for matches. See above for query format.
     * @param query the search query.
     * @return the matches sorted by relevance.
     */
    Iterable<RawQueryResult> search( String query );
    
    /**
     * Searches the index for matches. See above for query format.
     * @param query the search query.
     * @param snippetCountLimit only generate snippets for a maximum of hits.
     * The rest of the hits (if any) will be returned, but will not have
     * snippets generated for them.
     * @return the matches sorted by relevance.
     */
    Iterable<RawQueryResult> searchWithSnippets( String query,
        int snippetCountLimit );
    
    /**
     * Runs through the index and verifying that its contents are ok.
     * @param hook the hook which the caller uses to implement the verification
     * logic for an entry as well as displaying progress a.s.o.
     * @param queryOrNullForAll enter a query to just verify that result,
     * or pass in {@code null} to verify the entire index.
     * @return {@code true} if the index was completely ok, otherwise
     * {@code false}.
     */
    boolean verify( VerificationHook hook, String queryOrNullForAll );
    
    /**
     * @return the {@link LiteralReader} instance used to get data from a
     * literal.
     */
    LiteralReader getLiteralReader();
    
    /**
     * @return <code>true</code> if this fulltext index features a queue and
     * there's items in the queue waiting to be indexed, otherwise
     * <code>false</code>.
     */
    boolean queueIsEmpty();
    
    /**
     * Shuts down the index and its indexing threads.
     */
    void shutDown();
}
