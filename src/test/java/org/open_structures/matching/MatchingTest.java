package org.open_structures.matching;

import com.google.common.collect.Table;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openstructures.flow.FlowNetwork;
import org.openstructures.flow.Node;
import org.openstructures.flow.PushRelabelMaxFlow;

import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.openstructures.flow.ValueNode.node;

@RunWith(MockitoJUnitRunner.class)
public class MatchingTest {

    private final String u1 = "u1";
    private final String u2 = "u2";
    private final String u3 = "u3";

    private final String v1 = "v1";
    private final String v2 = "v2";
    private final String v3 = "v3";

    @Mock
    private BiPredicate<String, String> matchPredicate;

    @Before
    public void setUp() {
        when(matchPredicate.test(u1, v1)).thenReturn(true);
        when(matchPredicate.test(u1, v2)).thenReturn(false);
        when(matchPredicate.test(u1, v3)).thenReturn(false);

        when(matchPredicate.test(u2, v1)).thenReturn(true);
        when(matchPredicate.test(u2, v2)).thenReturn(true);
        when(matchPredicate.test(u2, v3)).thenReturn(false);

        when(matchPredicate.test(u3, v1)).thenReturn(false);
        when(matchPredicate.test(u3, v2)).thenReturn(true);
        when(matchPredicate.test(u3, v3)).thenReturn(true);
    }

    @Test
    public void buildTeamNetwork() {
        // when
        Matching<String, String> matching = Matching.newMatching(matchPredicate, newHashSet(u1, u2, u3), newHashSet(v1, v2, v3));

        // then
        assertThat(matching).isNotNull();

        // and
        FlowNetwork flowNetwork = matching.getFlowNetwork();
        assertThat(flowNetwork).isNotNull();
        assertThat(flowNetwork.getSource()).isNotNull();
        assertThat(flowNetwork.getSink()).isNotNull();

        Node source = flowNetwork.getSource();
        Node sink = flowNetwork.getSink();

        assertThat(flowNetwork.getArcCapacity(source, node(u1))).isEqualTo(1);
        assertThat(flowNetwork.getArcCapacity(source, node(u2))).isEqualTo(1);
        assertThat(flowNetwork.getArcCapacity(source, node(u3))).isEqualTo(1);

        assertThat(flowNetwork.getArcCapacity(node(u1), node(v1))).isEqualTo(1);
        assertThat(flowNetwork.getArcCapacity(node(u1), node(v2))).isEqualTo(0);
        assertThat(flowNetwork.getArcCapacity(node(u1), node(v3))).isEqualTo(0);

        assertThat(flowNetwork.getArcCapacity(node(u2), node(v1))).isEqualTo(1);
        assertThat(flowNetwork.getArcCapacity(node(u2), node(v2))).isEqualTo(1);
        assertThat(flowNetwork.getArcCapacity(node(u2), node(v3))).isEqualTo(0);

        assertThat(flowNetwork.getArcCapacity(node(u3), node(v1))).isEqualTo(0);
        assertThat(flowNetwork.getArcCapacity(node(u3), node(v2))).isEqualTo(1);
        assertThat(flowNetwork.getArcCapacity(node(u3), node(v3))).isEqualTo(1);

        assertThat(flowNetwork.getArcCapacity(node(v1), sink)).isEqualTo(1);
        assertThat(flowNetwork.getArcCapacity(node(v2), sink)).isEqualTo(1);
        assertThat(flowNetwork.getArcCapacity(node(v3), sink)).isEqualTo(1);
    }

    @Test
    public void shouldFindMatching() {
        // given
        Matching<String, String> matching = Matching.newMatching(matchPredicate, newHashSet(u1, u2, u3), newHashSet(v1, v2, v3));
        matching.findMatching();

        // when
        Table<String, String, Integer> matches = matching.getMatches();

        // then
        assertThat(matches.size()).isEqualTo(3);
        assertThat(matches.get(u1, v1)).isEqualTo(1);
        assertThat(matches.get(u2, v2)).isEqualTo(1);
        assertThat(matches.get(u3, v3)).isEqualTo(1);
    }

    /**
     * In this test we need 2 people for task1 and one person for task2.
     */
    @Test
    public void shouldFindMatchingWithMultiplesOfV() {
        // given
        Matching<String, String> matching = Matching.newMatching(matchPredicate, newHashSet(u1, u2, u3), Map.of(v1, 2, v2, 1));
        matching.findMatching();

        // when
        Table<String, String, Integer> matches = matching.getMatches();

        // then
        assertThat(matches.size()).isEqualTo(3);
        assertThat(matches.get(u1, v1)).isEqualTo(1);
        assertThat(matches.get(u2, v1)).isEqualTo(1);
        assertThat(matches.get(u3, v2)).isEqualTo(1);
    }

    @Test
    public void shouldFindMatchingWithMultiplesOfUAndV() {
        // given
        Matching<String, String> matching = Matching.newMatching(matchPredicate, newHashSet(u1, u2, u3), Map.of(v1, 2, v2, 2));
        matching.increaseUCount(u1, 1);
        matching.findMatching();

        // when
        Table<String, String, Integer> matches = matching.getMatches();

        // then
        assertThat(matches.size()).isEqualTo(3);
        assertThat(matches.get(u1, v1)).isEqualTo(2);
        assertThat(matches.get(u2, v2)).isEqualTo(1);
        assertThat(matches.get(u3, v2)).isEqualTo(1);
    }

    @Test
    public void shouldIncreaseUCount() {
        // given
        Matching<String, String> matching = Matching.newMatching(matchPredicate, newHashSet(u1, u2, u3), Map.of(v1, 2, v2, 1));

        // when
        matching.increaseUCount(u1, 2);

        // then
        FlowNetwork flowNetwork = matching.getFlowNetwork();
        assertThat(flowNetwork.getArcCapacity(flowNetwork.getSource(), node(u1))).isEqualTo(3);
        assertThat(flowNetwork.getArcCapacity(node(u1), node(v1))).isEqualTo(3);
        assertThat(flowNetwork.getArcCapacity(flowNetwork.getSource(), node(u2))).isEqualTo(1);
        assertThat(flowNetwork.getArcCapacity(flowNetwork.getSource(), node(u3))).isEqualTo(1);
    }

    @Test
    public void shouldSetMatch() {
        // given
        Matching<String, String> matching = Matching.newMatching(matchPredicate, Set.of(u1, u2, u3), Set.of(v1, v2, v3));

        // when
        matching.setMatch(u1, v1);
        matching.setMatch(u3, v3);

        // then
        Table<String, String, Integer> matches = matching.getMatches();
        assertThat(matches.size()).isEqualTo(2);
        assertThat(matches.get(u1, v1)).isEqualTo(1);
        assertThat(matches.get(u3, v3)).isEqualTo(1);

        // and when
        matching.findMatching();

        // then
        matches = matching.getMatches();
        assertThat(matches.size()).isEqualTo(3);
        assertThat(matches.get(u1, v1)).isEqualTo(1);
        assertThat(matches.get(u2, v2)).isEqualTo(1);
        assertThat(matches.get(u3, v3)).isEqualTo(1);
    }

    @Test
    public void shouldGetAndRestoreFlowState() {
        // given
        Matching<String, String> matching = Matching.newMatching(matchPredicate, Set.of(u1, u2, u3), Set.of(v1, v2, v3));
        matching.setMatch(u1, v1);

        // when
        PushRelabelMaxFlow.State state = matching.getState();
        matching.setMatch(u2, v2);
        matching.increaseUCount(u3, 1);
        matching.restore(state);

        // then everything is like it used to be
        Table<String, String, Integer> matches = matching.getMatches();
        assertThat(matches.size()).isEqualTo(1);
        assertThat(matches.get(u1, v1)).isEqualTo(1);

        FlowNetwork flowNetwork = matching.getFlowNetwork();
        assertThat(flowNetwork.getArcCapacity(flowNetwork.getSource(), node(u1))).isEqualTo(0); // because of the match
        assertThat(flowNetwork.getArcCapacity(flowNetwork.getSource(), node(u2))).isEqualTo(1);
        assertThat(flowNetwork.getArcCapacity(flowNetwork.getSource(), node(u3))).isEqualTo(1);
    }
}
