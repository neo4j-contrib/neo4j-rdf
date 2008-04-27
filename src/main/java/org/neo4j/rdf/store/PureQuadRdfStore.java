package org.neo4j.rdf.store;


public class PureQuadRdfStore //extends RdfStoreImpl
{
//    private final MetaStructure meta;
//
//    public PureQuadRdfStore( NeoService neo, MetaStructure meta,
//        PureQuadRepresentationStrategy representationStrategy )
//    {
//         super( neo, representationStrategy );
//         this.meta = meta;
//    }
//
//    protected MetaStructure meta()
//    {
//        return this.meta;
//    }
//
//    @Override
//    protected PureQuadRepresentationStrategy getRepresentationStrategy()
//    {
//        return ( PureQuadRepresentationStrategy )
//            super.getRepresentationStrategy();
//    }
//
//    @Override
//    public Iterable<Statement> getStatements( WildcardStatement statement,
//        boolean includeInferredStatements )
//    {
//        Transaction tx = neo().beginTx();
//        try
//        {
//            if ( !includeInferredStatements )
//            {
//                throw new UnsupportedOperationException( "We currently only " +
//                    "support getStatements() with reasoning enabled" );
//            }
//
//            Iterable<Statement> result = null;
//            if ( wildcardPattern( statement, false, false, true ) )
//            {
//                result = handleSubjectPredicateWildcard( statement );
//            }
//            else if ( wildcardPattern( statement, false, true, true ) )
//            {
//                result = handleSubjectWildcardWildcard( statement );
//            }
//            else if ( wildcardPattern( statement, true, true, false ) )
//            {
//
//            }
//            else if ( wildcardPattern( statement, true, false, false ) )
//            {
//
//            }
//            else
//            {
//                result = super.getStatements( statement,
//                    includeInferredStatements );
//            }
//
//            tx.success();
//            return result;
//        }
//        finally
//        {
//            tx.finish();
//        }
//    }
//
//    private Iterable<Statement> handleSubjectPredicateWildcard(
//        WildcardStatement statement )
//    {
//        Uri subject = ( Uri ) statement.getSubject();
//        Uri predicate = ( Uri ) statement.getPredicate();
//
//        AbstractNode abstractSubjectNode = new AbstractNode( subject );
//        Node subjectNode = getRepresentationStrategy().getExecutor().
//            lookupNode( abstractSubjectNode );
//        if ( subjectNode == null )
//        {
//            return new ArrayList<Statement>();
//        }
//
//        Validatable validatableInstance = new PureQuadValidatable( neo(),
//            subjectNode, meta() );
//        List<Statement> statementList = new LinkedList<Statement>();
//        addObjects( statementList, subject, predicate, subjectNode,
//            validatableInstance );
//        return statementList;
//    }
//
//    private void addObjects( List<Statement> statementList, Uri subject,
//        Uri predicate, Node subjectNode, Validatable validatableInstance )
//    {
//        if ( getRepresentationStrategy().pointsToObjectType( predicate ) )
//        {
//            Collection<? extends Validatable> objectProperties =
//                validatableInstance.complexProperties(
//                    predicate.getUriAsString() );
//            for ( Validatable objectProperty : objectProperties )
//            {
//                statementList.add( new CompleteStatement( subject, predicate,
//                    objectProperty.getUri() ) );
//            }
//        }
//        else
//        {
//            Object[] objectValues = validatableInstance.getProperties(
//                predicate.getUriAsString() );
//            for ( Object value : objectValues )
//            {
//                statementList.add( new CompleteStatement( subject, predicate,
//                    new Literal( value ) ) );
//            }
//        }
//    }
//
//    private Iterable<Statement> handleSubjectWildcardWildcard(
//        WildcardStatement statement )
//    {
//        Uri subject = ( Uri ) statement.getSubject();
//        AbstractNode abstractSubjectNode = new AbstractNode( subject );
//        Node subjectNode = getRepresentationStrategy().getExecutor().
//            lookupNode( abstractSubjectNode );
//        if ( subjectNode == null )
//        {
//            return new ArrayList<Statement>();
//        }
//
//        Validatable validatableInstance = new PureQuadValidatable( neo(),
//            subjectNode, meta() );
//        List<Statement> statementList = new LinkedList<Statement>();
//        for ( String predicate : validatableInstance.getAllPropertyKeys() )
//        {
//            addObjects( statementList, subject, new Uri( predicate ),
//                subjectNode, validatableInstance );
//        }
//        for ( MetaStructureClass cls : validatableInstance.getClasses() )
//        {
//            statementList.add( new CompleteStatement( subject, new Uri(
//                AbstractUriBasedExecutor.RDF_TYPE_URI ), new Uri(
//                    cls.getName() ) ) );
//        }
//        return statementList;
//    }
}
