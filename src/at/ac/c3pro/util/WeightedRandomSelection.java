package at.ac.c3pro.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Class for drawing interaction types to close remaining open branches
 * according to the frequency each interaction type was present in the beginning
 */
public class WeightedRandomSelection<InteractionType> {

	private List<InteractionType> interactionTypes;
	private List<Double> cumWeights;
	private Random random;

	public WeightedRandomSelection(Map<InteractionType, Integer> interactionTypes) {
		this.interactionTypes = new ArrayList<>();
		this.cumWeights = new ArrayList<>();

		int sumAllInteractions = interactionTypes.values().stream().mapToInt(Integer::intValue).sum();

		double totalWeight = 0;
		for (Map.Entry<InteractionType, Integer> entry : interactionTypes.entrySet()) {
			this.interactionTypes.add(entry.getKey());
			totalWeight += (double) entry.getValue().intValue() / sumAllInteractions;
			this.cumWeights.add(totalWeight);
		}

		this.random = new Random();
	}

	public InteractionType getRandomInteractionTypeAccInitialDist() {
		double value = random.nextDouble() * cumWeights.get(cumWeights.size() - 1);

		for (int i = 0; i < cumWeights.size(); i++) {
			if (value < cumWeights.get(i)) {
				return interactionTypes.get(i);
			}
		}
		return null;
	}
}
