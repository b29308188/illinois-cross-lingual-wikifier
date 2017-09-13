package edu.illinois.cs.cogcomp.xlwikifier.nominals;

import edu.illinois.cs.cogcomp.annotation.Annotator;
import edu.illinois.cs.cogcomp.annotation.AnnotatorConfigurator;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Sentence;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.SpanLabelView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.Configurator;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.lbjava.learn.Lexicon;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.ner.ExpressiveFeatures.ExpressiveFeaturesAnnotator;
import edu.illinois.cs.cogcomp.ner.InferenceMethods.Decoder;
import edu.illinois.cs.cogcomp.ner.LbjFeatures.NETaggerLevel1;
import edu.illinois.cs.cogcomp.ner.LbjFeatures.NETaggerLevel2;
import edu.illinois.cs.cogcomp.ner.LbjTagger.*;
import edu.illinois.cs.cogcomp.ner.NERAnnotator;
import edu.illinois.cs.cogcomp.ner.config.NerBaseConfigurator;
import edu.illinois.cs.cogcomp.ner.config.NerOntonotesConfigurator;
import edu.illinois.cs.cogcomp.xlwikifier.MultiLingualNER;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

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
