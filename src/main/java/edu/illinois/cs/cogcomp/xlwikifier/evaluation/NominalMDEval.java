package edu.illinois.cs.cogcomp.xlwikifier.evaluation;

import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.*;
import edu.illinois.cs.cogcomp.xlwikifier.nominals.NominalDetector;

import java.util.List;
import java.util.*;
import java.util.stream.Collectors;


import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by lchen112 on 9/12/17.
 */

public class NominalMDEval {
    private static Logger logger = LoggerFactory.getLogger(NominalMDEval.class);
    private static String[] TYPES = {"FAC", "GPE", "LOC", "ORG", "PER"};

    public static void evalauteTAC(String pathToTacDir, String configFile, String lang) throws Exception{

        File folder = new File(pathToTacDir);
        Set<String> testDocs = new HashSet<String>();
        for (File f : folder.listFiles()) {
            testDocs.add(f.getName());
        }
        TACDataReader reader = new TACDataReader(false);

        List<QueryDocument> docs = null;
        List<ELMention> golds = null;
        if (lang.equals("en")) {
            docs = reader.read2016EnglishEvalDocs();
            golds = reader.read2016EnglishEvalGoldNOM();
        }
        else if (lang.equals("es")) {
            docs = reader.read2016SpanishEvalDocs();
            golds = reader.read2016SpanishEvalGoldNOM();
        }
        else if (lang.equals("zh")) {
            docs = reader.read2016ChineseEvalDocs();
            golds = reader.read2016ChineseEvalGoldNOM();
        }
        else {
            logger.error("unknown language");
            System.exit(-1);
        }
        golds = golds.stream().filter( g -> testDocs.contains(g.getDocID())).collect(Collectors.toList());
        docs = docs.stream().filter(doc -> testDocs.contains(doc.getDocID())).collect(Collectors.toList());

        double mentionHit = 0.0;
        double mentionTypeHit = 0.0;
        double totalOutput = 0.0;
        Map<String, Double> hitsPerType = new HashMap<>();
        Arrays.stream(TYPES).forEach(t -> hitsPerType.put(t, 0.0));
        Map<String, Double> outputsPerType = new HashMap<>();
        Arrays.stream(TYPES).forEach(t -> outputsPerType.put(t, 0.0));
        Map<String, Double> outputsPerTypeThatMentionHit = new HashMap<>();
        Arrays.stream(TYPES).forEach(t -> outputsPerTypeThatMentionHit.put(t, 0.0));
        NominalDetector nd = new NominalDetector(configFile);
        for (QueryDocument doc : docs) {

            nd.annotate(doc);
            TACUtils.setXmlOffsets(doc);

            List<ELMention> doc_golds = golds.stream().filter(g -> g.getDocID().equals(doc.getDocID())).collect(Collectors.toList());
            for (ELMention m : doc.mentions){
                for (ELMention gold_m : doc_golds) {
                    if (m.getStartOffset() == gold_m.getStartOffset() && m.getEndOffset() == gold_m.getEndOffset()) {
                        mentionHit += 1;
                        outputsPerTypeThatMentionHit.put(m.getType(),outputsPerTypeThatMentionHit.get(m.getType())+1);
                        if (m.getType().equals(gold_m.getType())) {
                            mentionTypeHit += 1;
                            hitsPerType.put(m.getType(),hitsPerType.get(m.getType())+1);
                        }
                        break;
                    }
                }
                outputsPerType.put(m.getType(),outputsPerType.get(m.getType())+1);
                totalOutput += 1;
            }
        }

        System.out.printf("language = %s, # all nominal mentions: %d\n", lang, golds.size());

        double p, r, f1;
        p = mentionHit / totalOutput;
        r = mentionHit / golds.size();
        f1 = 2*p*r / (p + r);
        System.out.printf("(mention offset matches) Precision=%f, Recall=%f, F1=%f\n", p, r, f1);

        p = mentionTypeHit / totalOutput;
        r = mentionTypeHit / golds.size();
        f1 = 2*p*r / (p + r);
        System.out.printf("(mention offset + type matches) Precision=%f, Recall=%f, F1=%f\n", p, r, f1);

        for(String t : TYPES) {
            long totalTypeGolds = golds.stream().filter(g -> g.getType().equals(t)).collect(Collectors.counting());
            p = outputsPerTypeThatMentionHit.get(t) / outputsPerType.get(t);
            r = outputsPerTypeThatMentionHit.get(t) / totalTypeGolds;
            f1 = 2*p*r / (p + r);
            System.out.printf("Type %s, #mentions = %d, ", t, totalTypeGolds);
            System.out.printf("Precision=%f, Recall=%f, F1=%f \n", p , r, f1);
        }

    }
    public static void main(String[] args) throws Exception {
        // String lang = "en";
        // String lang = "es";
        String lang = "zh";
        String testDir = String.format("/shared/preprocessed/lchen112/nom-data/%s/tac2016.test", lang);
        String configFile = String.format("config/nom/%s.tac2016.config", lang);
        ConfigParameters.setPropValues("config/xlwikifier-tac.config");
        evalauteTAC(testDir, configFile, lang);
    }
}
