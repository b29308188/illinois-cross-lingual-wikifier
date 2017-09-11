package edu.illinois.cs.cogcomp.xlwikifier.evaluation;

import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.pos.POSAnnotator;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.*;

import org.cogcomp.md.MentionAnnotator;

import java.util.List;
import java.util.stream.Collectors;




public class NominalMentionEval {
    public static void main(String[] args) throws Exception {
        ConfigParameters.setPropValues("config/xlwikifier-tac.config");
        TACDataReader reader = new TACDataReader(false);
        List<QueryDocument> docs = reader.read2016EnglishEvalDocs();
        List<ELMention> golds = reader.read2016EnglishEvalGoldNOM();
        POSAnnotator posannotator = new POSAnnotator();
        MentionAnnotator mentionAnnotator = new MentionAnnotator("ERE_type");
        double mentionHit = 0.0;
        double mentionTypeHit = 0.0;
        double totalOutput = 0.0;
        for (QueryDocument doc : docs) {
            TextAnnotation ta = doc.getTextAnnotation();
            ta.addView(posannotator);
            mentionAnnotator.addView(ta);
            View mentionView = ta.getView(ViewNames.MENTION);
            List<Constituent> mentions = mentionView.getConstituents();
            List<ELMention> doc_golds = golds.stream().filter(g -> g.getDocID().equals(doc.getDocID())).collect(Collectors.toList());
            mentions = mentions
                    .stream()
                    .filter(
                            x -> x.getAttribute("EntityMentionType").equals("NOM")
                                    && ( x.getAttribute("EntityType").equals("GPE")
                                    ||  x.getAttribute("EntityType").equals("ORG")
                                    ||  x.getAttribute("EntityType").equals("PER")
                                    ||  x.getAttribute("EntityType").equals("LOC")
                                    ||  x.getAttribute("EntityType").equals("FAC")
                            )
                    )
                    .collect(Collectors.toList());
            for (Constituent extent : mentions){
                //Get the head if needed
                Constituent head = MentionAnnotator.getHeadConstituent(extent, "MENTION_HEAD");

                for (ELMention gold_m : doc_golds)
                    if (head.getStartCharOffset() == gold_m.getStartOffset() && head.getEndCharOffset() == gold_m.getEndOffset()) {
                        mentionHit += 1;
                        if (head.getAttribute("EntityType").equals(gold_m.getType())) {
                            mentionTypeHit += 1;
                        }
                        break;
                    }
            }
            totalOutput += mentions.size();
        }
        double mp = mentionHit / totalOutput;
        double mr = mentionHit / golds.size();
        double mf1 = 2*mp*mr / (mp + mr);
        double mtp = mentionTypeHit / totalOutput;
        double mtr = mentionTypeHit / golds.size();
        double mtf1 = 2*mtp*mr / (mtp + mtr);
        System.out.printf("(NOM mention matches) Precision=%f Recall=%f F1=%f\n", mp, mr, mf1);
        System.out.printf("(NOM mention type matches) Precision=%f Recall=%f F1=%f\n", mtp, mtr, mtf1);
    }
}
