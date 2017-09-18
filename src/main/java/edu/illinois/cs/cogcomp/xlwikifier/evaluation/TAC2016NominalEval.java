package edu.illinois.cs.cogcomp.xlwikifier.evaluation;

import edu.illinois.cs.cogcomp.core.constants.Language;
import edu.illinois.cs.cogcomp.xlwikifier.*;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.WikiCand;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.illinois.cs.cogcomp.xlwikifier.nominals.NominalDetector;
import edu.illinois.cs.cogcomp.xlwikifier.nominals.NominalPerfectDetector;
import edu.illinois.cs.cogcomp.xlwikifier.nominals.NominalUtils;
import edu.illinois.cs.cogcomp.xlwikifier.postprocessing.PostProcessing;
import edu.illinois.cs.cogcomp.xlwikifier.postprocessing.SurfaceClustering;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.MediaWikiSearch;
import net.didion.jwnl.dictionary.database.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.FileUtils;

import java.io.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

/**
 * This class runs MultiLingualNER and CrossLingualWikifier on TAC-KBP 2016 EDL dataset.
 *
 * The paths to the data are specified in config/xlwikifier-tac.config
 *
 * It can be run by "scripts/run-benchmark.sh"
 *
 * Created by ctsai12 on 10/27/16.
 */
public class TAC2016NominalEval {

    private static final String NAME = TAC2016Eval.class.getCanonicalName();

    private static Logger logger = LoggerFactory.getLogger(TAC2016Eval.class);
    private static List<ELMention> golds;

    private static int span_cnt = 0, ner_cnt = 0, link_cnt = 0;
    private static double pred_total = 0;
    private static double gold_total = 0;

    private static int match_nil = 0, has_cand = 0, has_cand1 = 0, incand = 0;

    public static void evaluate(QueryDocument doc){

        List<ELMention> doc_golds = golds.stream().filter(x -> doc.getDocID().startsWith(x.getDocID()))
                .collect(Collectors.toList());

        gold_total += doc_golds.size();
        Map<String, ELMention> nonHitGolds= new HashMap<>();
        doc_golds.stream().forEach(m -> nonHitGolds.put(m.getStartOffset()+":"+m.getEndOffset(), m));

        for(ELMention m: doc.mentions){
            if (nonHitGolds.containsKey(m.getStartOffset()+":"+m.getEndOffset())) {
                span_cnt++;
                ELMention gm = nonHitGolds.get(m.getStartOffset()+":"+m.getEndOffset());
                nonHitGolds.remove(m.getStartOffset()+":"+m.getEndOffset());
                if(m.getType().equals(gm.getType())) {
                    ner_cnt++;

                    // correct KB ID prediction
                    if ((m.getMid().startsWith("NIL") && gm.gold_mid.startsWith("NIL")) ||
                            m.getMid().equals(gm.gold_mid)) {
                        link_cnt++;
                    }

                    // gold is not NIL, wrong prediction, gold is in candidate set
                    if (!gm.gold_mid.startsWith("NIL") && !gm.gold_mid.equals(m.getMid())) {
                        if (m.getCandidates() != null) {
                            Set<String> cands = m.getCandidates().stream().filter(x -> x != null).map(x -> x.title).collect(Collectors.toSet());
                            if (cands.contains(gm.gold_mid)) {
                                System.out.println(doc.getDocID() + " " + m.getSurface() + " " + gm.gold_mid + " " + m.getMid() + " " + m.getType() + " " + gm.getType());
                                for (WikiCand cand : m.getCandidates())
                                    if (cand != null)
                                        System.out.println("\t" + cand.title + " " + cand.orig_title);

                                incand++;
                            }
                        }
                    }

                    // NIL gold, but has candidate
                    if (gm.gold_mid.startsWith("NIL")) {
                        match_nil++;
                        if (m.getCandidates().size() > 0)
                            has_cand++;

                        if (!m.getMid().startsWith("NIL"))
                            has_cand1++;

                    }
                }
            }
        }
        pred_total += doc.mentions.size();
    }


    public static void main(String[] args) throws Exception{

        String language = "zh";
        String config = "config/xlwikifier-tac.config";
        ConfigParameters.setPropValues(config);
        TACDataReader reader = new TACDataReader(false);
        Language lang = null;
        List<QueryDocument> docs = null;

        try {
            if (language.equals("zh")) {
                lang = Language.Chinese;
                docs = reader.read2016ChineseEvalDocs();
                golds = reader.read2016ChineseEvalGoldNOM();
            } else if (language.equals("es")) {
                lang = Language.Spanish;
                docs = reader.read2016SpanishEvalDocs();
                golds = reader.read2016SpanishEvalGoldNOM();
            } else if (language.equals("en")) {
                lang = Language.English;
                docs = reader.read2016EnglishEvalDocs();
                golds = reader.read2016EnglishEvalGoldNOM();
            } else
                logger.error("Unknown language: " + args[0]);
        }
        catch ( IOException e ) {
            e.printStackTrace();
            System.exit(-1);
        }

        MultiLingualNER mlner = MultiLingualNERManager.buildNerAnnotator(lang, config);

        NominalDetector nd = new NominalDetector(String.format("config/nom/%s.tac2016.config", language));

        CrossLingualWikifier xlwikifier = CrossLingualWikifierManager.buildWikifierAnnotator(lang, config);

        // only author mentions are clustered across documents
        List<ELMention> authors = new ArrayList<>();

        for(int i = 0; i < docs.size(); i++){

            QueryDocument doc = docs.get(i);

            logger.info(i+" Working on document: "+doc.getDocID());

            // ner
            mlner.annotate(doc);

            // nom
            nd.annotate(doc);

            // clean mentions contain xml tags
            PostProcessing.cleanSurface(doc);

            // wikification
            xlwikifier.annotate(doc);

            // map plain text offsets to xml offsets
            TACUtils.setXmlOffsets(doc);

            // remove mentions between <quote> and </quote>
            TACUtils.removeQuoteMentions(doc);

            // simple coref to re-set short mentions' title
            PostProcessing.fixPerAnnotation(doc);

            // cluster mentions based on surface forms
            doc.mentions = SurfaceClustering.cluster(doc.mentions);

            // add author mentions inside xml tags
            authors.addAll(TACUtils.getDFAuthors(doc));

            authors.addAll(TACUtils.getNWAuthors(doc));

            NominalUtils.annotateCoref(doc);

            if(golds == null)
                docs.set(i, null);
        }

        authors = SurfaceClustering.clusterAuthors(authors);

        // Evaluate the results if golds is not null.
        // Note that one can always run the official evaluation script on the output file.
        // This is just for convenience.
        if(golds != null) {
            for (QueryDocument doc : docs) {
                doc.mentions.addAll(authors.stream().filter(x -> x.getDocID().equals(doc.getDocID())).collect(Collectors.toList()));
            }
        }

        // only NOM
        for (QueryDocument doc: docs) {
            //authors' noun type is null
            doc.mentions = doc.mentions.stream().filter(m -> m.getNounType() != null && m.getNounType().equals("NOM")).collect(Collectors.toList());
            evaluate(doc);
        }

        System.out.println("#golds: "+gold_total);
        System.out.println("#preds: "+pred_total);
        double rec = span_cnt/gold_total;
        double pre = span_cnt/pred_total;
        double f1 = 2*rec*pre/(rec+pre);
        System.out.print("Mention Span: ");
        System.out.printf("Precision:%.4f Recall:%.4f F1:%.4f\n", pre, rec, f1);

        rec = ner_cnt/gold_total;
        pre = ner_cnt/pred_total;
        f1 = 2*rec*pre/(rec+pre);
        System.out.print("Mention Span + Entity Type: ");
        System.out.printf("Precision:%.4f Recall:%.4f F1:%.4f\n", pre, rec, f1);

        rec = link_cnt/gold_total;
        pre = link_cnt/pred_total;
        f1 = 2*rec*pre/(rec+pre);
        System.out.print("Mention Span + Entity Type + FreeBase ID: ");
        System.out.printf("Precision:%.4f Recall:%.4f F1:%.4f\n", pre, rec, f1);

        System.out.println("#NER matched NIL "+match_nil+", has cand "+has_cand+", non-NIL mid "+has_cand1);
        System.out.println("#Gold MIDs in cands, but not top: "+incand);
    }
}
