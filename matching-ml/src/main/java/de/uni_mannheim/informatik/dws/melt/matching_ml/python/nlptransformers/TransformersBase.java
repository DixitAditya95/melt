package de.uni_mannheim.informatik.dws.melt.matching_ml.python.nlptransformers;

import de.uni_mannheim.informatik.dws.melt.matching_base.FileUtil;
import de.uni_mannheim.informatik.dws.melt.matching_jena.MatcherYAAAJena;
import java.io.File;
import de.uni_mannheim.informatik.dws.melt.matching_jena.TextExtractor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a base class for all Transformers.
 * It just contains some variables and getter and setters.
 */
public abstract class TransformersBase extends MatcherYAAAJena {


    private static final Logger LOGGER = LoggerFactory.getLogger(TransformersBase.class);

    protected TextExtractor extractor;
    protected String modelName;
    
    protected TransformersTrainerArguments trainingArguments;
    protected boolean usingTensorflow;
    protected String cudaVisibleDevices;
    protected File transformersCache;
    protected TransformersMultiProcessing multiProcessing;
    protected boolean multipleTextsToMultipleExamples;

    /**
     * Constructor with all required parameters.
     * @param extractor the extractor to select which text for each resource should be used.
     * @param modelName the model name which can be a model id (a hosted model on huggingface.co) or a path to a directory containing a model and tokenizer
     * (<a href="https://huggingface.co/transformers/main_classes/model.html#transformers.PreTrainedModel.from_pretrained">
     * see first parameter pretrained_model_name_or_path of the from_pretrained
     * function in huggingface library</a>). In case of a path, it should be absolute.
     * The path can be generated by e.g. {@link FileUtil#getCanonicalPathIfPossible(java.io.File) }
     */
    public TransformersBase(TextExtractor extractor, String modelName) {
        this.extractor = extractor;
        this.modelName = modelName;
        
        //set useful defaults
        this.trainingArguments = new TransformersTrainerArguments();
        this.usingTensorflow = false;
        this.cudaVisibleDevices = ""; //use all GPUs
        this.transformersCache = null; //use default
        this.multiProcessing = TransformersMultiProcessing.SPAWN;
        this.multipleTextsToMultipleExamples = false;
    }
    
    /**
     * Returns the text extractor which extracts text from a given resource. This is the text which represents a resource.
     * @return the text extractor
     */
    public TextExtractor getExtractor() {
        return extractor;
    }

    /**
     * Sets the extractor which computes the text from a given resource. 
     * This is the text which represents a resource.
     * @param extractor the text extractor
     */
    public void setExtractor(TextExtractor extractor) {
        this.extractor = extractor;
    }

    /**
     * Returns the model name which can be a model id (a hosted model on huggingface.co) or a path to a directory containing a model and tokenizer
     * (<a href="https://huggingface.co/transformers/main_classes/model.html#transformers.PreTrainedModel.from_pretrained">
     * see first parameter pretrained_model_name_or_path of the from_pretrained
     * function in huggingface library</a>)
     * @return the model name as a string
     */
    public String getModelName() {
        return modelName;
    }
    
    /**
     * Sets the model name which can be a model id (a hosted model on huggingface.co) or a path to a directory containing a model and tokenizer
     * (<a href="https://huggingface.co/transformers/main_classes/model.html#transformers.PreTrainedModel.from_pretrained">
     * see first parameter pretrained_model_name_or_path of the from_pretrained
     * function in huggingface library</a>). In case of a path, it should be abolute. 
     * The path can be generated by e.g. {@link FileUtil#getCanonicalPathIfPossible(java.io.File) }
     * @param modelName the model name as a string
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /**
     * Returns the training arguments of the huggingface trainer.
     * Any of the training arguments can be used. For further documentation, see{@link TransformersTrainerArguments}
     * @return the transformer location
     */
    public TransformersTrainerArguments getTrainingArguments() {
        return trainingArguments;
    }

    /**
     * Sets the training arguments of the huggingface trainer.
     * Any of the training arguments can be used. For further documentation, see{@link TransformersTrainerArguments}
     * @param configuration the trainer configuration
     */
    public void setTrainingArguments(TransformersTrainerArguments configuration) {
        this.trainingArguments = configuration;
    }

    /**
     * Returns a boolean value if tensorflow is used to train the model.
     * If true, the models are run with tensorflow. If false, pytorch is used.
     * @return true, if tensorflow is used. false, if pytorch is used.
     */
    public boolean isUsingTensorflow() {
        return usingTensorflow;
    }

    /**
     * Sets the boolean value if tensorflow is used.
     * If set to false, true, pytorch is used.
     * @param usingTensorflow true to use tensorflow and false to use pytorch.
     */
    public void setUsingTensorflow(boolean usingTensorflow) {
        this.usingTensorflow = usingTensorflow;
    }

    /**
     * Returns a string which is set to the environment variable CUDA_VISIBLE_DEVICES to select on
     * which GPU the process should run. If null or empty, the default is used (all available GPUs).
     * @return the variable CUDA_VISIBLE_DEVICES
     */
    public String getCudaVisibleDevices() {
        return cudaVisibleDevices;
    }

    /**
     * Sets the environment variable CUDA_VISIBLE_DEVICES to select on
     * which GPUs the process should run. If null or the string is empty, the default is used (all available GPUs).
     * If multiple GPUs can be used, then the values should be comma separated.
     * Example: "0" to use only the first GPU. "1,3" to use the second and fourth GPU.
     * The use of {@link #setCudaVisibleDevices(int...) } is preffered because it is more type safe.
     * @param cudaVisibleDevices the string which is set to the environment variable CUDA_VISIBLE_DEVICES
     */
    public void setCudaVisibleDevices(String cudaVisibleDevices) {
        this.cudaVisibleDevices = cudaVisibleDevices.trim();
    }
    
    /**
     * Sets the environment variable CUDA_VISIBLE_DEVICES to select on
     * which GPUs the process should run. If no values are provided, then all available GPUs are used.
     * If multiple GPUs should be used, then provide the values one after the other.
     * All indices are zero based. So call {@code setCudaVisibleDevices(0,1)} to use the first two GPUs.
     * @param cudaVisibleDevices the integer numbers which refers to the GPUs which should be used.
     */
    public void setCudaVisibleDevices(int... cudaVisibleDevices) {
        this.cudaVisibleDevices = Arrays.stream(cudaVisibleDevices).mapToObj(String::valueOf).collect(Collectors.joining(","));
    }

    /**
     * Returns the cache folder where the pretrained transformers models are stored.
     * If set to null, the default locations is used (<a href="https://huggingface.co/transformers/installation.html#caching-models">
     * which is usually ~/.cache/huggingface/transformers/</a>).
     * @return the transformers cache folder.
     */
    public File getTransformersCache() {
        return transformersCache;
    }

    /**
     * Sets the cache folder where the pretrained transformers models are stored.
     * If set to null, the default locations is used (<a href="https://huggingface.co/transformers/installation.html#caching-models">
     * which is usually ~/.cache/huggingface/transformers/</a>).
     * This setter is useful, if the default location does not have enough space available.
     * Then just set it to a folder which have a lot of free space.
     * @param transformersCache The transformers cache folder.
     */
    public void setTransformersCache(File transformersCache) {
        if(transformersCache == null || transformersCache.isDirectory()) {
            this.transformersCache = transformersCache; //null sets the default value
        } else {
            throw new IllegalArgumentException("transformersCache is not a directory or does not exist.");
        }
    }

    /**
     * Returns the multiprocessing value of the transformer.
     * The transformers library may not free all memory from GPU.
     * Thus the prediction and training are wrapped in an external process.
     * This enum defines how the process is started and if multiprocessing should be used at all.
     * Default is to use the system dependent default.
     * @return the enum which represent the multi process starting method.
     */
    public TransformersMultiProcessing getMultiProcessing() {
        return multiProcessing;
    }
    
    /**
     * Sets the multiprocessing value of the transformer.
     * The transformers library may not free all memory from GPU.
     * Thus the prediction and training are wrapped in an external process.
     * This enum defines how the process is started and if multiprocessing should be used at all.
     * Default is to use the system dependent default.
     * @param multiProcessing the enum which represent the multi process starting method.
     */
    public void setMultiProcessing(TransformersMultiProcessing multiProcessing) {
        this.multiProcessing = multiProcessing;
    }
    
    /**
     * Enable or disable the mixed precision training.
     * This will optimize the runtime of training and 
     * @param mpt true to enable mixed precision training
     */
    public void setOptimizeForMixedPrecisionTraining(boolean mpt){
        this.trainingArguments.addParameter("fp16", mpt);
    }
    
    /**
     * Returns the value if mixed precision training is enabled or diabled.
     * @return true if mixed precision training is enabled.
     */
    public boolean isOptimizeForMixedPrecisionTraining(){
        Object o = this.trainingArguments.getParameterOrDefault("fp16", false);
        if(o instanceof Boolean){
            return (boolean) o;
        }else{
            LOGGER.warn("parameter fp16 is not a boolean value");
            return false;
        }
    }

    /**
     * Returns the value if all texts returned by the text extractor are used separately to generate the examples.
     * Otherwise it will concatenate all texts together to form one example(the default).
     * This should be only enabled when the extractor does not return many texts because otherwise a lot of examples are produced.
     * @return true, if generation of multiple examples is enabled
     */
    public boolean isMultipleTextsToMultipleExamples() {
        return multipleTextsToMultipleExamples;
    }

    /**
     * Is set to true, then all texts returned by the text extractor are used separately to generate the examples.
     * Otherwise it will concatenate all texts together to form one example(the default).
     * This should be only enabled when the extractor does not return many texts because otherwise a lot of examples are produced.
     * @param multipleTextsToMultipleExamples true, to enable the generation of multiple examples.
     */
    public void setMultipleTextsToMultipleExamples(boolean multipleTextsToMultipleExamples) {
        this.multipleTextsToMultipleExamples = multipleTextsToMultipleExamples;
    }
    
    /**
     * This function copies a part of a csv file to another file.This is used to find the best batch size.
     * @param source the source file
     * @param target the target file
     * @param numberOfCSVLines how many lines should be copied
     * @return true if enough lines are found in the input file.
     * @throws IOException in case of any io exception
     */
    protected boolean copyCSVLines(File source, File target, int numberOfCSVLines) throws IOException{
        int i = 1;
        try(CSVParser csvParser = CSVFormat.DEFAULT.parse(new InputStreamReader(new FileInputStream(source), StandardCharsets.UTF_8));
            CSVPrinter csvPrinter = CSVFormat.DEFAULT.print(target, StandardCharsets.UTF_8)){
            for (CSVRecord record : csvParser) {
                csvPrinter.printRecord(record);
                if(i >= numberOfCSVLines){
                    break;
                }
                i++;
            }
        }
        return i >= numberOfCSVLines;
    }
}
