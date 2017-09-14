package edu.illinois.cs.cogcomp.xlwikifier.nominals;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.ner.NERAnnotator;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;

import java.io.IOException;

/**
 * Created by lchen112 on 9/12/2017.
 */
/**
 * Created by lchen112 on 9/12/2017.
 */
public class NominalDetector extends NERAnnotator {
    public NominalDetector(String configFile) throws IOException {
        super(configFile, "NOM");
        doInitialize();
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
    }
}
