package edu.illinois.cs.cogcomp.xlwikifier.nominals;

import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Relation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NominalUtils {

    private final Logger logger = LoggerFactory.getLogger(NominalUtils.class);
    /**
     * Add nominals into named entity clusters
     * @param nams
     * @param noms
     * @return
     */
    public static void annotateCoref(QueryDocument doc){

        boolean use_window = true;
        boolean remove = true;
        int window = 300;
        int nil = 999999999;
        List<ELMention> newm = new ArrayList<>();

        List<ELMention> nes = doc.mentions.stream()
                .filter(m -> m.getNounType().equals("NAM"))
                .sorted(Comparator.comparingInt(ELMention::getStartOffset))
                .collect(Collectors.toList());
        List<ELMention> nos = doc.mentions.stream()
                .filter(m -> m.getNounType().equals("NOM"))
                .sorted(Comparator.comparingInt(ELMention::getStartOffset))
                .collect(Collectors.toList());

        for(ELMention no: nos){
            boolean over = false;
            for(ELMention ne: nes){
                if((no.getStartOffset() >= ne.getStartOffset() && no.getStartOffset() <= ne.getEndOffset())
                        || (no.getEndOffset() >= ne.getStartOffset() && no.getEndOffset() <= ne.getEndOffset())) {
                    over = true;
                    break;
                }
            }
            if(!over)
                nes.add(no);
        }

        nes = nes.stream().sorted(Comparator.comparingInt(ELMention::getStartOffset)).collect(Collectors.toList());

        for(int i = 0; i < nes.size(); i++) {
            ELMention m = nes.get(i);
            if (!m.noun_type.equals("NOM")) continue;
            m.setMid("NIL");
            ELMention prev_nam = null, prev_nom = null;
            for (int j = i - 1; j >= 0; j--) {
                ELMention pm = nes.get(j);
                if (pm == null) continue;
                if (prev_nam == null && pm.noun_type.equals("NAM") && pm.getType().equals(m.getType())) {
                    if (!use_window || m.getStartOffset() - pm.getEndOffset() < window)
                        prev_nam = pm;
                }
                if (prev_nom == null && pm.noun_type.equals("NOM") && pm.getType().equals(m.getType())
                        && pm.getSurface().toLowerCase().equals(m.getSurface().toLowerCase())) {
                    if (!use_window || m.getStartOffset() - pm.getEndOffset() < window) {
                        prev_nom = pm;
                    }
                }
            }

            ELMention next_nam = null;
            for(int j = i+1; j<nes.size(); j++){
                ELMention pm = nes.get(j);
                if (next_nam == null && pm.noun_type.equals("NAM") && pm.getType().equals(m.getType())) {
                    if (!use_window || m.getStartOffset() - pm.getEndOffset() < window)
                        next_nam = pm;
                }
            }

            ELMention closest = prev_nam;
            if(next_nam != null){
                if(closest == null || next_nam.getStartOffset() - m.getEndOffset() < m.getStartOffset() - prev_nam.getEndOffset())
                    closest = next_nam;
            }

            if(closest != null){
                m.setMid(closest.getMid());
                // don't know why 0.9, just feel not confident with nominals
                m.confidence = closest.confidence*0.9;
            } else {
                if (remove) {
                    nes.set(i, null);
                } else {
                    m.setMid("NIL" + nil);
                    nil--;
                }
            }
        }

        for(ELMention m: nes){
            if(m != null)
                newm.add(m);
        }

        doc.mentions.addAll(newm);
    }
}
