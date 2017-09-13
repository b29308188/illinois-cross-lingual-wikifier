package edu.illinois.cs.cogcomp.xlwikifier.nominals;

import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Relation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.curator.CuratorFactory;
import edu.illinois.cs.cogcomp.pos.POSAnnotator;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.cogcomp.md.MentionAnnotator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class NominalUtils {

    private final Logger logger = LoggerFactory.getLogger(NominalUtils.class);
    private final AnnotatorService curatorAnnotator;
    private final POSAnnotator posannotator;
    private final MentionAnnotator mentionAnnotator;

    public NominalUtils() throws Exception {
        curatorAnnotator = CuratorFactory.buildCuratorClient();
        posannotator = new POSAnnotator();
        mentionAnnotator = new MentionAnnotator("ERE_type");
    }

    public boolean isOffsetOverlapped(int xStart, int xEnd, int yStart, int yEnd) {
        return !(xEnd < yStart || yEnd < xStart);
    }

    public void annotateCoref(QueryDocument doc) throws AnnotatorException {

        TextAnnotation ta = doc.getTextAnnotation();

        ta.addView(posannotator);
        mentionAnnotator.addView(ta);
        View mentionView = ta.getView(ViewNames.MENTION);
        List<Constituent> mentions = mentionView.getConstituents();
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
            ELMention m = new ELMention(
                    doc.getDocID(),
                    head.getStartCharOffset(),
                    head.getEndCharOffset()
            );
            m.setSurface(head.getSurfaceForm());
            m.setNounType("NOM");
            doc.mentions.add(m);
        }

        curatorAnnotator.addView(ta, ViewNames.COREF);
        for (Relation r : ta.getView(ViewNames.COREF).getRelations()) {
            ELMention sourceMention = null;
            ELMention targetMention = null;
            Constituent source = r.getSource();
            Constituent target = r.getTarget();
            for (ELMention m : doc.mentions) {
                if (isOffsetOverlapped(source.getStartCharOffset(), source.getEndCharOffset(), m.getStartOffset(), m.getEndOffset())) {
                    sourceMention = m;
                }
                if (isOffsetOverlapped(target.getStartCharOffset(), target.getEndCharOffset(), m.getStartOffset(), m.getEndOffset())) {
                    targetMention = m;
                }
            }
            if (sourceMention != null && targetMention != null && sourceMention != targetMention) {
                if (sourceMention.getEnWikiTitle().equals("NIL") && !targetMention.getEnWikiTitle().equals("NIL") && sourceMention.getNounType().equals("NOM")) {
                    sourceMention.setEnWikiTitle(targetMention.getEnWikiTitle());
                }
                if (!sourceMention.getEnWikiTitle().equals("NIL") && targetMention.getEnWikiTitle().equals("NIL") && targetMention.getNounType().equals("NOM")) {
                    targetMention.setEnWikiTitle(sourceMention.getEnWikiTitle());
                }
            }
        }
    }
}
