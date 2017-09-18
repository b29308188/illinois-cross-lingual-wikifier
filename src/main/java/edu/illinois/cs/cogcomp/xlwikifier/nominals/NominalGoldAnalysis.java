package edu.illinois.cs.cogcomp.xlwikifier.nominals;

import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.evaluation.TACDataReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NominalGoldAnalysis {
    private static Logger logger = LoggerFactory.getLogger(NominalGoldAnalysis.class);
    private String lang;
    private List<QueryDocument> docs;
    private List<ELMention> NAMGolds;
    private List<ELMention> NOMGolds;

    public NominalGoldAnalysis(String lang) {
        this.lang = lang;
        readData();
    }

    public void readData() {
        try {
            ConfigParameters.setPropValues("config/xlwikifier-tac.config");
            TACDataReader reader = new TACDataReader(false);
            if (lang.equals("en")) {
                docs = reader.read2016EnglishEvalDocs();
                NAMGolds = reader.read2016EnglishEvalGoldNAM();
                NOMGolds = reader.read2016EnglishEvalGoldNOM();
            } else if (lang.equals("es")) {
                docs = reader.read2016SpanishEvalDocs();
                NAMGolds = reader.read2016SpanishEvalGoldNAM();
                NOMGolds = reader.read2016SpanishEvalGoldNOM();
           } else if (lang.equals("zh")) {
                docs = reader.read2016ChineseEvalDocs();
                NAMGolds = reader.read2016ChineseEvalGoldNAM();
                NOMGolds = reader.read2016ChineseEvalGoldNOM();
            } else
                logger.error("Unknown language: " + lang);
        }
        catch ( IOException e ) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void simpleCounts() {
        int nilCount = 0;
        for (ELMention m : NOMGolds) {
            if (m.getGoldMid().startsWith("NIL")) {
                nilCount += 1;
            }
        }
        Map<String, Long> surfaceCount =
                NOMGolds.stream().collect(Collectors.groupingBy(x -> x.getSurface(), Collectors.counting()));
        Map<String, Long> midCount =
                NOMGolds.stream().collect(Collectors.groupingBy(x -> x.getGoldMid(), Collectors.counting()));
        Map<String, Long> surfaceMidCount =
                NOMGolds.stream().collect(Collectors.groupingBy(x -> x.getSurface() + ":" + x.getGoldMid(), Collectors.counting()));
        System.out.printf("Lang = %s\n", lang);
        System.out.printf("number of mentions = %d\n", NOMGolds.size());
        System.out.printf("number of NILs %d\n", nilCount);
        System.out.printf("number of different surfaces %d\n", surfaceCount.size());
        System.out.printf("number of different MIDs %d\n", midCount.size());
        System.out.printf("number of different surface-mid pair %d\n", surfaceMidCount.size());
    }

    public void NOMLinkToNAM() {
        int corefableCnt = 0;
        int corefableNILCnt = 0;
        int nonCorefableCnt = 0;
        int nonCorefableNILCnt = 0;

        int corefableCntDF = 0;
        int corefableNILCntDF = 0;
        int nonCorefableCntDF = 0;
        int nonCorefableNILCntDF = 0;
        String pDF = null;
        if (lang.equals("en")) {
            pDF = "ENG_DF_";
        }
        else if (lang.equals("es")) {
            pDF = "SPA_DF_";
        }
        else if (lang.equals("zh")) {
            pDF = "CMN_DF_";
        }
        else {
            logger.error("Unknown language: " + lang);
        }
        final String prefixDF = pDF;

        for (QueryDocument doc : docs) {
            Map<String, List<ELMention>> docNAMGolds = NAMGolds.stream()
                    .filter(m -> m.getDocID().equals(doc.getDocID()))
                    .collect(Collectors.groupingBy(ELMention::getGoldMid));
            List<ELMention> docNOMGolds = NOMGolds.stream()
                    .filter(m -> m.getDocID().equals(doc.getDocID()))
                    .collect(Collectors.toList());
            List<ELMention> corefables = docNOMGolds.stream()
                    .filter(m -> docNAMGolds.containsKey(m.getGoldMid()))
                    .collect(Collectors.toList());
            List<ELMention> nonCorefables = docNOMGolds.stream()
                    .filter(m -> !docNAMGolds.containsKey(m.getGoldMid()))
                    .collect(Collectors.toList());
            corefableCnt += corefables.size();
            corefableNILCnt += corefables.stream()
                    .filter(m -> m.getGoldMid().startsWith("NIL"))
                    .collect(Collectors.counting());
            nonCorefableCnt += nonCorefables.size();
            nonCorefableNILCnt += nonCorefables.stream()
                    .filter(m -> m.getGoldMid().startsWith("NIL"))
                    .collect(Collectors.counting());

            corefableCntDF += corefables.stream().filter(m -> m.getDocID().startsWith(prefixDF)).collect(Collectors.counting());
            corefableNILCntDF += corefables.stream()
                    .filter(m -> m.getDocID().startsWith(prefixDF) && m.getGoldMid().startsWith("NIL"))
                    .collect(Collectors.counting());
            nonCorefableCntDF += nonCorefables.stream().filter(m -> m.getDocID().startsWith(prefixDF)).collect(Collectors.counting());
            nonCorefableNILCntDF += nonCorefables.stream()
                    .filter(m -> m.getDocID().startsWith(prefixDF) && m.getGoldMid().startsWith("NIL"))
                    .collect(Collectors.counting());

        }
        System.out.println("Nominals that coref to name entities within the document: ");
        System.out.printf("(Corefable) #nominals = %d(%f%%), #NILs = %d, #nonNILs = %d\n", corefableCnt, (double) corefableCnt / NOMGolds.size() * 100, corefableNILCnt, corefableCnt - corefableNILCnt);
        System.out.printf("(Non-corefable) #nominals = %d(%f%%), #NILs = %d, #nonNILs = %d\n",nonCorefableCnt, (double)nonCorefableCnt / NOMGolds.size() * 100, nonCorefableNILCnt, nonCorefableCnt - nonCorefableNILCnt);

        System.out.println("***Discussion Format***");
        System.out.printf("(Corefable) #nominals = %d, #NILs = %d, #nonNILs = %d\n", corefableCntDF,  corefableNILCntDF, corefableCntDF - corefableNILCntDF);
        System.out.printf("(Non-corefable) #nominals = %d, #NILs = %d, #nonNILs = %d\n",nonCorefableCntDF,  nonCorefableNILCntDF, nonCorefableCntDF - nonCorefableNILCntDF);

        System.out.println("***Non-discussion Format***");
        System.out.printf("(Corefable) #nominals = %d, #NILs = %d, #nonNILs = %d\n", corefableCnt - corefableCntDF, corefableNILCnt - corefableNILCntDF, corefableCnt - corefableCntDF - (corefableNILCnt - corefableNILCntDF));
        System.out.printf("(Non-corefable) #nominals = %d, #NILs = %d, #nonNILs = %d\n",nonCorefableCnt - nonCorefableCntDF,  nonCorefableNILCnt - nonCorefableNILCntDF, nonCorefableCnt - nonCorefableCntDF - (nonCorefableNILCnt - nonCorefableNILCntDF));
    }

    public void typeCounts() {
        Map<String, Long> goldTypeCnt=
                NOMGolds.stream().collect(Collectors.groupingBy(x -> x.getType(), Collectors.counting()));
        goldTypeCnt.keySet().stream()
                .sorted()
                .forEach(x -> System.out.printf("%s->%d(%f%%) ", x, goldTypeCnt.get(x), goldTypeCnt.get(x)*100 / (double)NOMGolds.size()));
        System.out.println("");
    }

    public static void main (String[] args) throws Exception {
         String lang = "en";
        // String lang = "es";
        // String lang = "zh";
        NominalGoldAnalysis analysis = new NominalGoldAnalysis(lang);
        System.out.printf("language = %s, #nominals = %d\n", lang, analysis.NOMGolds.size());
        // analysis.simpleCounts();
        analysis.NOMLinkToNAM();
        // analysis.typeCounts();
    }
}
