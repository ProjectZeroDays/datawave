package datawave.query.language.parser.jexl;

import com.google.common.collect.Sets;
import datawave.query.jexl.JexlASTHelper;
import datawave.query.jexl.nodes.QueryPropertyMarker;
import org.apache.commons.jexl2.parser.JexlNode;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static datawave.query.jexl.JexlASTHelper.nodeToKey;

/**
 * Utility class that implements the {@link Set} interface for use with a collection of Jexl nodes.
 *
 * Nodes are stored in an underlying map of String keys to JexlNode values. If the underlying nodes are changed then the set is considered invalid.
 *
 * The node keys are built using {@link JexlASTHelper#nodeToKey(JexlNode)}.
 *
 * If the {@link #useSourceNodeForKeys} flag is set then {@link datawave.query.jexl.nodes.QueryPropertyMarker} nodes will generate a node key from the unwrapped
 * source node. NOTE: This flag is enabled by default.
 *
 * For example, (ASTDelayedPredicate &amp;&amp; FOO == 'bar') will be unwrapped to FOO == 'bar', generating the node key "FOO == 'bar'".
 */
public class JexlNodeSet implements Set<JexlNode> {
    
    // Determines if we unwrap QueryPropertyMarker nodes and use their source node to determine node uniqueness.
    private boolean useSourceNodeForKeys = false;
    
    // Internal map of node keys to nodes;
    private final Map<String,JexlNode> nodeMap;
    
    public JexlNodeSet() {
        this(true);
    }
    
    /**
     *
     * @param useSourceNodeForKeys
     *            determines how node keys are generated.
     */
    public JexlNodeSet(boolean useSourceNodeForKeys) {
        this.useSourceNodeForKeys = useSourceNodeForKeys;
        this.nodeMap = new HashMap<>();
    }
    
    /**
     * Get all the Jexl nodes in the set.
     *
     * @return the underlying node map values.
     */
    public Collection<JexlNode> getNodes() {
        return nodeMap.values();
    }
    
    /**
     * Get the set of generated node keys.
     * 
     * @return the underlying node map keySet.
     */
    public Set<String> getNodeKeys() {
        return nodeMap.keySet();
    }
    
    @Override
    public int size() {
        return nodeMap.size();
    }
    
    @Override
    public boolean isEmpty() {
        return nodeMap.isEmpty();
    }
    
    @Override
    public boolean contains(Object o) {
        if (o instanceof JexlNode) {
            String nodeKey = buildKey((JexlNode) o);
            return nodeMap.containsKey(nodeKey);
        }
        return false;
    }
    
    @Override
    public Iterator<JexlNode> iterator() {
        return Collections.unmodifiableCollection(nodeMap.values()).iterator();
    }
    
    @Override
    public Object[] toArray() {
        return nodeMap.entrySet().toArray();
    }
    
    @Override
    public <T> T[] toArray(T[] ts) {
        throw new UnsupportedOperationException("JexlNodeSet does not support toArray() calls to pre-allocated arrays.");
    }
    
    @Override
    public boolean add(JexlNode node) {
        String nodeKey = buildKey(node);
        if (nodeMap.containsKey(nodeKey)) {
            
            // If the node mapped to our node key is delayed, do not overwrite.
            // If we are not delayed but a delayed version of our node already exists, do not add.
            if (isDelayed(nodeMap.get(nodeKey)) || !isDelayed(node)) {
                return false;
            }
        }
        nodeMap.put(nodeKey, node);
        return true;
    }
    
    /**
     * Remove by object or node key.
     *
     * @param o
     *            object or node key to be removed from the set of Jexl nodes.
     * @return True if the set contained the specified element.
     */
    @Override
    public boolean remove(Object o) {
        if (o instanceof JexlNode) {
            // Remove by value
            String nodeKey = buildKey((JexlNode) o);
            return nodeMap.remove(nodeKey, nodeMap.get(nodeKey));
        } else if (o instanceof String) {
            // Remove by key
            JexlNode node = nodeMap.get(o);
            return nodeMap.remove(o, node);
        }
        return false;
    }
    
    @Override
    public boolean containsAll(Collection<?> collection) {
        if (collection != null) {
            for (Object o : collection) {
                if (!contains(o)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    @Override
    public boolean addAll(Collection<? extends JexlNode> collection) {
        boolean modified = false;
        if (collection != null) {
            for (Object o : collection) {
                if (o instanceof JexlNode) {
                    if (add((JexlNode) o)) {
                        modified = true;
                    }
                }
            }
        }
        return modified;
    }
    
    @Override
    public boolean retainAll(Collection<?> collection) {
        Set<String> retainKeys = new HashSet<>();
        if (collection != null) {
            for (Object o : collection) {
                retainKeys.add(buildKey((JexlNode) o));
            }
        }
        
        boolean modified = false;
        for (String key : Sets.newHashSet(nodeMap.keySet())) {
            if (!retainKeys.contains(key)) {
                if (remove(key)) {
                    modified = true;
                }
            }
        }
        return modified;
    }
    
    @Override
    public boolean removeAll(Collection<?> collection) {
        boolean modified = false;
        if (collection != null) {
            for (Object o : collection) {
                if (remove(o)) {
                    modified = true;
                }
            }
        }
        return modified;
    }
    
    @Override
    public void clear() {
        this.nodeMap.clear();
    }
    
    // Is a node marked as delayed for any reason?
    protected boolean isDelayed(JexlNode node) {
        return QueryPropertyMarker.instanceOf(node, null);
    }
    
    /**
     * Build a key for the provided Jexl node. If the {@link #useSourceNodeForKeys} flag is set, this method will unwrap
     * {@link datawave.query.jexl.nodes.QueryPropertyMarker} nodes when generating the node key.
     * 
     * @param node
     * @return
     */
    public String buildKey(JexlNode node) {
        if (useSourceNodeForKeys && isDelayed(node)) {
            JexlNode sourceNode = QueryPropertyMarker.getQueryPropertySource(node, null);
            return nodeToKey(sourceNode);
        } else {
            return nodeToKey(node);
        }
    }
}
