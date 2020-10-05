# Quickstart

----

*** Note ***

This samples intent is to explore the data engineering needed to work with data generated from a streaming Beam 
pipeline and delivered to an auto encoder - decoder.

It is not intended to demonstrate cutting edge ML model techniques. 

In these samples we are using FIRST and LAST for ETH in the learning, and the outlier test.

----

## Generate TF.Example's with Band Protocol data

In order to build the model you will need to first run the generator job 
[SimpleDataBootstrapGenerator.java](../timeseries-java-applications/BandExamples/src/main/java/io/blockchainetl/band/examples/simpledata/transforms/BandDataBootstrapGenerator.java).

```
git clone https://github.com/blockchain-etl/band-dataflow-sample-applications.git
cd dataflow-sample-applications/timeseries-streaming/timeseries-java-applications
```

Run the Dataflow job:

```
TIMESTAMP_THRESHOLD="2020-10-03T00:00:00.0Z"
BUCKET=band-etl-dataflow-temp0
./gradlew generate_band_bootstrap_data --args="\
    --interchangeLocation=gs://$BUCKET/band_bootstrap_tfexamples/run0 \
    --timestampThreshold="$TIMESTAMP_THRESHOLD" \
    --runner=DataflowRunner \
    --tempLocation=gs://$BUCKET/temp \
    --maxNumWorkers=1 \
    --region=us-central1 \
"
```

Once the job is done, download the generated files from `gs://$BUCKET/band_bootstrap_tfexamples/run0`, 
change the information in the [config.py](MLPipelineExamples/test_pipelines/config.py) to match your local env.

## Train the model

Setup virtual environment:

```
virtualenv -p python3.7 streaming-tf-consumer
source streaming-tf-consumer/bin/activate
```

Install the dependencies:

```
git clone https://github.com/blockchain-etl/band-dataflow-sample-applications.git
cd dataflow-sample-applications/timeseries-streaming/timeseries-python-applications
cd MLPipeline
pip install -e .
cd ..
ls
```

Run the command with the virtual-env activated:

```
python MLPipelineExamples/test_pipelines/timeseries_local_simple_data.py
``` 

You should see the model building as below.

```
....
Epoch 26/30
280/280 [==============================] - 6s 21ms/step - loss: 119.8072 - mean_absolute_error: 6.8178 - val_loss: 684588.3750 - val_mean_absolute_error: 670.2068
Epoch 27/30
280/280 [==============================] - 7s 23ms/step - loss: 119.6002 - mean_absolute_error: 6.8087 - val_loss: 203.5257 - val_mean_absolute_error: 8.6160
Epoch 28/30
280/280 [==============================] - 6s 20ms/step - loss: 119.5842 - mean_absolute_error: 6.8084 - val_loss: 41512.6406 - val_mean_absolute_error: 160.1564
Epoch 29/30
280/280 [==============================] - 6s 20ms/step - loss: 119.5832 - mean_absolute_error: 6.8084 - val_loss: 5213.2568 - val_mean_absolute_error: 58.4286
Epoch 30/30
280/280 [==============================] - 5s 19ms/step - loss: 119.5791 - mean_absolute_error: 6.8083 - val_loss: 24351.4551 - val_mean_absolute_error: 151.2784
```

This will output a serving_model_dir under the location you specified for ```PIPELINE_ROOT``` in the config.py file. 
With this you can now follow the rest of the steps outlines in Option 1 but using your own model.

For example:
```
python MLPipelineExamples/test_pipelines/batch_inference.py --saved_model_location=<PIPELINE_ROOT>/serving_model_dir --tfrecord_folder=/<your-directory>/simple-data/data/*
```

## Batch inference

[batch_inference](MLPipelineExamples/test_pipelines/batch_inference.py) reads given TF.Example's and the saved model,
runs inference to get a predicted value, compares this value with the actual value and if the difference is greater 
than the threshold reports the value as anomalous.

Run the command with the virtual-env activated, providing values for the location of 
the ```--saved_model_location``` using the model built on the previous step, 
and the location of the generated data you downloaded from GCS bucket with ```--tfrecord_folder```.

```
python MLPipelineExamples/test_pipelines/batch_inference.py \
--saved_model_location=./build/Trainer/model/5/serving_model_dir \
--tfrecord_folder=/<your-directory>/data/*
```

You will see messages of detected outliers:

```
...
Outlier detected for ETH-value-LAST at 2020-08-23 21:20:00 - 2020-08-23 22:20:00 Difference was 279.74764251708984 for value input 388.8900146484375 output 109.14237213134766 with raw data [388.8900146484375]
Outlier detected for ETH-value-FIRST at 2020-08-22 14:20:00 - 2020-08-22 15:20:00 Difference was 310.73509216308594 for value input 392.1044921875 output 81.36940002441406 with raw data [392.1044921875]
Outlier detected for ETH-value-LAST at 2020-08-22 14:20:00 - 2020-08-22 15:20:00 Difference was 308.1451416015625 for value input 392.1044921875 output 83.9593505859375 with raw data [392.1044921875]
```

For more example pipelines refer to this repository 
https://github.com/GoogleCloudPlatform/dataflow-sample-applications/tree/master/timeseries-streaming/timeseries-python-applications.
