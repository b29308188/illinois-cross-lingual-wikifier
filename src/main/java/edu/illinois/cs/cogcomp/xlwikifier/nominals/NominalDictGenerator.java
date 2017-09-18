package edu.illinois.cs.cogcomp.xlwikifier.nominals;

import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.WikiDocReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class NominalDictGenerator {
    private static Logger logger = LoggerFactory.getLogger(NominalDictGenerator.class);

    public Set<String> tagNominals(List<QueryDocument> docs, String configFile, String lang) throws Exception{

        Set<String> nominalsAndTypes = new HashSet<String>();
        NominalDetector nd = new NominalDetector(configFile);
        for (QueryDocument doc : docs) {
            nd.annotate(doc);
            for (ELMention m : doc.mentions) {
                if (m.getNounType() != null && m.getNounType().equals("NOM"))
                    nominalsAndTypes.add(m.getSurface()+"\t"+m.getType());
            }
        }
        return nominalsAndTypes;
    }

    public void writeNominalDict(Set<String> nominalsAndTypes, String outputFile) {
        try{
            PrintWriter writer = new PrintWriter(outputFile, "UTF-8");
            for(String nt : nominalsAndTypes) {
                writer.println(nt);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws Exception {
        // String lang = "en";
        // String lang = "es";
        String lang = "zh";
        ConfigParameters.setPropValues("config/xlwikifier-tac.config");
        WikiDocReader r = new WikiDocReader();
        List<QueryDocument> docs = r.readWikiDocs(lang, 50000);

        NominalDictGenerator ndg = new NominalDictGenerator();
        String configFile = String.format("config/nom/%s.tac2016.config", lang);
        String outputFile = String.format("/shared/preprocessed/lchen112/nom-data/%s/dict.tac2016.train", lang);
        Set<String> nominalsAndTypes = ndg.tagNominals(docs, configFile, lang);
        ndg.writeNominalDict(nominalsAndTypes, outputFile);
    }
}
