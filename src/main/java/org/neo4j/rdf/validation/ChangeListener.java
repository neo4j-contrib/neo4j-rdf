package org.neo4j.rdf.validation;

import org.neo4j.kernel.impl.event.Event;
import org.neo4j.kernel.impl.event.EventData;
import org.neo4j.kernel.impl.event.ProActiveEventListener;

public class ChangeListener implements ProActiveEventListener
{
    public boolean proActiveEventReceived( Event event, EventData data )
    {
        return true;
    }
    
//    private Map<Thread, Set<OwlInstance>> changes = Collections.synchronizedMap(
//        new HashMap<Thread, Set<OwlInstance>>() );
//    private Map<Thread, Set<OwlInstance>> deleted = Collections.synchronizedMap(
//        new HashMap<Thread, Set<OwlInstance>>() );
//    
//    /**
//     * Registers this listener on the event manager so that it can receive
//     * events.
//     */
//    public ChangeListener()
//    {
//        try
//        {
//            IdmNeoRepo.getInstance().eventManager().
//            registerProActiveEventListener( this,
//                IdmNeoRepo.INSTANCE_CHANGED );
//        }
//        catch ( Exception e )
//        {
//            throw new RuntimeException( e );
//        }
//    }
//    
//    /**
//     * Unregisters this listener, should be done at shut down of this component.
//     */
//    public void unregister()
//    {
//        try
//        {
//            IdmNeoRepo.getInstance().eventManager().
//            unregisterProActiveEventListener( this,
//                IdmNeoRepo.INSTANCE_CHANGED );
//        }
//        catch ( Exception e )
//        {
//            throw new RuntimeException( e );
//        }
//    }
//    
//    /**
//     * Received events about a changed {@link OwlInstance} and stores it in
//     * a set for the current thread.
//     */
//    public boolean proActiveEventReceived(
//        Event event, EventData data )
//    {
//        Thread thread = Thread.currentThread();
//        Set<OwlInstance> set = changes.get( thread );
//        if ( set == null )
//        {
//            set = new HashSet<OwlInstance>();
//            changes.put( thread, set );
//        }
//        Set<OwlInstance> deleted = this.deleted.get( thread );
//        if ( deleted == null )
//        {
//            deleted = new HashSet<OwlInstance>();
//            this.deleted.put( thread, deleted );
//        }
//        
//        InstanceEventData instanceData = ( InstanceEventData ) data.getData();
//        OwlInstance instance = instanceData.getObject();
//        if ( instance.isDeleted() ||
//            instanceData.getAlterationMode() == AlterationMode.DELETED )
//        {
//            set.remove( instance );
//            deleted.add( instance );
//        }
//        else if ( !deleted.contains( instance ) )
//        {
//            set.add( instance );
//        }
//        return true;
//    }
//    
//    /**
//     * @return all changed instances for the current thread since the last
//     * call to this method (the list is cleared after a call to this method)
//     */
//    public OwlInstance[] getChangedInstances()
//    {
//        Thread thread = Thread.currentThread();
//        Set<OwlInstance> set = changes.remove( thread );
//        return set == null ? new OwlInstance[ 0 ] :
//            set.toArray( new OwlInstance[ set.size() ] );
//    }
}
