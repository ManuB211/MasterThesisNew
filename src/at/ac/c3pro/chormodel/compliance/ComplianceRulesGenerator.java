package at.ac.c3pro.chormodel.compliance;

import at.ac.c3pro.node.Interaction;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ComplianceRulesGenerator {

    private final int numberOfRules;
    private final int numberOfInteractions;
    private final ArrayList<CompliancePattern> complianceRules = new ArrayList<CompliancePattern>();
    private final ArrayList<Interaction> interactions = new ArrayList<Interaction>();

    public ComplianceRulesGenerator(int numberOfInteractions, int numberOfRules) {
        this.numberOfRules = numberOfRules;
        this.numberOfInteractions = numberOfInteractions;
    }

    public ArrayList<CompliancePattern> generate() {
        // initialize interactions
        for (int i = 0; i < numberOfInteractions; i++) {
            Interaction ia = new Interaction();
            ia.setName("IA " + (i + 200));
            interactions.add(ia);
        }

        // init random rules
        for (int i = 0; i < numberOfRules; i++) {

            RulePattern randomPattern = RulePattern.getRandomPattern();
            String label = "CR" + i;
            Interaction p = getRandomInteraction();
            Interaction q;
            do {
                q = getRandomInteraction();
            } while (p.equals(q));

            switch (randomPattern) {
                case LEADTO:
                    complianceRules.add(new LeadsTo(label, p, q));
                    break;
                case PRECEDES:
                    complianceRules.add(new Precedes(label, p, q));
                    break;
                /*
                 * case UNIVERSAL: complianceRules.add(new Universal(label, p)); break;
                 */
                case EXISTS:
                    complianceRules.add(new Exists(label, p));
                    break;
                default:
                    break;
            }

        }
        return complianceRules;
    }

    private Interaction getRandomInteraction() {
        int index = ThreadLocalRandom.current().nextInt(interactions.size());
        return interactions.get(index);
    }

    public void printRules() {
        for (CompliancePattern cr : complianceRules) {
            if (cr instanceof LeadsTo) {
                System.out
                        .println(cr.getLabel() + ": " + cr.getP().getName() + " LeadsTo " + ((OrderPattern) cr).getQ());
            } else if (cr instanceof Precedes) {
                System.out.println(
                        cr.getLabel() + ": " + cr.getP().getName() + " Precedes " + ((OrderPattern) cr).getQ());
            } else if (cr instanceof Universal) {
                System.out.println(cr.getLabel() + ": " + cr.getP().getName() + " Universal");
            } else if (cr instanceof Exists) {
                System.out.println(cr.getLabel() + ": " + cr.getP().getName() + " Exists");
            }
        }
    }

    private enum RulePattern {
        LEADTO, PRECEDES, EXISTS;
        // UNIVERSAL;

        public static RulePattern getRandomPattern() {
            Random random = new Random();
            return values()[random.nextInt(values().length)];
        }
    }

}
