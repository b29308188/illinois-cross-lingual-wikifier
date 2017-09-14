package edu.illinois.cs.cogcomp.xlwikifier.evaluation;

import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.pos.POSAnnotator;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.*;
import edu.illinois.cs.cogcomp.xlwikifier.nominals.NominalDetector;

import org.cogcomp.md.MentionAnnotator;

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

        NominalDetector nd = new NominalDetector(configFile);
        double mentionHit = 0.0;
        double mentionTypeHit = 0.0;
        double totalOutput = 0.0;
        for (QueryDocument doc : docs) {
            nd.annotate(doc);
            TACUtils.setXmlOffsets(doc);

            List<ELMention> doc_golds = golds.stream().filter(g -> g.getDocID().equals(doc.getDocID())).collect(Collectors.toList());
            for (ELMention m : doc.mentions){
                for (ELMention gold_m : doc_golds)
                    if (m.getStartOffset() == gold_m.getStartOffset() && m.getEndOffset() == gold_m.getEndOffset()) {
                        mentionHit += 1;
                        if (m.getType().equals(gold_m.getType())) {
                            mentionTypeHit += 1;
                        }
                        break;
                    }
            }
            totalOutput += doc.mentions.size();
        }
        double mp = mentionHit / totalOutput;
        double mr = mentionHit / golds.size();
        double mf1 = 2*mp*mr / (mp + mr);
        double mtp = mentionTypeHit / totalOutput;
        double mtr = mentionTypeHit / golds.size();
        double mtf1 = 2*mtp*mtr / (mtp + mtr);
        System.out.printf("number of gold mentions : %d\n", golds.size());
        System.out.printf("(NOM mention matches) Precision=%f Recall=%f F1=%f\n", mp, mr, mf1);
        System.out.printf("(NOM mention type matches) Precision=%f Recall=%f F1=%f\n", mtp, mtr, mtf1);

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
