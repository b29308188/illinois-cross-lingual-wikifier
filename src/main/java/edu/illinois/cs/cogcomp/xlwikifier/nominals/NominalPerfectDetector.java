package edu.illinois.cs.cogcomp.xlwikifier.nominals;

import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.evaluation.TAC2016NominalMDEval;
import edu.illinois.cs.cogcomp.xlwikifier.evaluation.TACDataReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class NominalPerfectDetector {
    private static Logger logger = LoggerFactory.getLogger(TAC2016NominalMDEval.class);
    private List<ELMention> golds = null;

    public NominalPerfectDetector(String lang) throws IOException{
        ConfigParameters.setPropValues("config/xlwikifier-tac.config");
        TACDataReader reader = new TACDataReader(false);
        if (lang.equals("en")) {
            golds = reader.read2016EnglishEvalGoldNOM();
        }
        else if (lang.equals("es")) {
            golds = reader.read2016SpanishEvalGoldNOM();
        }
        else if (lang.equals("zh")) {
            golds = reader.read2016ChineseEvalGoldNOM();
        }
        else {
            logger.error("unknown language");
            System.exit(-1);
        }
    }
    public void annotate(QueryDocument doc) {
        List<ELMention> doc_golds = golds.stream().filter(g -> g.getDocID().equals(doc.getDocID())).collect(Collectors.toList());
        doc.mentions.addAll(doc_golds);
    }
}
