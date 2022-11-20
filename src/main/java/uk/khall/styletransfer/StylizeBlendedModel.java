package uk.khall.styletransfer;


import org.tensorflow.Graph;
import org.tensorflow.Operand;
import org.tensorflow.Result;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFunction;

import org.tensorflow.op.Ops;
import org.tensorflow.op.core.Constant;
import org.tensorflow.op.core.ExpandDims;
import org.tensorflow.op.core.Reshape;
import org.tensorflow.op.core.Reverse;
import org.tensorflow.op.image.DecodeJpeg;
import org.tensorflow.op.image.ResizeBilinear;
import org.tensorflow.op.io.ReadFile;
import org.tensorflow.op.math.Add;
import org.tensorflow.op.math.Div;
import org.tensorflow.proto.framework.ConfigProto;
import org.tensorflow.proto.framework.GPUOptions;
import org.tensorflow.types.TFloat32;
import org.tensorflow.types.TInt32;
import org.tensorflow.types.TString;
import org.tensorflow.types.TUint8;

import uk.khall.ui.utils.ImageUtility;
import uk.khall.utils.ModelHubUtils;


import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class StylizeBlendedModel {
    //serving_default -> {ConcreteFunction@2270} "Signature for "serving_default":
    //	Method: "tensorflow/serving/predict"
    //	Inputs:
    //		"placeholder": dtype=DT_FLOAT, shape=(-1, -1, -1, 3)
    //		"placeholder_1": dtype=DT_FLOAT, shape=(-1, -1, -1, 3)
    //	Outputs:
    //		"output_0": dtype=DT_FLOAT, shape=(-1, -1, -1, 3)
    //
    private static String sourceImageDir = null;

    /**
     * Stylize using magenta arbitrary-image-stylization model
     *
     * @param imagePath      path to test image
     * @param styleImagePath path to style image
     * @param imgSize
     * @param slideValue
     * @return
     */
    public static BufferedImage stylize(String imagePath, String styleImagePath, Integer imgSize, Integer slideValue) {

        BufferedImage bufferedImage = null;

        float blendVal = slideValue.floatValue() / 100;

        // get path to model folder
        String modelFolder = "models";
        String modelName = "arbitrary-image-stylization-v1-256";
        String version = "2";
        String urlStart = "https://tfhub.dev/google/magenta";

        String modelPath = modelFolder + "/" + modelName;
        try {
            if (!new File(modelPath).exists()) {
                ModelHubUtils.extractTarGZ(urlStart, modelName, modelFolder, version);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        GPUOptions gpu = ConfigProto.getDefaultInstance().getGpuOptions().toBuilder() //
                .setPerProcessGpuMemoryFraction(0.9) //
                .setAllowGrowth(true) //
                .build(); //

        ConfigProto configProto = ConfigProto.newBuilder(ConfigProto.getDefaultInstance()) //
                .setLogDevicePlacement(true) //
                .mergeGpuOptions(gpu) //
                .build(); //
        try (Graph graph = new Graph(); Session s = new Session(graph)) {
            Ops tf = Ops.create(graph);
            Constant<TString> fileName = tf.constant(imagePath);
            ReadFile readFile = tf.io.readFile(fileName);
            DecodeJpeg.Options options = DecodeJpeg.channels(3L);
            DecodeJpeg decodeImage = tf.image.decodeJpeg(readFile.contents(), options);
            //fetch image from file
            try (TUint8 inputImage = (TUint8) s.runner().fetch(decodeImage).run().get(0)) {
                Div<TFloat32> normalizedImage = tf.math.div(tf.dtypes.cast(decodeImage, TFloat32.class), tf.constant(255.0f));
                ExpandDims<TFloat32> reshapedImage = tf.expandDims(normalizedImage, tf.constant(0));
                try (TFloat32 resizeInputTensor = (TFloat32) s.runner().fetch(reshapedImage).run().get(0)) {
                    Constant<TString> styleFileName = tf.constant(styleImagePath);
                    readFile = tf.io.readFile(styleFileName);
                    options = DecodeJpeg.channels(3L);
                    decodeImage = tf.image.decodeJpeg(readFile.contents(), options);
                    //fetch image from file
                    try (TUint8 styleImage = (TUint8) s.runner().fetch(decodeImage).run().get(0)) {
                        //create a 4D tensor of shape `[num_boxes, crop_height, crop_width, depth]`
                        ExpandDims<TUint8> reshapeStyle = tf.expandDims(tf.constant(styleImage), tf.constant(0));
                        try (TUint8 reshapedStyle = (TUint8) s.runner().fetch(reshapeStyle).run().get(0)) {
                            //resize image
                            Operand<TUint8> reshapedStyleOP = tf.constant(reshapedStyle);
                            ResizeBilinear.Options halfPixelCenters = ResizeBilinear.halfPixelCenters(true);
                            Operand<TInt32> reSize = tf.constant(new int[]{imgSize, imgSize});
                            ResizeBilinear resizeBilinear = tf.image.resizeBilinear(reshapedStyleOP, reSize, halfPixelCenters);
                            try (TFloat32 croppedStyleImage = (TFloat32) s.runner().fetch(resizeBilinear).run().get(0)) {
                                Div<TFloat32> div = tf.math.div(
                                        tf.constant(croppedStyleImage),
                                        tf.constant(255.0f)
                                );
                                try (TFloat32 styleResizeTensor = (TFloat32) s.runner().fetch(div).run().get(0)) {

                                    ResizeBilinear resizeBilinearInputImage = tf.image.resizeBilinear(reshapedStyleOP, reSize, halfPixelCenters);
                                    try (TFloat32 croppedInputImage = (TFloat32) s.runner().fetch(resizeBilinearInputImage).run().get(0)) {
                                        Add<TFloat32> divInput =
                                                tf.math.add(
                                                        tf.math.mul(
                                                                tf.math.div(tf.constant(croppedInputImage), tf.constant(255.0f)),
                                                                tf.constant(blendVal)
                                                        ),
                                                        tf.math.mul(tf.constant(styleResizeTensor), tf.constant(1 - blendVal))
                                                );

                                        try (TFloat32 styleBlendResizeTensor = (TFloat32) s.runner().fetch(divInput).run().get(0)) {
                                            //The given SavedModel MetaGraphDef key
                                            SavedModelBundle model = SavedModelBundle.loader(modelPath).withTags("serve").withConfigProto(configProto).load();
                                            Map<String, Tensor> feedDict = new HashMap<>();
                                            //The given SavedModel SignatureDef input
                                            feedDict.put("placeholder", resizeInputTensor);
                                            feedDict.put("placeholder_1", styleBlendResizeTensor);
                                            TensorFunction serving = model.function("serving_default");
                                            //System.out.println(serving.signature());

                                            try (Result result = serving.call(feedDict);
                                                 TFloat32 outputTensor = (TFloat32) result.get("output_0").orElseThrow(Exception::new)) {

                                                Reverse<TUint8> reverse = tf.reverse(tf.reshape(tf.dtypes.cast(tf.math.mul(
                                                                        tf.constant(outputTensor),
                                                                        tf.constant(255.0f)
                                                                ), TUint8.class),
                                                                tf.array(
                                                                        outputTensor.shape().asArray()[1],
                                                                        outputTensor.shape().asArray()[2],
                                                                        outputTensor.shape().asArray()[3]
                                                                )
                                                        ), tf.constant(new long[]{2L})
                                                );
                                                try (TUint8 outputImage = (TUint8) s.runner().fetch(reverse).run().get(0)) {
                                                    bufferedImage = ImageUtility.bufferedImageFromTensor(outputImage, outputImage.shape());
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
        return bufferedImage;
    }

    /**
     * Stylize using magenta arbitrary-image-stylization model
     *
     * @param imagePath      path to test image
     * @param styleImagePath path to style image
     * @param imgSize
     * @param slideValue     ratio of mix between original and style
     * @param imageRatio     resize
     * @return
     */
    public static BufferedImage stylize(String imagePath, String styleImagePath, Integer imgSize, Integer slideValue, Float imageRatio) {
        BufferedImage bufferedImage = null;
        float blendVal = slideValue.floatValue() / 100;

        // get path to model folder
        String modelFolder = "models";
        String modelName = "arbitrary-image-stylization-v1-256";
        String version = "2";
        String urlStart = "https://tfhub.dev/google/magenta";

        String modelPath = modelFolder + "/" + modelName;
        try {
            if (!new File(modelPath).exists()) {
                ModelHubUtils.extractTarGZ(urlStart, modelName, modelFolder, version);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (Graph graph = new Graph(); Session s = new Session(graph)) {
            Ops tf = Ops.create(graph);
            Constant<TString> fileName = tf.constant(imagePath);
            ReadFile readFile = tf.io.readFile(fileName);

            DecodeJpeg.Options options = DecodeJpeg.channels(3L);
            DecodeJpeg decodeImage = tf.image.decodeJpeg(readFile.contents(), options);
            //fetch image from file
            try (TUint8 inputImage = (TUint8) s.runner().fetch(decodeImage).run().get(0)) {

                ExpandDims<TUint8> reshapeInput = tf.expandDims(tf.constant(inputImage), tf.constant(0));
                try (TUint8 reshapedInput = (TUint8) s.runner().fetch(reshapeInput).run().get(0)) {
                    //resize input image to new ratio
                    Operand<TUint8> reshapedInputOP = tf.constant(reshapedInput);
                    ResizeBilinear.Options halfPixelCenters = ResizeBilinear.halfPixelCenters(true);
                    Operand<TInt32> reSize = tf.constant(new int[]{(int) (imageRatio * inputImage.shape().asArray()[0]),
                            (int) (imageRatio * inputImage.shape().asArray()[1])});
                    ResizeBilinear resizeBilinear = tf.image.resizeBilinear(reshapedInputOP, reSize, halfPixelCenters);
                    try (TFloat32 croppedInputTensor = (TFloat32) s.runner().fetch(resizeBilinear).run().get(0)) {
                        //divide cropped input by 255
                        Div<TFloat32> divCropInput = tf.math.div(
                                tf.constant(croppedInputTensor), tf.constant(255.0f)
                        );
                        try (TFloat32 divCropInputTensor = (TFloat32) s.runner().fetch(divCropInput).run().get(0)) {
                            //original sized input image
                            Reshape<TFloat32> reshapeDivInput = tf.reshape(tf.math.div(
                                            tf.dtypes.cast(tf.constant(inputImage), TFloat32.class),
                                            tf.constant(255.0f)
                                    ),
                                    tf.array(1,
                                            inputImage.shape().asArray()[0],
                                            inputImage.shape().asArray()[1],
                                            inputImage.shape().asArray()[2]
                                    )
                            );
                            //original sized input image divided by 255
                            try (TFloat32 resizeInputTensor = (TFloat32) s.runner().fetch(reshapeDivInput).run().get(0)) {

                                Constant<TString> styleFileName = tf.constant(styleImagePath);
                                readFile = tf.io.readFile(styleFileName);
                                options = DecodeJpeg.channels(3L);
                                decodeImage = tf.image.decodeJpeg(readFile.contents(), options);
                                //fetch style image from file
                                try (TUint8 styleImage = (TUint8) s.runner().fetch(decodeImage).run().get(0)) {
                                    //create a 4D tensor of shape `[num_boxes, crop_height, crop_width, depth]`
                                    Reshape<TUint8> reshapeStyle = tf.reshape(tf.constant(styleImage),
                                            tf.array(1,
                                                    styleImage.shape().asArray()[0],
                                                    styleImage.shape().asArray()[1],
                                                    styleImage.shape().asArray()[2]
                                            )
                                    );
                                    try (TUint8 reshapedStyle = (TUint8) s.runner().fetch(reshapeStyle).run().get(0)) {
                                        //resize image
                                        reSize = tf.constant(new int[]{imgSize, imgSize});
                                        ResizeBilinear resizeBilinearStyle = tf.image.resizeBilinear(reshapedInputOP, reSize, halfPixelCenters);
                                        try (TFloat32 croppedStyleImage = (TFloat32) s.runner().fetch(resizeBilinearStyle).run().get(0)) {
                                            Div<TFloat32> div = tf.math.div(
                                                    tf.constant(croppedStyleImage),
                                                    tf.constant(255.0f)
                                            );
                                            try (TFloat32 styleResizeTensor = (TFloat32) s.runner().fetch(div).run().get(0)) {

                                                ResizeBilinear resizeBilinearInput = tf.image.resizeBilinear(reshapedInputOP, reSize, halfPixelCenters);
                                                try (TFloat32 croppedInputImage = (TFloat32) s.runner().fetch(resizeBilinearInput).run().get(0)) {
                                                    Add<TFloat32> divInput =
                                                            tf.math.add(
                                                                    tf.math.mul(
                                                                            tf.math.div(tf.constant(croppedInputImage), tf.constant(255.0f)),
                                                                            tf.constant(blendVal)
                                                                    ),
                                                                    tf.math.mul(tf.constant(styleResizeTensor), tf.constant(1 - blendVal))
                                                            );

                                                    try (TFloat32 styleBlendResizeTensor = (TFloat32) s.runner().fetch(divInput).run().get(0)) {
                                                        //The given SavedModel MetaGraphDef key
                                                        SavedModelBundle model = SavedModelBundle.load(modelPath, "serve");
                                                        Map<String, Tensor> feedDict = new HashMap<>();
                                                        //The given SavedModel SignatureDef input
                                                        feedDict.put("placeholder", resizeInputTensor);
                                                        feedDict.put("placeholder_1", styleBlendResizeTensor);
                                                        Result result = model.function("serving_default").call(feedDict);
                                                        Map<String, Tensor> outputTensorMap = new HashMap<String, Tensor>();
                                                        for (Map.Entry<String, Tensor> e : result) {
                                                            outputTensorMap.put(e.getKey(), e.getValue());
                                                        }
                                                        try (TFloat32 outputTensor = (TFloat32) outputTensorMap.get("output_0")) {
                                                            Reshape<TUint8> reshapeImage = tf.reshape(tf.dtypes.cast(tf.math.mul(
                                                                            tf.constant(outputTensor),
                                                                            tf.constant(255.0f)
                                                                    ), TUint8.class),
                                                                    tf.array(
                                                                            outputTensor.shape().asArray()[1],
                                                                            outputTensor.shape().asArray()[2],
                                                                            outputTensor.shape().asArray()[3]
                                                                    )
                                                            );

                                                            try (TUint8 outputImage = (TUint8) s.runner().fetch(reshapeImage).run().get(0)) {
                                                                Reverse<TUint8> reverse = tf.reverse(tf.constant(outputImage), tf.constant(new long[]{2L}));
                                                                try (TUint8 reversedImage = (TUint8) s.runner().fetch(reverse).run().get(0)) {
                                                                    bufferedImage = ImageUtility.bufferedImageFromTensor(reversedImage, outputImage.shape());
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return bufferedImage;
    }
}

