package edu.illinois.cs.cogcomp.xlwikifier.nominals;

import edu.illinois.cs.cogcomp.annotation.TextAnnotationBuilder;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.ner.NERAnnotator;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by lchen112 on 9/12/2017.
 */
/**
 * Created by lchen112 on 9/12/2017.
 */
public class NominalDetector extends NERAnnotator {

    // surface -> type
    private Map<String, String> dict = new HashMap<>();
    private TextAnnotationBuilder tokenizer;
    private static Logger logger = LoggerFactory.getLogger(NominalDetector.class);

    public NominalDetector(String configFile) throws IOException {
        super(configFile, "NOM");
        doInitialize();
    }

    public NominalDetector(String configFile, String dictFile, String lang) throws IOException {
        super(configFile, "NOM");
        doInitialize();
        readDict(dictFile);
        tokenizer = MultiLingualTokenizer.getTokenizer(lang);
    }

    public void annotate(QueryDocument doc) {
        TextAnnotation ta = doc.getTextAnnotation();
        addView(ta);
        for (Constituent c : ta.getView(this.viewName).getConstituents()) {
            ELMention m = new ELMention(doc.getDocID(), c.getStartCharOffset(), c.getEndCharOffset());
            m.setSurface(c.getSurfaceForm());
            m.setType(c.getLabel());
            m.setNounType("NOM");
            doc.mentions.add(m);
        }

        if (dict.size() > 0) { // use dict to enhance the recall

            //prevent duplicate
            Set<String> addedMentions = new HashSet<>();
            doc.mentions.stream().forEach(m -> addedMentions.add(m.getStartOffset()+":"+m.getEndOffset()));

            ta = tokenizer.createTextAnnotation(ta.getText());
            for (Constituent c : ta.getView("TOKENS")) {
                if (dict.containsKey(c.getSurfaceForm()) && !addedMentions.contains(c.getStartCharOffset()+":"+c.getEndCharOffset())) {
                    ELMention m = new ELMention(doc.getDocID(), c.getStartCharOffset(), c.getEndCharOffset());
                    m.setSurface(c.getSurfaceForm());
                    m.setType(dict.get(c.getSurfaceForm()));
                    m.setNounType("NOM");
                    doc.mentions.add(m);
                }
            }
        }
    }

    public void readDict(String dictFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(dictFile))) {
            String line;
            String[] tokens;
            while ((line = br.readLine()) != null) {
                try {
                    tokens = line.trim().split("\t");
                    dict.put(tokens[0], tokens[1]);

                }
                catch (Exception e) {
                        logger.warn("Fail when processing" + line);
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException{
        String lang;

        // lang = "en";
        // lang = "es";
        lang = "zh";

        String configFile = String.format("config/nom/%s.tac2016.config", lang);
        String dictFile = String.format("/shared/preprocessed/lchen112/nom-data/%s/dict.tac.2016.train", lang);
        NominalDetector nd = new NominalDetector(configFile, dictFile, lang);
        System.out.println("GG");
    }
}
