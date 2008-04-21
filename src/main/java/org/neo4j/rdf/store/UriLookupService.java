package org.neo4j.rdf.store;

import org.neo4j.api.core.Node;
import org.neo4j.rdf.model.Uri;

public interface UriLookupService
{
    /**
     * Returns the underlying node for the resource with URI <code>uri</code>.
     * @param uri the URI of the resource
     * @return the underlying node for the resource or <code>null</code> if
     * it doesn't exist
     */
    Node getNodeForUri( Uri uri );
    /**
     * @see #getNodeForUri(Uri)
     */
    Node getNodeForUri( String uriAsString );
}
