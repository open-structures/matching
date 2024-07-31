package org.open_structures.matching;

import org.openstructures.flow.FlowNetwork;
import org.openstructures.flow.Node;
import org.openstructures.flow.PushRelabelMaxFlow;
import org.openstructures.flow.ValueNode;

import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

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
        for (Node wNode : flowNetwork.getSuccessors(flowNetwork.getSink())) {
            for (Node uNode : flowNetwork.getPredecessors(flowNetwork.getSource())) {
                if (existsFlow(wNode, uNode)) {
                    matches.put(((ValueNode<U>) uNode).getValue(), ((ValueNode<V>) wNode).getValue());
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

        final Node source = new SourceNode();
        final Node sink = new SinkNode();
        FlowNetwork flowNetwork = new FlowNetwork(source, sink);
        for (V v : vSet) {
            flowNetwork.setArcCapacity(1, node(v), sink);
        }

        for (U u : uSet) {
            flowNetwork.setArcCapacity(1, source, node(u));
            for (V v : vSet) {
                if (matchPredicate.test(u, v)) {
                    Node uNode = node(u);
                    Node vNode = node(v);
                    flowNetwork.setArcCapacity(1, uNode, vNode);
                }
            }
        }

        return new Matching<>(flowNetwork);
    }

    private static final class SourceNode implements Node {
    }

    private static final class SinkNode implements Node {
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