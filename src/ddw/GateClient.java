package ddw;

/**
 * Created by Janka on 19.3.2015.
 */
        import gate.Annotation;
        import gate.AnnotationSet;
        import gate.Corpus;
        import gate.CreoleRegister;
        import gate.Document;
        import gate.Factory;
        import gate.FeatureMap;
        import gate.Gate;
        import gate.Node;
        import gate.ProcessingResource;
        import gate.creole.ConditionalSerialAnalyserController;
        import gate.learning.RunMode;
        import gate.util.ExtensionFileFilter;
        import gate.util.GateException;
        import java.io.File;
        import java.io.IOException;
        import java.net.MalformedURLException;
        import java.net.URL;
        import java.util.ArrayList;
        import java.util.HashMap;
        import java.util.logging.Level;
        import java.util.logging.Logger;



public class GateClient {

    // corpus pipeline
    private static ConditionalSerialAnalyserController conditionalCorpusPipelineTraining = null;
    private static ConditionalSerialAnalyserController conditionalCorpusPipelineTesting = null;

    final static String NEGATIVE ="negative";
    final static String NEUTRAL ="neutral";
    final static String POSITIVE ="positive";



    // whether the GATE is initialised
    private static boolean isGateInitilised = false;
    private HashMap<String, HashMap<String, Integer>> confusionMatrix;

    public void run() {

        if(!isGateInitilised){

            initialiseGate();
        }

        confusionMatrix = new HashMap<String, HashMap<String, Integer>>();
        confusionMatrix.put(NEGATIVE, new HashMap<String, Integer>());
        confusionMatrix.put(NEUTRAL, new HashMap<String, Integer>());
        confusionMatrix.put(POSITIVE, new HashMap<String, Integer>());

        RunMode mode=RunMode.TRAINING;
        RunMode modeApplication=RunMode.APPLICATION;

        try {
            Gate.getCreoleRegister().registerDirectories(new File(Gate.getPluginsHome(), "Tools").toURL());
            Gate.getCreoleRegister().registerDirectories(new File(Gate.getPluginsHome(), "Learning").toURL());

            // create an instance of a Document Reset processing resource
            ProcessingResource documentResetPR = (ProcessingResource) Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR");

            // create an instance of a English Tokeniser processing resource
            ProcessingResource tokenizerPR = (ProcessingResource) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser");

            // create an instance of a Sentence Splitter processing resource
            ProcessingResource sentenceSplitterPR = (ProcessingResource) Factory.createResource("gate.creole.splitter.SentenceSplitter");

            // create an instance of a Annotation Set Transfer processing resource
            ProcessingResource annotationSetTransfer = (ProcessingResource) Factory.createResource("gate.creole.annotransfer.AnnotationSetTransfer");

            // create an instance of a POS Tagger processing resource
            ProcessingResource posTagger = (ProcessingResource) Factory.createResource("gate.creole.POSTagger");

            // create an instance of a GATE Morphological Analyser processing resource
            ProcessingResource morphologicalAnalyser = (ProcessingResource) Factory.createResource("gate.creole.morph.Morph");

            // create an instance of a Batch Learning processing resource
            File configFile=new File("D:\\Downloads_data\\module-11-sentiment\\sentiment-exercise\\paum.xml");

            FeatureMap fmForBatchLearning= Factory.newFeatureMap();
            fmForBatchLearning.put("configFileURL",configFile.toURI().toURL());
            fmForBatchLearning.put("learningMode",mode);
            ProcessingResource batchLearning = (ProcessingResource) Factory.createResource("gate.learning.LearningAPIMain",fmForBatchLearning);

            //-------------------------------------------------------------------------TRAINING---------------------------------------------------------------------------------------------

            //parameters setting of ProcessingResources for trainig machine
            documentResetPR.setParameterValue("setsToKeep","Key");
            annotationSetTransfer.setParameterValue("annotationTypes","comment");
            annotationSetTransfer.setParameterValue("copyAnnotations",true);
            annotationSetTransfer.setParameterValue("inputASName","Key");
            annotationSetTransfer.setParameterValue("outputASName","");
            annotationSetTransfer.setParameterValue("tagASName","");
            annotationSetTransfer.setParameterValue("textTagName","");

            batchLearning.setParameterValue("inputASName","");
            batchLearning.setParameterValue("outputASName","");

             // locate the JAPE grammar file
            File japeOrigFile = new File("D:\\Downloads_data\\module-11-sentiment\\sentiment-exercise\\copy_comment_spans.jape");
            java.net.URI japeURI = japeOrigFile.toURI();

            // create feature map for the transducer
            FeatureMap transducerFeatureMap = Factory.newFeatureMap();
            try {
                // set the grammar location
                transducerFeatureMap.put("grammarURL", japeURI.toURL());
                // set the grammar encoding
                transducerFeatureMap.put("encoding", "UTF-8");
            } catch (MalformedURLException e) {
                System.out.println("Malformed URL of JAPE grammar");
                System.out.println(e.toString());
            }

            Corpus corpusTraining = Factory.newCorpus("My XML Files");
            File directory =new File("D:\\Downloads_data\\module-11-sentiment\\sentiment-exercise\\corpora\\trainingMovie");
            ExtensionFileFilter filter = new ExtensionFileFilter("XML files", "xml");
            URL url = null;
            try {
                url = directory.toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            try {
                corpusTraining.populate(url, filter, null, false);
            } catch (IOException e) {
                e.printStackTrace();
            }


            // create an instance of a JAPE Transducer processing resource
            ProcessingResource japeTransducerPR = (ProcessingResource) Factory.createResource("gate.creole.Transducer", transducerFeatureMap);

            // create corpus pipeline
            conditionalCorpusPipelineTraining = (ConditionalSerialAnalyserController) Factory.createResource("gate.creole.ConditionalSerialAnalyserController");

            // add the processing resources (modules) to the pipeline
            conditionalCorpusPipelineTraining.add(documentResetPR);
            conditionalCorpusPipelineTraining.add(annotationSetTransfer);
            conditionalCorpusPipelineTraining.add(tokenizerPR);
            conditionalCorpusPipelineTraining.add(sentenceSplitterPR);
            conditionalCorpusPipelineTraining.add(posTagger);
            conditionalCorpusPipelineTraining.add(morphologicalAnalyser);
            conditionalCorpusPipelineTraining.add(batchLearning);

            // set the corpus to the pipeline
            conditionalCorpusPipelineTraining.setCorpus(corpusTraining);

            //run the pipeline
            conditionalCorpusPipelineTraining.execute();
            //------------------------------------------------------------TESTING-----------------------------------------------------------------------------------------------------------

            //creating testing corpus
            Corpus corpusTesting = Factory.newCorpus("My XML Files");
            File directoryTesting =new File("D:\\Downloads_data\\module-11-sentiment\\sentiment-exercise\\corpora\\testingMovie");
            ExtensionFileFilter filterTesting = new ExtensionFileFilter("XML files", "xml");
            URL urlTesting = null;
            try {
                urlTesting = directoryTesting.toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            try {
                corpusTesting.populate(urlTesting, filterTesting, null, false);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // create corpus pipeline
            conditionalCorpusPipelineTesting = (ConditionalSerialAnalyserController) Factory.createResource("gate.creole.ConditionalSerialAnalyserController");

            //setting for testing
            japeTransducerPR.setParameterValue("inputASName","Key");
            japeTransducerPR.setParameterValue("outputASName","");
            batchLearning.setParameterValue("inputASName","");
            batchLearning.setParameterValue("learningMode",modeApplication);
            batchLearning.setParameterValue("outputASName","Output");

            // add the processing resources (modules) to the pipeline
            conditionalCorpusPipelineTesting.add(documentResetPR);
            //conditionalCorpusPipelineTesting.add(annotationSetTransfer);
            conditionalCorpusPipelineTesting.add(japeTransducerPR);
            conditionalCorpusPipelineTesting.add(tokenizerPR);
            conditionalCorpusPipelineTesting.add(sentenceSplitterPR);
            conditionalCorpusPipelineTesting.add(posTagger);
            conditionalCorpusPipelineTesting.add(morphologicalAnalyser);
            conditionalCorpusPipelineTesting.add(batchLearning);

            // set the corpus to the pipeline
            conditionalCorpusPipelineTesting.setCorpus(corpusTesting);

            //run the pipeline
            conditionalCorpusPipelineTesting.execute();
            //-------------------------------------------------------------RESULT-----------------------------------------------------------------


            for(int i=0; i< corpusTesting.size(); i++) {

                Document document = corpusTesting.get(i);
                AnnotationSet key = document.getAnnotations("Key");
                AnnotationSet output = document.getAnnotations("Output");
                FeatureMap futureMapKey = null;
                FeatureMap futureMapOutput = null;

                AnnotationSet annSetKey = key.get("comment", futureMapKey);
                AnnotationSet annSetOutput = output.get("comment", futureMapOutput);
                // System.out.println("Number of Key Comment annotations: " + annSetKey.size());

                ArrayList keyCommentAnnotations = new ArrayList(annSetKey);
                ArrayList outputCommentAnnotations = new ArrayList(annSetOutput);

                for (int j = 0; j < keyCommentAnnotations.size(); ++j) {
                    System.out.println("***********************************************************************************************************************");

                    // get a token annotation
                    Annotation keyComment = (Annotation) keyCommentAnnotations.get(j);
                    Annotation outputComment = (Annotation) outputCommentAnnotations.get(j);

                    // get the features of the token
                    FeatureMap annFM = keyComment.getFeatures();
                    FeatureMap annFMOutput = outputComment.getFeatures();

                    // get the underlying string for the Token
                    Node isaStart = keyComment.getStartNode();
                    Node isaEnd = keyComment.getEndNode();
                    String underlyingString = document.getContent().getContent(isaStart.getOffset(), isaEnd.getOffset()).toString();
                    System.out.println("Comment: " + underlyingString);


                    // get the value of the "string" feature
                    String valueKey = (String) annFM.get("rating");
                    String valueOutput = (String) annFMOutput.get("rating");


                    HashMap<String, Integer> row = confusionMatrix.get(valueKey);
                    Integer current = row.getOrDefault(valueOutput, 0);
                    row.put(valueOutput, current + 1);

                    System.out.println("My opinion: " + valueKey);
                    System.out.println("Machine opinion: " + valueOutput);

                }
            }



            System.out.println("        NEGATIVE   NEUTRAL   POSITIVE");
            PrintRow(NEGATIVE, "NEGATIVE");
            PrintRow(NEUTRAL, "NEUTRAL ");
            PrintRow(POSITIVE, "POSITIVE");

            double sum=0;
            double error=0;
            sum+= CalculateError(NEGATIVE);
            sum+= CalculateError(NEUTRAL);
            sum+= CalculateError(POSITIVE);
            error= Math.sqrt(sum)/corpusTesting.size();

            System.out.println("ERROR RATE: " + error);

        } catch (GateException ex) {
            Logger.getLogger(GateClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (MalformedURLException ex)
        {
            Logger.getLogger(GateClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }



    private double CalculateError(String rowIdentificator) {
        HashMap<String, Integer> row = confusionMatrix.get(rowIdentificator);
       if (rowIdentificator=="negative") {
           return ((row.getOrDefault(NEUTRAL, 0)*1) + (row.getOrDefault(POSITIVE, 0)*4));
       }
        else if (rowIdentificator=="neutral") {
            return ((row.getOrDefault(NEGATIVE, 0)*1) + (row.getOrDefault(POSITIVE, 0)*1));
        }else {
           return ((row.getOrDefault(NEGATIVE, 0)*4) + (row.getOrDefault(NEUTRAL, 0)*1));
       }

    }
    private void PrintRow(String rowIdentificator, String rowName) {
        HashMap<String, Integer> row = confusionMatrix.get(rowIdentificator);
        System.out.print(rowName);
        System.out.printf("\t%d         %d         %d", row.getOrDefault(NEGATIVE, 0), row.getOrDefault(NEUTRAL, 0), row.getOrDefault(POSITIVE, 0));
        System.out.println();
    }

    private void initialiseGate() {

        try {
            // set GATE home folder
            // Eg. /Applications/GATE_Developer_7.0
            File gateHomeFile = new File("C:\\Program Files\\GATE_Developer_8.0");
            Gate.setGateHome(gateHomeFile);

            // set GATE plugins folder
            // Eg. /Applications/GATE_Developer_7.0/plugins
            File pluginsHome = new File("C:\\Program Files\\GATE_Developer_8.0\\plugins");
            Gate.setPluginsHome(pluginsHome);

            // set user config file (optional)
            // Eg. /Applications/GATE_Developer_7.0/user.xml
            Gate.setUserConfigFile(new File("C:\\Program Files\\GATE_Developer_8.0", "user.xml"));

            // initialise the GATE library
            Gate.init();

            // load ANNIE plugin
            CreoleRegister register = Gate.getCreoleRegister();
            URL annieHome = new File(pluginsHome, "ANNIE").toURL();
            register.registerDirectories(annieHome);

            // flag that GATE was successfuly initialised
            isGateInitilised = true;

        } catch (MalformedURLException ex) {
            Logger.getLogger(GateClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GateException ex) {
            Logger.getLogger(GateClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}