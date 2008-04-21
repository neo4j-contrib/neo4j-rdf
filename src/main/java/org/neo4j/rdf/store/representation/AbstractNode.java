package org.neo4j.rdf.store.representation;

import java.util.Map;

import org.neo4j.rdf.model.Uri;

public interface AbstractNode extends AbstractElement
{
    Map<String, Object> properties();
    Uri getUriOrNull();
}
