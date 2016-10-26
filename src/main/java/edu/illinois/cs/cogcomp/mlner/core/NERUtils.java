package edu.illinois.cs.cogcomp.mlner.core;

import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import edu.illinois.cs.cogcomp.xlwikifier.core.Ranker;
import edu.illinois.cs.cogcomp.xlwikifier.core.StopWord;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.WikiCand;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinker;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.WikiCandidateGenerator;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * Created by ctsai12 on 3/2/16.
 */
public class NERUtils {

    private final Logger logger = LoggerFactory.getLogger(NERUtils.class);
    public Set<String> stops;
    public String lang;
    private LangLinker ll = new LangLinker();
    private WikiCandidateGenerator en_wcg = new WikiCandidateGenerator(true);
    private WikiCandidateGenerator wcg;
    private NERFeatureManager fm;
    private Ranker ranker;

    public NERUtils(){
    }

    public NERUtils(String lang){
        setLang(lang);
        wcg = new WikiCandidateGenerator(true);
        ranker = Ranker.loadPreTrainedRanker(lang, ConfigParameters.model_path+"/ranker/ner/" + lang+"/ranker.model");
        ranker.fm.ner_mode = true;
    }

    public void setLang(String lang){
        this.lang = lang;
        stops = StopWord.getStopWords(lang);
        fm = new NERFeatureManager(lang);
    }

    /**
     * Wikify n-grams and generate NER features
     * The main logic of Tsai et al., CoNLL 2016
     * @param doc
     */
    public void wikifyNgrams(QueryDocument doc){
        logger.info("Wikifying n-grams...");

        List<ELMention> prevm = new ArrayList<>();
        for(int n = 4; n >0; n--) {
            doc.mentions = getNgramMentions(doc, n);
            propFeatures(doc, prevm);
            wikifyMentions(doc, n);
            extractNERFeatures(doc);
            prevm = doc.mentions;
        }
    }

    public void extractNERFeatures(QueryDocument doc){
        for(int j = 0; j < doc.mentions.size(); j++){
            ELMention m = doc.mentions.get(j);
            if(m.ner_features.size() > 0)
                continue;
            if(NumberUtils.isNumber(m.getMention().trim()))
                continue;
            Map<String, Double> map = getFeatureMap(doc.mentions, j, true);
            for(String key: map.keySet()){
                m.ner_features.put(key, map.get(key));
            }
        }
    }

    private Map<String, Double> getFeatureMap(List<ELMention> mentions, int idx, boolean train){
        int window = 3;
        List<ELMention> before_mentions = new ArrayList<>();
        List<ELMention> after_mentions = new ArrayList<>();
        ELMention m = mentions.get(idx);
        for(int i = idx - 1; i >=0 && before_mentions.size() < window; i --) {
            before_mentions.add(mentions.get(i));
        }
        for(int i = idx+1; i < mentions.size() && after_mentions.size() < window; i++) {
            after_mentions.add(mentions.get(i));
        }
        return fm.getFeatureMap(m, before_mentions, after_mentions, train);
    }

    public List<ELMention> getNgramMentions(QueryDocument doc, int n){
        List<ELMention> mentions = new ArrayList<>();
        TextAnnotation ta = doc.getTextAnnotation();
        if(ta == null) return mentions;
        for (int i = 0; i < ta.getTokens().length - n + 1; i++) {
            if(ta.getSentenceId(i)!=ta.getSentenceId(i+n-1))
                continue;
            IntPair offset = ta.getTokenCharacterOffset(i);
            IntPair offset1 = ta.getTokenCharacterOffset(i + n -1);
            ELMention m = new ELMention(doc.getDocID(), offset.getFirst(), offset1.getSecond());
            String token = ta.getToken(i);
            int idx = token.indexOf("'");
            if(idx >=0 )  token = token.substring(0, idx);
            String surface = token;
            for(int j = i+1; j < i+n; j++) {
                token = ta.getToken(j);
                idx = token.indexOf("'");
                if(idx >=0 )  token = token.substring(0, idx);
                if(lang.equals("zh"))
                    surface += token;
                else
                    surface += " " + token;
            }
            m.setMention(surface);
            m.ngram = n;
            m.is_stop = false;
            List<String> tokens = Arrays.asList(surface.toLowerCase().split("\\s+"));
            if(tokens.stream().filter(x -> stops.contains(x)).count() == tokens.size())
                m.is_stop = true;
            m.is_ne_gold = false;
            m.setType("O");

            m.is_ne = false;
            m.pred_type = "O";
            mentions.add(m);
        }

        mentions = mentions.stream().sorted((x1, x2) -> Integer.compare(x1.getStartOffset(), x2.getStartOffset()))
                .collect(toList());
        mentions.forEach(x -> x.setLanguage(lang));
        return mentions;
    }

    public void wikifyMentions(QueryDocument doc, int n){
        for(ELMention m: doc.mentions){
            if(!m.getWikiTitle().startsWith("NIL")) continue;
            if(m.ner_features.size()>0) continue;
            String surface = m.getMention().toLowerCase();
            List<WikiCand> cands = wcg.getCandsBySurface(surface, lang);
            if(cands.size() == 0 && n==1 && surface.length()>0){
                String init = m.getMention().substring(0, 1);
                if(!init.toLowerCase().equals(init)) {
                    cands.addAll(wcg.getCandidateByWord(surface, lang, 6));
                }
            }

            if(cands.size() == 0 && !lang.equals("en")){
                if(n > 1){
                    cands.addAll(en_wcg.getCandsBySurface(surface, "en"));
                }
                else if(surface.length()>0){
                    String init = m.getMention().substring(0, 1);
                    if(!init.toLowerCase().equals(init)){   // capitalized
                        cands.addAll(en_wcg.getCandsBySurface(surface, "en"));
                        if(cands.size()==0)
                            cands.addAll(en_wcg.getCandidateByWord(surface, "en", 6));
                    }
                }
            }

            m.setCandidates(cands);
        }

//        ranker.setWikiTitleByModel(doc);
        ranker.setWikiTitleByTopCand(doc);


        // just keep top 2 cands to reduce cache size
        for(ELMention m: doc.mentions){
            List<WikiCand> cands = m.getCandidates();
            m.setCandidates(cands.subList(0, Math.min(cands.size(),2)));
        }
        setMidByWikiTitle(doc);

    }

    public void setMidByWikiTitle(QueryDocument doc){
        for (ELMention m : doc.mentions) {
            if(m.is_ne) continue;

            if (m.getWikiTitle().startsWith("NIL")){
                m.setMid("NIL");
            }

            for (WikiCand c : m.getCandidates()) {
                String mid = getMidByWikiTitle(c.getTitle(), ll, c.lang);
                c.orig_title = c.title;
                if (mid != null)
                    c.title = mid;
                else
                    c.title = "NIL";
            }

            if(m.getCandidates().size()>0){
                m.setMid(m.getCandidates().get(0).title);
            }

        }
    }

    public String getMidByWikiTitle(String title, LangLinker ll, String lang){
        if(title.trim().isEmpty())
            return null;
        if(title.startsWith("NIL"))
            return null;
        String ent = ll.translateToEn(title, lang);
        title = formatTitle(title);
        if(lang.equals("zh")) lang = "zh-cn";

        String mid = FreeBaseQuery.getMidFromTitle(title, lang);
        if(mid != null) return mid; // in the m.12345 format

        if(ent!=null){
            ent = formatTitle(ent);
            mid = FreeBaseQuery.getMidFromTitle(ent, "en");
            if(mid!=null) return mid;
        }
        return null;
    }

    public String formatTitle(String title){
        String tmp = "";
        for(String token: title.split("_")){
            if(token.length()==0) continue;
            tmp+=token.substring(0,1).toUpperCase();
            if(token.length()>1)
                tmp+=token.substring(1,token.length());
            tmp+="_";
        }
        return tmp.substring(0, tmp.length()-1);
    }

    public void propFeatures(QueryDocument doc, List<ELMention> prevms){
        if(prevms == null) return;
        int pidx;
        for(ELMention m: doc.mentions){
            for(pidx = 0 ;pidx < prevms.size(); pidx++){
                ELMention pm = prevms.get(pidx);
                if(pm.getStartOffset() >= m.getEndOffset()) break;

                if(m.getStartOffset() >= pm.getStartOffset() &&
                        m.getEndOffset() <= pm.getEndOffset()){

                    String BI = "";
                    if(m.getStartOffset() > pm.getStartOffset()) BI = "I-";
                    else BI="B-";

                    for(String fname: pm.ner_features.keySet()){
                        if(fname.contains("TOPFBTYPETOKEN-person"))
                            m.ner_features.put(BI+"TOPFBTYPETOKEN-person",1.0);
                        if(fname.contains("TOPFBTYPETOKEN-location"))
                            m.ner_features.put(BI+"TOPFBTYPETOKEN-location",1.0);
                        if(fname.contains("TOPFBTYPETOKEN-organization"))
                            m.ner_features.put(BI+"TOPFBTYPETOKEN-organization",1.0);
                    }
                }
            }
        }
    }
}
