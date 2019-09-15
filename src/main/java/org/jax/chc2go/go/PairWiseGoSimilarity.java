package org.jax.chc2go.go;

import com.google.common.collect.Sets;
import org.jax.chc2go.chc.ChcInteraction;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.formats.go.GoGaf21Annotation;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.io.obo.go.GoGeneAnnotationParser;
import org.monarchinitiative.phenol.ontology.algo.InformationContentComputation;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermAnnotation;
import org.monarchinitiative.phenol.analysis.*;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermIds;
import org.monarchinitiative.phenol.ontology.similarity.PairwiseResnikSimilarity;
import org.monarchinitiative.phenol.ontology.similarity.ResnikSimilarity;

import java.io.File;
import java.util.*;

public class PairWiseGoSimilarity {

    private final List<ChcInteraction> chcInteractionList;
    /**  go.obo file */
    private final File goOboFile;
    /** GoGaf file. */
    private final File goGafFile;

    private Ontology geneOntology;

    private AssociationContainer associationContainer;

    private ResnikSimilarity resnikSimilarity;

    PairwiseResnikSimilarity pairwiseSimilarity;

    private Map<TermId, Double> icMap;

    private Map<String,TermId> geneSymbol2TermId;

    private Map<TermId, Collection<TermId>> geneIdToTermIds;
    private Map<TermId, Collection<TermId>> termIdToGeneIds;

    private int numThreads = 2;


    public PairWiseGoSimilarity(List<ChcInteraction> interactions, String goObo, String goGaf) {
        chcInteractionList = interactions;
        goOboFile = new File(goObo);
        goGafFile = new File(goGaf);
        if (! goOboFile.exists()) {
            throw new RuntimeException("Could not find go.obo file");
        }
        if (! goGafFile.exists()) {
            throw new RuntimeException("Could not find go.gaf file");
        }
        System.out.println("[INFO] Parsing GO data.....");
        geneOntology = OntologyLoader.loadOntology(goOboFile, "GO");
        int n_terms = geneOntology.countAllTerms();
        System.out.println("[INFO] parsed " + n_terms + " GO terms.");
        System.out.println("[INFO] parsing  " + goGafFile.getAbsolutePath());
        try {
            final GoGeneAnnotationParser annotparser = new GoGeneAnnotationParser(goGafFile.getAbsolutePath());
            List<TermAnnotation> goAnnots = annotparser.getTermAnnotations();
            System.out.println("[INFO] parsed " + goAnnots.size() + " GO annotations.");
            associationContainer = new AssociationContainer(goAnnots);
            int n = associationContainer.getTotalNumberOfAnnotatedTerms();
            System.out.println("[INFO] parsed " + n + " annotated terms");
        } catch (PhenolException e) {
            e.printStackTrace();
            System.out.println("[NOTE] The GAF file needs to be unzipped");
            return;
        }
        try {
            prepareResnik();
        } catch (PhenolException e) {
            e.printStackTrace();
        }
        prepareGeneSymbolMapping();
        testAllInteractions();
    }

    /**
     * e.g., map KMT2B to UniProtKB:Q9BV73
     */
    private void prepareGeneSymbolMapping() {
        geneSymbol2TermId = new HashMap<>();
        for (TermId geneId : associationContainer.getAllAnnotatedGenes()) {
            try {
                ItemAssociations iassoc = associationContainer.get(geneId);
                Iterator it = iassoc.iterator();
                if (it.hasNext()) {
                    GoGaf21Annotation gaf = (GoGaf21Annotation) iassoc.iterator().next();
                    String dbObjectSymbol = gaf.getDbObjectSymbol();
                    TermId dbObjectTermId = gaf.getDbObjectTermId();
                    //System.out.println(dbObjectSymbol +": " + dbObjectTermId.getValue());
                    geneSymbol2TermId.put(dbObjectSymbol,dbObjectTermId);
                    continue;
                }
            } catch (PhenolException e) {
                e.printStackTrace();
            }
        }
    }




    private void prepareResnik() throws PhenolException {
        // Compute list of annoations and mapping from OMIM ID to term IDs.
        this.geneIdToTermIds = new HashMap<>();
        this.termIdToGeneIds = new HashMap<>();

        for (TermId geneId : this.associationContainer.getAllAnnotatedGenes()) {
            //System.out.println(geneId);
            ItemAssociations iassocs = this.associationContainer.get(geneId);
            List<TermId> goTerms = iassocs.getAssociations();
            geneIdToTermIds.putIfAbsent(geneId, new HashSet<>());
            final Set<TermId> inclAncestorTermIds = TermIds.augmentWithAncestors(geneOntology, Sets.newHashSet(goTerms), true);
            for (TermId goId : inclAncestorTermIds) {
                termIdToGeneIds.putIfAbsent(goId, new HashSet<>());
                termIdToGeneIds.get(goId).add(geneId);
                geneIdToTermIds.get(geneId).add(goId);
            }
        }
        System.out.printf("[INFO] termIdToGeneIds n=%d\n",termIdToGeneIds.size());
        System.out.printf("[INFO] geneIdToTermIds n=%d\n",geneIdToTermIds.size());
        // Compute information content of HPO terms, given the term-to-disease annotation.
        System.out.println("[INFO] Performing IC precomputation...");
        this.icMap =
                new InformationContentComputation(geneOntology)
                        .computeInformationContent(termIdToGeneIds);
        System.out.println("[INFO] DONE: Performing IC precomputation. Size of IC map is: " + icMap.size());
        // Setup PairwiseResnikSimilarity to use for computing scores.
        this.pairwiseSimilarity =
                new PairwiseResnikSimilarity(geneOntology, icMap);
    }


    private double getSimilarity(String geneA, String geneB) {
        TermId tidA = this.geneSymbol2TermId.get(geneA);
        TermId tidB = this.geneSymbol2TermId.get(geneB);
        if (tidA == null || tidB == null) {
            return 0.0;
        }
        Collection<TermId> goTidA = this.geneIdToTermIds.get(geneA);
        Collection<TermId> goTidB = this.geneIdToTermIds.get(geneB);
        if (goTidA==null || goTidB==null) return 0.0;
        double sum = 0.0;
        int c = 0;
        for (TermId a : goTidA) {
            for (TermId b : goTidB) {
                double sim = this.pairwiseSimilarity.computeScore(a,b);
                sum += sim;
                c++;
            }
        }
        if (c==0) return 0.0;
        return sum/(double)c;
    }


    private void testInteraction(ChcInteraction chci) {
        List<String> genelistA = chci.getGenelistA();
        List<String> genelistB = chci.getGenelistB();
        String maxGeneA = null;
        String maxGeneB = null;
        double maxSim = -1.0;
        for (String a : genelistA) {
            for (String b : genelistB) {
                double d = getSimilarity(a,b);
                System.out.printf("%s <-> %s: %.2f\n",a,b,d);
            }
        }
    }

    private void testAllInteractions() {
        for (ChcInteraction chci : this.chcInteractionList) {
            testInteraction(chci);
        }
    }

}
