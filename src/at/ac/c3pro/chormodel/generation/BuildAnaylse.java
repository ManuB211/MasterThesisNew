package at.ac.c3pro.chormodel.generation;

public class BuildAnaylse {

    public int iterations;
    public int interactions;
    public int actInteractions;
    public int xors;
    public int ands;
    public int maxBranching;
    public int crIas;
    public int crs;
    public boolean success;
    public long duration;

    public BuildAnaylse(int iterations, int interactions, int actInteractions, boolean success, long duration, int xors, int ands, int maxBranching, int crIas, int crs) {
        this.iterations = iterations;
        this.interactions = interactions;
        this.actInteractions = actInteractions;
        this.success = success;
        this.duration = duration;
        this.ands = ands;
        this.xors = xors;
        this.maxBranching = maxBranching;
        this.crIas = crIas;
        this.crs = crs;
    }

}
