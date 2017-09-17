package edu.illinois.cs.cogcomp.xlwikifier.nominals;

import edu.illinois.cs.cogcomp.annotation.BasicTextAnnotationBuilder;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.ner.ExpressiveFeatures.ExpressiveFeaturesAnnotator;
import edu.illinois.cs.cogcomp.ner.LbjTagger.Data;
import edu.illinois.cs.cogcomp.ner.LbjTagger.NERDocument;
import edu.illinois.cs.cogcomp.ner.LbjTagger.NEWord;
import edu.illinois.cs.cogcomp.ner.LbjTagger.ParametersForLbjCode;
import edu.illinois.cs.cogcomp.ner.config.NerBaseConfigurator;
import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static edu.illinois.cs.cogcomp.ner.LbjTagger.LearningCurveMultiDataset.getLearningCurve;
import static edu.illinois.cs.cogcomp.ner.LbjTagger.Parameters.readAndLoadConfig;

/**
 * Created by lchen112 on 9/12/2017.
 */
public class NominalDetectorTrainer {

    private static final Logger logger = LoggerFactory.getLogger(NominalDetectorTrainer.class);

    private static int iteration = 100;

    public static void trainModel(String trainDir, String testDir, String config){

        try {
            NerBaseConfigurator baseConfigurator = new NerBaseConfigurator();
            ResourceManager rm = new ResourceManager(config);
            ParametersForLbjCode.currentParameters = readAndLoadConfig(baseConfigurator.getConfig(rm), true);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        try {
            Data trainData = new Data(trainDir, trainDir, "-c", new String[]{}, new String[]{});
            Data testData = new Data(testDir, testDir, "-c", new String[]{}, new String[]{});
            ExpressiveFeaturesAnnotator.annotate(trainData);
            ExpressiveFeaturesAnnotator.annotate(testData);
            Vector<Data> train=new Vector<>();
            train.addElement(trainData);
            Vector<Data> test=new Vector<>();
            test.addElement(testData);
            getLearningCurve(train, test, iteration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // String lang = "en";
        // String lang = "es";
        String lang = "zh";
        // String trainDir = String.format("/shared/preprocessed/lchen112/nom-data/%s/ere+tac2016train", lang);
        // String testDir = String.format("/shared/preprocessed/lchen112/nom-data/%s/tac2016.test", lang);
        String trainDir = String.format("/shared/preprocessed/lchen112/nom-data/%s/tac2016.train", lang);
        String testDir = String.format("/shared/preprocessed/lchen112/nom-data/%s/tac2016.test", lang);
        //String trainDir = String.format("/shared/preprocessed/lchen112/nom-data/%s/tac2016.all", lang);
        //String testDir = String.format("/shared/preprocessed/lchen112/nom-data/%s/tac2016.all", lang);
        //String config = String.format("config/nom/%s.exp.config", lang);
        String config = String.format("config/nom/%s.tac2016.config", lang);
        trainModel(trainDir, testDir, config);
    }
}
