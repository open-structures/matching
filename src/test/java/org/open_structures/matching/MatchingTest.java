package org.open_structures.matching;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openstructures.flow.FlowNetwork;
import org.openstructures.flow.Node;

import java.util.Map;
import java.util.function.BiPredicate;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.openstructures.flow.ValueNode.node;

@RunWith(MockitoJUnitRunner.class)
public class MatchingTest {

    private final String person1 = "person-1";
    private final String person2 = "person-2";
    private final String person3 = "person-3";

    private final String task1 = "task-1";
    private final String task2 = "task-2";
    private final String task3 = "task-3";

    @Mock
    private BiPredicate<String, String> skillsPredicate;

    @Before
    public void setUp() {
        when(skillsPredicate.test(person1, task1)).thenReturn(true);
        when(skillsPredicate.test(person1, task2)).thenReturn(false);
        when(skillsPredicate.test(person1, task3)).thenReturn(false);

        when(skillsPredicate.test(person2, task1)).thenReturn(true);
        when(skillsPredicate.test(person2, task2)).thenReturn(true);
        when(skillsPredicate.test(person2, task3)).thenReturn(false);

        when(skillsPredicate.test(person3, task1)).thenReturn(false);
        when(skillsPredicate.test(person3, task2)).thenReturn(true);
        when(skillsPredicate.test(person3, task3)).thenReturn(true);
    }


    @Test
    public void buildTeamNetwork() {
        // when
        Matching<String, String> matching = Matching.newMatching(skillsPredicate, newHashSet(person1, person2, person3), newHashSet(task1, task2, task3));

        // then
        assertThat(matching).isNotNull();

        // and
        FlowNetwork flowNetwork = matching.getFlowNetwork();
        assertThat(flowNetwork).isNotNull();
        assertThat(flowNetwork.getSource()).isNotNull();
        assertThat(flowNetwork.getSink()).isNotNull();

        Node source = flowNetwork.getSource();
        Node sink = flowNetwork.getSink();

        assertThat(flowNetwork.getArcCapacity(source, node(person1))).isEqualTo(1);
        assertThat(flowNetwork.getArcCapacity(source, node(person2))).isEqualTo(1);
        assertThat(flowNetwork.getArcCapacity(source, node(person3))).isEqualTo(1);

        assertThat(flowNetwork.getArcCapacity(node(person1), node(task1))).isEqualTo(1);
        assertThat(flowNetwork.getArcCapacity(node(person1), node(task2))).isEqualTo(0);
        assertThat(flowNetwork.getArcCapacity(node(person1), node(task3))).isEqualTo(0);

        assertThat(flowNetwork.getArcCapacity(node(person2), node(task1))).isEqualTo(1);
        assertThat(flowNetwork.getArcCapacity(node(person2), node(task2))).isEqualTo(1);
        assertThat(flowNetwork.getArcCapacity(node(person2), node(task3))).isEqualTo(0);

        assertThat(flowNetwork.getArcCapacity(node(person3), node(task1))).isEqualTo(0);
        assertThat(flowNetwork.getArcCapacity(node(person3), node(task2))).isEqualTo(1);
        assertThat(flowNetwork.getArcCapacity(node(person3), node(task3))).isEqualTo(1);

        assertThat(flowNetwork.getArcCapacity(node(task1), sink)).isEqualTo(1);
        assertThat(flowNetwork.getArcCapacity(node(task2), sink)).isEqualTo(1);
        assertThat(flowNetwork.getArcCapacity(node(task3), sink)).isEqualTo(1);
    }

    @Test
    public void shouldFindMatching() {
        // given
        Matching<String, String> matching = Matching.newMatching(skillsPredicate, newHashSet(person1, person2, person3), newHashSet(task1, task2, task3));
        matching.findMatching();

        // when
        Map<String, String> matches = matching.getMatches();

        // then
        assertThat(matches.get(person1)).isEqualTo(task1);
        assertThat(matches.get(person2)).isEqualTo(task2);
        assertThat(matches.get(person3)).isEqualTo(task3);
    }

    /**
     * In this test we need 2 people for task1 and one person for task2.
     */
    @Test
    public void shouldFindMatchingWithMultipleQuantitiesOfU() {
        // given
        Matching<String, String> matching = Matching.newMatching(skillsPredicate, newHashSet(person1, person2, person3), Map.of(task1, 2, task2, 1));
        matching.findMatching();

        // when
        Map<String, String> matches = matching.getMatches();

        // then
        assertThat(matches.get(person1)).isEqualTo(task1);
        assertThat(matches.get(person2)).isEqualTo(task1);
        assertThat(matches.get(person3)).isEqualTo(task2);

    }
}
