package org.open_structures.matching;

import org.openstructures.flow.FlowNetwork;
import org.openstructures.flow.Node;
import org.openstructures.flow.PushRelabelMaxFlow;
import org.openstructures.flow.ValueNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Objects.requireNonNull;
import static org.openstructures.flow.ValueNode.node;

public class Matching<U, V> {
    private final FlowNetwork flowNetwork;
    private final PushRelabelMaxFlow flow;

    private Matching(FlowNetwork flowNetwork) {
        this.flowNetwork = requireNonNull(flowNetwork);
        this.flow = new PushRelabelMaxFlow(flowNetwork);
    }

    public FlowNetwork getFlowNetwork() {
        return flowNetwork;
    }

    public void findMatching() {
        // Pushes as much flow as possible from source to sink
        flow.preflowPush();
    }

    public Map<U, V> getMatches() {
        Map<U, V> matches = newHashMap();
        for (Node vNode : flowNetwork.getSuccessors(flowNetwork.getSink())) {
            for (Node uNode : flowNetwork.getPredecessors(flowNetwork.getSource())) {
                if (existsFlow(vNode, uNode)) {
                    matches.put(((ValueNode<U>) uNode).getValue(), ((ValueNode<V>) vNode).getValue());
                }
            }
        }

        return matches;
    }

    /**
     * {@link Matching} is a network created from bipartite graph UV.
     * A node of set U is adjacent to a node is set V only if matchPredicate returns true.
     * The capacity of all arcs is set to 1 – one 'u' can be matched with only one 'v' and 'v' can only have one matching 'u'.
     * The source node is adjacent to all nodes in U and all nodes in V are adjacent to the sink node.
     */
    public static <U, V> Matching<U, V> newMatching(BiPredicate<U, V> matchPredicate, Set<U> uSet, Set<V> vSet) {
        checkNotNull(matchPredicate);
        checkNotNull(uSet);
        checkNotNull(vSet);

        Map<V, Integer> vSetAndQty = new HashMap<>();
        vSet.forEach(v -> vSetAndQty.put(v, 1));

        return newMatching(matchPredicate, uSet, vSetAndQty);
    }

    /**
     * Very similar to {@link #newMatching(BiPredicate, Set, Set)} except it allows to set quantity of elements in the V set.
     * An example of when that is useful is when U represents people and V represents job roles and some of those roles need several people.
     * For example, if role 1 requires 3 people and role 2 – one person, then it would be represented as {'role1':3, 'role2':1}
     * If U set contains two people that qualify for role 1 then they both could be matched (assigned) to it.
     */
    public static <U, V> Matching<U, V> newMatching(BiPredicate<U, V> matchPredicate, Set<U> uSet, Map<V, Integer> vSetAndQty) {
        checkNotNull(matchPredicate);
        checkNotNull(uSet);
        checkNotNull(vSetAndQty);

        final Node source = new SourceNode();
        final Node sink = new SinkNode();
        FlowNetwork flowNetwork = new FlowNetwork(source, sink);
        for (Map.Entry<V, Integer> vAndQty : vSetAndQty.entrySet()) {
            flowNetwork.setArcCapacity(vAndQty.getValue(), node(vAndQty.getKey()), sink);
        }

        for (U u : uSet) {
            Node uNode = node(u);
            flowNetwork.setArcCapacity(1, source, uNode);
            for (V v : vSetAndQty.keySet()) {
                if (matchPredicate.test(u, v)) {
                    Node vNode = node(v);
                    flowNetwork.setArcCapacity(1, uNode, vNode);
                }
            }
        }

        return new Matching<>(flowNetwork);
    }

    private static final class SourceNode implements Node {
        @Override
        public String toString() {
            return "Source";
        }
    }

    private static final class SinkNode implements Node {
        @Override
        public String toString() {
            return "Sink";
        }
    }

    private boolean existsFlow(Node origin, Node destination) {
        for (Node successor : flowNetwork.getSuccessors(origin)) {
            if (successor.equals(destination)) {
                return true;
            }
        }
        return false;
    }
}