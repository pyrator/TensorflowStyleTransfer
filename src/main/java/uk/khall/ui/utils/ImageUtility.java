package uk.khall.ui.utils;

import org.tensorflow.ndarray.ByteNdArray;
import org.tensorflow.ndarray.NdArrays;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.ndarray.buffer.ByteDataBuffer;
import org.tensorflow.ndarray.buffer.DataBuffers;
import org.tensorflow.types.TUint8;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;

public class ImageUtility {

    /**
     * resize the buffered image to fit the frame
     * @param bi
     * @return
     */
    public static BufferedImage resizeImage(BufferedImage bi, int width, int height, Component comp){
        Image scaledImage;
        if ((bi.getWidth() <= bi.getHeight())){
            scaledImage=bi.getScaledInstance(-1, height, BufferedImage.SCALE_SMOOTH);
            //scaledHeight =jpegFileImagePanel.getHeight();
            //scaledWidth = (int)(scaledHeight/bi.getHeight()) * bi.getWidth();
        } else {
            scaledImage=bi.getScaledInstance(width, -1, BufferedImage.SCALE_SMOOTH);
            //scaledWidth =jpegFileImagePanel.getWidth();
            //scaledHeight = (int)(scaledWidth/bi.getWidth()) * bi.getHeight();
        }
        return createBufferedImage(scaledImage, BufferedImage.TYPE_INT_RGB, comp);
    }
    /**
     * resize the buffered image to fit the frame
     * @param bi
     * @return
     */
    public static BufferedImage resizeImageToWandH(BufferedImage bi, int width, int height, Component comp){
        Image scaledImage;
            scaledImage=bi.getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH);
            //scaledHeight =jpegFileImagePanel.getHeight();
            //scaledWidth = (int)(scaledHeight/bi.getHeight()) * bi.getWidth();

        return createBufferedImage(scaledImage, BufferedImage.TYPE_INT_RGB, comp);
    }
    static public BufferedImage createBufferedImage(Image imageIn, int imageType, Component comp) {
        MediaTracker mt = new MediaTracker(comp);
        mt.addImage(imageIn, 0);
        try {
            mt.waitForID(0);
        } catch (InterruptedException ie) {
        }
        int w = imageIn.getWidth(null);
        int h = imageIn.getHeight(null);
        if(w < 1){
            w = 1;
        }
        if(h < 1){
            h = 1;
        }
        BufferedImage bufferedImageOut = new BufferedImage(w, h, imageType);
        Graphics g = bufferedImageOut.getGraphics();
        g.drawImage(imageIn, 0, 0, null);

        return bufferedImageOut;
    }

    static public BufferedImage setHSV(BufferedImage bufferedImage, int h, int s, int v){
        int WIDTH = bufferedImage.getWidth();
        int HEIGHT = bufferedImage.getHeight();
        BufferedImage processedImage = new BufferedImage(WIDTH,HEIGHT,bufferedImage.getType());
        if (h > 360 || s > 360 || v > 300) {
            System.out.println("Values too high");
            return bufferedImage;
        }
        if (h < 0 || s < 0 || v < 0) {
            System.out.println("Values too low");
            return bufferedImage;
        }
        float hue = h/360.0f;
        float sat = s/360.0f;
        float val = v/360.0f;
        for(int Y=0; Y<HEIGHT;Y++) {
            for(int X=0;X<WIDTH;X++) {
                int RGB = bufferedImage.getRGB(X,Y);
                int R = (RGB >> 16) & 0xff;
                int G = (RGB >> 8) & 0xff;
                int B = (RGB) & 0xff;
                float[] HSV = new float[3];
                Color.RGBtoHSB(R,G,B,HSV);
                processedImage.setRGB(X,Y,Color.getHSBColor(hue,sat,val).getRGB());
            }
        }

        return processedImage;
    }

    public static BufferedImage bufferedImageFromTensor(TUint8 imageTensor, Shape imageShape){
        ByteNdArray byteNdArray_3 = NdArrays.ofBytes(imageTensor.shape());
        ByteNdArray newbyteNdArray_3 = imageTensor.copyTo(byteNdArray_3);
        byte[] outpixels_3 = new byte[(int) (imageShape.asArray()[0] * (int) (imageShape.asArray()[1]) * 3)];
        ByteDataBuffer newbyteDataBuffer_3 = DataBuffers.of(outpixels_3, false, false);
        newbyteNdArray_3.read(newbyteDataBuffer_3);
        newbyteDataBuffer_3.read(outpixels_3);
        BufferedImage bufferedImage = new BufferedImage((int) (imageShape.asArray()[1]), (int) (imageShape.asArray()[0]), BufferedImage.TYPE_3BYTE_BGR);
        bufferedImage.setData(Raster.createRaster(bufferedImage.getSampleModel(), new DataBufferByte(outpixels_3, outpixels_3.length), new Point()));
        return bufferedImage;
    }

}
