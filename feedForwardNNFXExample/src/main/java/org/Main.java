/* using feedforward neural network for regression of GBPUSD spot rate on (SONIA-SOFR rate)
 differences.
 If a country's risk-free interest rate falls relative to other countries, its exchange
 rate will typically depreciate. In terms of data, if (SONIA - SOFR) falls, this means
 SONIA fell relative to SOFR therefore GBPUSD spot expected to fall
 However, if the country whose interest rate falls relatively has stronger economic
 performance, more favourable balance of trade, lower relative inflation, or speculation,
 the spot rate can appreciate instead.
 Reference for SOFR data: https://fred.stlouisfed.org/
 Reference for SONIA data: https://www.bankofengland.co.uk/
 GBPUSD End of Day Spot Data Ref: Google Finance
 x-axis data: SONIA-SOFR daily rate difference, y-axis: GBPUSD End of Day spot rate
 Daily data in csv across business days excluding 13 Oct 2025 from 17 Sep 2025 to 5 Nov 2025
*/

package org;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Main {

    private static DataSetIterator readCSVDataset(String filename) throws IOException, InterruptedException{
        int batchSize = 100;
        RecordReader rr = new CSVRecordReader();
        rr.initialize(new FileSplit(new File(filename)));

        DataSetIterator iter = new RecordReaderDataSetIterator(rr, batchSize, 1, 1, true);
        return iter;
    }

    private static MultiLayerNetwork fitSpotIntRateDiffData(DataSetIterator ds, int seed, int noEps, double learnRate,
                                                            int noInputs, int noOutputs){

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .weightInit(WeightInit.XAVIER)
                .updater(new Nesterovs(learnRate, 0.9))
                .list() // start adding layers sequentially
                .layer(0, new DenseLayer.Builder().nIn(noInputs).nOut(noOutputs)
                        .activation(Activation.IDENTITY)
                        .build())
                .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .nIn(noInputs).nOut(noOutputs).build())
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        net.setListeners(new ScoreIterationListener(1));

        for (int i = 0; i < noEps; i++){
            net.fit(ds);
        }
        return net;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String fileName = new File("intRateDiffGBPUSDSpot.csv").getAbsolutePath();
        DataSetIterator ds = readCSVDataset(fileName);
        // Set up params for our feedforward neural network
        int seed = 34567;
        int noEps = 100;
        double learnRate = 0.00001;
        int noInputs = 1;
        int noOutputs = 1;
        MultiLayerNetwork netFitted = fitSpotIntRateDiffData(ds, seed, noEps, learnRate, noInputs, noOutputs);
        NormalizerMinMaxScaler preProcessor = new NormalizerMinMaxScaler();
        preProcessor.fit(ds);
        int nSamples = 50;
        INDArray x = Nd4j.linspace(preProcessor.getMin().getInt(0), preProcessor.getMax().getInt(0), nSamples).reshape(nSamples, 1);
        INDArray y = netFitted.output(x);
        DataSet modelOutput = new DataSet(x, y);
        System.out.println("The slope and intercept params of the first dense layer respectively are: "
                + Arrays.stream(netFitted.getLayers()).findFirst().get().params());
        System.out.println("The slope and intercept params of the output layer respectively are: "
                + Arrays.stream(netFitted.getLayers()).toList().get(1).params());
    }
}