package org.open_structures.matching;

import org.open_structures.memento.Restorable;
import org.openstructures.flow.FlowNetwork;
import org.openstructures.flow.Node;
import org.openstructures.flow.PushRelabelMaxFlow;
import org.openstructures.flow.ValueNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Objects.requireNonNull;
import static org.openstructures.flow.ValueNode.node;

public class Matching<U, V> implements Restorable<PushRelabelMaxFlow.State> {
    private final FlowNetwork flowNetwork;
    private final PushRelabelMaxFlow maxFlow;

    private Matching(FlowNetwork flowNetwork) {
        this.flowNetwork = requireNonNull(flowNetwork);
        this.maxFlow = new PushRelabelMaxFlow(flowNetwork);
    }

    FlowNetwork getFlowNetwork() {
        return flowNetwork;
    }

    public void findMatching() {
        // Pushes as much flow as possible from source to sink
        maxFlow.preflowPush();
    }

    /**
     * Increases count of the specific element of U
     */
    public void increaseUCount(U u, int increase) {
        checkNotNull(u);
        checkArgument(increase > 0);

        Node source = flowNetwork.getSource();
        Node uNode = node(u);
        checkArgument(flowNetwork.getSuccessors(source).contains(uNode) || flowNetwork.getPredecessors(source).contains(uNode));

        int currentCapacity = flowNetwork.getArcCapacity(source, uNode);
        flowNetwork.setArcCapacity(currentCapacity + increase, source, uNode);
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

        Map<V, Integer> vSetAndCount = new HashMap<>();
        vSet.forEach(v -> vSetAndCount.put(v, 1));

        return newMatching(matchPredicate, uSet, vSetAndCount);
    }

    /**
     * Very similar to {@link #newMatching(BiPredicate, Set, Set)} except it allows to set count of elements in the V set.
     * An example of when that is useful is when U represents people and V represents job roles and some of those roles need several people.
     * For example, if role 1 requires 3 people and role 2 – one person, then it would be represented as {'role1':3, 'role2':1}
     * If U set contains two people that qualify for role 1 then they both could be matched (assigned) to it.
     */
    public static <U, V> Matching<U, V> newMatching(BiPredicate<U, V> matchPredicate, Set<U> uSet, Map<V, Integer> vSetAndCount) {
        checkNotNull(matchPredicate);
        checkNotNull(uSet);
        checkNotNull(vSetAndCount);

        final Node source = new SourceNode();
        final Node sink = new SinkNode();
        FlowNetwork flowNetwork = new FlowNetwork(source, sink);
        for (Map.Entry<V, Integer> vAndQty : vSetAndCount.entrySet()) {
            flowNetwork.setArcCapacity(vAndQty.getValue(), node(vAndQty.getKey()), sink);
        }

        for (U u : uSet) {
            Node uNode = node(u);
            flowNetwork.setArcCapacity(1, source, uNode);
            for (V v : vSetAndCount.keySet()) {
                if (matchPredicate.test(u, v)) {
                    Node vNode = node(v);
                    flowNetwork.setArcCapacity(1, uNode, vNode);
                }
            }
        }

        return new Matching<>(flowNetwork);
    }

    /**
     * You can 'manually' match U with V by calling this method.
     * Subsequent call of {@link #findMatching()} will still search for maximum matching if not already archived.
     */
    public void setMatch(U u, V v) {
        checkNotNull(u);
        checkNotNull(v);

        Node source = flowNetwork.getSource();
        Node sink = flowNetwork.getSink();
        Node uNode = node(u);
        Node vNode = node(v);

        if (flowNetwork.getSuccessors(vNode).stream().anyMatch(node -> node.equals(uNode))) {
            // u and v are already matched;
            return;
        }
        if (flowNetwork.getSuccessors(source).stream().noneMatch(node -> node.equals(uNode))) {
            if (flowNetwork.getSuccessors(uNode).stream().anyMatch(node -> node.equals(source))) {
                throw new IllegalStateException(u + " has already been matched");
            } else {
                throw new IllegalStateException(u + " is not part of this matching");
            }
        }
        if (flowNetwork.getSuccessors(vNode).stream().noneMatch(node -> node.equals(sink))) {
            if (flowNetwork.getSuccessors(sink).stream().anyMatch(node -> node.equals(vNode))) {
                throw new IllegalStateException(v + " has already been matched");
            } else {
                throw new IllegalStateException(v + " is not part of this matching");
            }
        }
        if (flowNetwork.getSuccessors(uNode).stream().noneMatch(node -> node.equals(vNode))) {
            throw new IllegalStateException("There is not path between " + u + " and " + v);
        }
        maxFlow.pushFlow(1, flowNetwork.getSource(), uNode);
        maxFlow.pushFlow(1, uNode, vNode);
        maxFlow.pushFlow(1, vNode, flowNetwork.getSink());
    }

    @Override
    public PushRelabelMaxFlow.State getState() {
        return maxFlow.getState();
    }

    @Override
    public void restore(PushRelabelMaxFlow.State state) {
        checkNotNull(state);
        maxFlow.restore(state);
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