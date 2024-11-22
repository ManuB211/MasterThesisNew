package at.ac.c3pro.util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import at.ac.c3pro.node.Interaction.InteractionType;

public class WeightedRandomSelection {

	private List<InteractionTypeItem> interactionTypeCounts;
	private Integer completeWeight;

	public WeightedRandomSelection(Map<InteractionType, Integer> pInteractionTypeCounts) {
		this.interactionTypeCounts = pInteractionTypeCounts.entrySet().stream()
				.map(item -> new InteractionTypeItem(item)).collect(Collectors.toList());
		this.completeWeight = pInteractionTypeCounts.values().stream().mapToInt(Integer::intValue).sum();
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

	private InteractionType type;
	private Integer weight;

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
