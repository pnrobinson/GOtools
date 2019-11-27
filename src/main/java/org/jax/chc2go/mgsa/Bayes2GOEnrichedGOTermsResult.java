package org.jax.chc2go.mgsa;


import org.monarchinitiative.phenol.analysis.AssociationContainer;
import org.monarchinitiative.phenol.analysis.StudySet;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Cares about the result of the b2g approach.
 *
 * @author Sebastian Bauer
 */
public class Bayes2GOEnrichedGOTermsResult
{
    private Bayes2GOScore score;

    /** A linear list containing properties for go terms */
    protected List<AbstractGOTermProperties> list = new ArrayList<AbstractGOTermProperties>();

    /** Maps the go term to an integer (for accesses in constant time) */
    private Map<TermId, Integer> term2Index = new HashMap<>();

    /** The current index for adding a new go term property */
    private int index = 0;

    /** The GO Graph */
    protected Ontology go;

    /** The association container */
    private AssociationContainer associations;

    private final int populationGeneCount;

    private final StudySet studySet;

    double p;
    double p_adjusted;
    double p_min;
    double marg;


    /* FIXME: Remove this */
   // private IntMapper<TermId> termMapper;

    public Bayes2GOEnrichedGOTermsResult(Ontology go,
                                         AssociationContainer associations, StudySet studySet,
                                         int populationGeneCount)
    {
        this.go = go;
        this.associations = associations;
        this.studySet = studySet;
        this.populationGeneCount = populationGeneCount;
    }

    public void setScore(Bayes2GOScore score)
    {
        this.score = score;
    }

    public Bayes2GOScore getScore()
    {
        return score;
    }

//    public void setTermMapper(IntMapper<TermID> termMapper)
//    {
//        this.termMapper = termMapper;
//    }
//
//    public IntMapper<TermID> getTermMapper()
//    {
//        return termMapper;
//    }
}
