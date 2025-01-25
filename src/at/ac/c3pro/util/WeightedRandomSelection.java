package at.ac.c3pro.util;

import at.ac.c3pro.node.Interaction.InteractionType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class handles random draws of interaction types according to the initial
 * distribution of the user input. It purposely excludes the handover-of-work
 * interaction type though, when the draw of said type would lead to more
 * handover-of-work interactions in the resulting model than feasible. That
 * scenario holds, when the handover-of-work type would be present more than
 * participants-1 times
 */
public class WeightedRandomSelection {

    private List<InteractionTypeItem> interactionTypeCounts;
    private Integer completeWeight;

    private final Integer amountParticipants;
    private Integer amountHOWAlreadyAdded;

    public WeightedRandomSelection(Map<InteractionType, Integer> pInteractionTypeCounts, Integer pAmountParticipants) {
        this.interactionTypeCounts = pInteractionTypeCounts.entrySet().stream()
                .map(item -> new InteractionTypeItem(item)).collect(Collectors.toList());
        this.completeWeight = pInteractionTypeCounts.values().stream().mapToInt(Integer::intValue).sum();
        this.amountParticipants = pAmountParticipants;
        this.amountHOWAlreadyAdded = 0;
    }

    public void handleHandoverOfWorkAdded() {
        amountHOWAlreadyAdded++;

        if (amountHOWAlreadyAdded == amountParticipants - 1) {
            recomputeRandomSelection();
        }
    }

    private void recomputeRandomSelection() {
        List<InteractionTypeItem> interactionTypeCountsCopy = new ArrayList<>(interactionTypeCounts);

        Iterator<InteractionTypeItem> iter = interactionTypeCountsCopy.iterator();

        while (iter.hasNext()) {
            InteractionTypeItem curr = iter.next();

            if (curr.getType().equals(InteractionType.HANDOVER_OF_WORK)) {
                iter.remove();
                break;
            }
        }

        this.completeWeight = interactionTypeCountsCopy.stream().mapToInt(elem -> elem.getWeight()).sum();
        this.interactionTypeCounts = interactionTypeCountsCopy;
    }

    public InteractionType chooseAccordingToDistribution() {
        double weight = 0.0;

        double random = Math.random() * this.completeWeight;

        double weightCounter = 0;
        for (InteractionTypeItem interactionType : this.interactionTypeCounts) {
            weightCounter += interactionType.getWeight();
            if (weightCounter >= random)
                return interactionType.getType();
        }
        throw new IllegalStateException(
                "Error in trying to get random interaction type based on its distribution. This error should have never occured");
    }
}

class InteractionTypeItem {

    private final InteractionType type;
    private final Integer weight;

    InteractionTypeItem(Map.Entry<InteractionType, Integer> typeToWeight) {
        this.type = typeToWeight.getKey();
        this.weight = typeToWeight.getValue();
    }

    public Integer getWeight() {
        return this.weight;
    }

    public InteractionType getType() {
        return this.type;
    }
}
