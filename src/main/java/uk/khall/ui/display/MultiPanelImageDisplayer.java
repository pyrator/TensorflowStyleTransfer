package uk.khall.ui.display;

import uk.khall.styletransfer.StylizeBlendedModel;
import uk.khall.ui.utils.FileChooser;
import uk.khall.ui.utils.ImageUtility;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;


public class MultiPanelImageDisplayer extends JFrame {
    GraphicsConfiguration config;
    private BufferedImage sourceImage = null;
    private BufferedImage outputImage = null;
    private BufferedImage previewImage = null;
    private BufferedImage previewStyleImage = null;
    private BufferedImage previewBlendedImage = null;
    private JScrollPane jScrollPane;
    private JScrollPane jSmallScrollPane;
    private JScrollPane jSmallBlendedScrollPane;
    private ImageCanvas canvasPanel = null;
    private ImageCanvas smallCanvasPanel = null;
    private ImageCanvas smallBlendedCanvasPanel = null;
    private int canvasWidth = 0;
    private int canvasHeight = 0;
    private JButton loadImage;
    private JButton prevStyle;
    private JButton nextStyle;
    private JComboBox styleSizeList;
    private JSlider percentStyleSlider;
    private JButton runStyle;
    private JButton saveImage;
    private JTextField messageText;
    private final String styleFolder = "styleimages";
    File[] styleImages;
    private String mainImagePath;
    private String styleImagePath;
    private int styleNumber;
    private Integer percentStyle =50;
    private Integer styleSize = 256;
    Integer[] scaleValues = {128, 256, 512, 1024};
    static final int scaleMIN = 0;
    static final int scaleMAX = 100;
    static final int scaleINIT = 50;
    private Integer previewSize = 256;
    private JPanel buttonPanel = null;
    public MultiPanelImageDisplayer(int canvasWidth, int canvasHeight) {
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.styleNumber = 0;
        GridBagConstraints gridBagConstraints;
        jScrollPane = new JScrollPane();
        canvasPanel = new ImageCanvas();
        smallCanvasPanel = new ImageCanvas();
        smallBlendedCanvasPanel = new ImageCanvas();
        File dir = new File(styleFolder);
        styleImages = dir.listFiles();
        int n = 0;
        boolean found = false;
        do{
            System.out.println("files =" + styleImages[n]);
            if (styleImages[n].getName().toLowerCase().endsWith(".jpg") || styleImages[n].getName().toLowerCase().endsWith(".jpeg")) {
                try {
                    BufferedImage bi = ImageIO.read(styleImages[n]);
                    previewStyleImage = ImageUtility.resizeImageToWandH(bi, previewSize, previewSize, this);
                    smallCanvasPanel.setImage(previewStyleImage);
                    smallBlendedCanvasPanel.setImage(previewStyleImage);
                    found = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            n++;
        }while (!found);
        if(!found){
            previewStyleImage = new BufferedImage(previewSize,previewSize,BufferedImage.TYPE_INT_RGB);
            smallCanvasPanel.setImage(previewStyleImage);
            smallBlendedCanvasPanel.setImage(previewStyleImage);
        }
        styleNumber = n;
        setStyleImagePath(styleImages[styleNumber].getPath());
        getContentPane().setLayout(new GridBagLayout());
        setTitle("Style Transfer");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });

        jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane.setViewportBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.RAISED));
        jScrollPane.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(MouseEvent evt) {
                jScrollPaneMouseReleased(evt);
            }
        });

        canvasPanel.setFont(new Font("Dialog", 0, 11));
        canvasPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                canvasPanelMouseClicked(evt);
            }

            public void mouseReleased(MouseEvent evt) {
                canvasPanelMouseReleased(evt);
            }

            public void mousePressed(MouseEvent evt) {
                canvasPanelMousePressed(evt);
            }
        });
        canvasPanel.addMouseMotionListener(new java.awt.event.MouseAdapter() {
            public void mouseMoved(MouseEvent evt) {
                canvasPanelMouseMoved(evt);
            }

        });

        jScrollPane.setViewportView(canvasPanel);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 9;
        gridBagConstraints.gridheight = 12;
        gridBagConstraints.weightx = 1.5;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

        getContentPane().add(jScrollPane, gridBagConstraints);

        buttonPanel = new JPanel(new GridBagLayout());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 9;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        getContentPane().add(buttonPanel, gridBagConstraints);


        loadImage = new JButton();
        loadImage.setText("Load Image");
        loadImage.setName("loadImage");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        //gridBagConstraints.weightx = 0.1;
        //gridBagConstraints.weighty = 0.05;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        //getContentPane().add(loadImage, gridBagConstraints);

        loadImage.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                mouseButtonClicked(evt);
            }
        });
        buttonPanel.add(loadImage,gridBagConstraints);

        prevStyle = new JButton();
        prevStyle.setText("Prev Style");
        prevStyle.setName("prevStyle");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        //gridBagConstraints.weightx = 0.1;
        //gridBagConstraints.weighty = 0.05;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        //getContentPane().add(prevStyle, gridBagConstraints);
        prevStyle.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                mouseButtonClicked(evt);
            }
        });
        buttonPanel.add(prevStyle,gridBagConstraints);


        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 2;
        //gridBagConstraints.weightx = 0.1;
        //gridBagConstraints.weighty = 0.2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        jSmallScrollPane =  new JScrollPane();
        jSmallScrollPane.setViewportView(smallCanvasPanel);
        //getContentPane().add(jSmallScrollPane, gridBagConstraints);
        buttonPanel.add(jSmallScrollPane,gridBagConstraints);

        nextStyle = new JButton();
        nextStyle.setText("Next Style");
        nextStyle.setName("nextStyle");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        //gridBagConstraints.weightx = 0.1;
        //gridBagConstraints.weighty = 0.05;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        //getContentPane().add(nextStyle, gridBagConstraints);
        nextStyle.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                mouseButtonClicked(evt);
            }
        });
        buttonPanel.add(nextStyle,gridBagConstraints);


        styleSizeList = new JComboBox(scaleValues);
        styleSizeList.setSelectedIndex(1);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        //gridBagConstraints.weightx = 0.1;
        //gridBagConstraints.weighty = 0.05;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        //getContentPane().add(styleSizeList, gridBagConstraints);
        buttonPanel.add(styleSizeList,gridBagConstraints);


        gridBagConstraints = new GridBagConstraints();
        percentStyleSlider = new JSlider();
        percentStyleSlider = new JSlider(JSlider.HORIZONTAL,
                scaleMIN, scaleMAX, scaleINIT);
        percentStyleSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(MouseEvent evt) {
                scaleValReleased(evt);
            }
        });
        percentStyleSlider.setOpaque(true); //content panes must be opaque

        //Turn on labels at major tick marks.

        percentStyleSlider.setMajorTickSpacing(10);
        percentStyleSlider.setMinorTickSpacing(1);
        percentStyleSlider.setPaintTicks(true);
        percentStyleSlider.setPaintLabels(true);


        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        //gridBagConstraints.weightx = 0.1;
        //gridBagConstraints.weighty = 0.05;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        //getContentPane().add(percentStyleSlider, gridBagConstraints);
        buttonPanel.add(percentStyleSlider,gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridheight = 2;
        //gridBagConstraints.weightx = 0.1;
        //gridBagConstraints.weighty = 0.2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        jSmallBlendedScrollPane =  new JScrollPane();
        jSmallBlendedScrollPane.setViewportView(smallBlendedCanvasPanel);
        //getContentPane().add(jSmallBlendedScrollPane, gridBagConstraints);
        buttonPanel.add(jSmallBlendedScrollPane,gridBagConstraints);

        runStyle = new JButton();
        runStyle.setText("Run Style");
        runStyle.setName("runStyle");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        //gridBagConstraints.weightx = 0.1;
        //gridBagConstraints.weighty = 0.05;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        //getContentPane().add(runStyle, gridBagConstraints);
        runStyle.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                mouseButtonClicked(evt);
            }
        });
        buttonPanel.add(runStyle,gridBagConstraints);

        saveImage = new JButton();
        saveImage.setText("Save Image");
        saveImage.setName("saveImage");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        //gridBagConstraints.weightx = 0.1;
        //gridBagConstraints.weighty = 0.05;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        getContentPane().add(saveImage, gridBagConstraints);
        saveImage.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                mouseButtonClicked(evt);
            }
        });
        buttonPanel.add(saveImage,gridBagConstraints);

        messageText = new JTextField();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        //gridBagConstraints.weightx = 0.1;
        //gridBagConstraints.weighty = 0.05;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        getContentPane().add(messageText, gridBagConstraints);
        buttonPanel.add(messageText,gridBagConstraints);

        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (getCanvasWidth() == 0 || getCanvasHeight() == 0) {
            setSize(new Dimension(900, 1000));
        } else {
            setSize(new Dimension(getCanvasWidth(), getCanvasHeight()));
        }
        setLocation(1,1);
        //setLocation((screenSize.width - 500) / 2, (screenSize.height - 600) / 2);
    }

    private void jScrollPaneMouseReleased(MouseEvent evt) {//GEN-FIRST:event_jScrollPaneMouseReleased
        // repaint the image Panel:
        canvasPanel.repaint();
    }//GEN-LAST:event_jScrollPaneMouseReleased

    private void canvasPanelMouseClicked(MouseEvent evt) {//GEN-FIRST:event_demImagePanelMouseClicked
        // find the x and y co-ordinate of image panel:
        int clickX = evt.getX();
        int clickY = evt.getY();
        Color col = getColorPixel(clickX, clickY);

    }//GEN-LAST:event_canvasPanelMouseClicked

    private void canvasPanelMouseReleased(MouseEvent evt) {//GEN-FIRST:event_jScrollPaneMouseReleased
        // repaint the image Panel:

    }//GEN-LAST:event_jScrollPaneMouseReleased

    private void canvasPanelMouseMoved(MouseEvent evt) {//GEN-FIRST:event_jScrollPaneMouseReleased
        // repaint the image Panel:

    }//GEN-LAST:event_jScrollPaneMouseReleased

    private void canvasPanelMousePressed(MouseEvent evt) {//GEN-FIRST:event_jScrollPaneMouseReleased
        // repaint the image Panel:

    }//GEN-LAST:event_jScrollPaneMouseReleased
    /**
     * select click event to process
     *
     * @param evt
     */
    private void mouseButtonClicked(MouseEvent evt) {
        String evtName = evt.getComponent().getName();
        //System.out.println("evtName = " + evtName);
        if (evt.getButton() == MouseEvent.BUTTON1) {
            if (evtName.equals("loadImage")) {
                sourceImage = loadImage();
                previewImage = ImageUtility.resizeImageToWandH(sourceImage,previewSize,previewSize, this);
                previewBlendedImage = blendImage(previewImage,previewStyleImage,percentStyle);
                smallBlendedCanvasPanel.setImage(previewBlendedImage);
            } else if (evtName.equals("prevStyle")) {
                styleNumber = styleNumber-1;
                if(styleNumber<0)
                    styleNumber = styleImages.length-1;
                setStyleImagePath(styleImages[styleNumber].getPath());
                previewStyleImage = loadStyle(styleImagePath);
                if(mainImagePath!=null){
                    previewBlendedImage = blendImage(previewImage,previewStyleImage,percentStyle);
                    smallBlendedCanvasPanel.setImage(previewBlendedImage);
                } else {
                    smallBlendedCanvasPanel.setImage(previewStyleImage);
                }

            } else if (evtName.equals("nextStyle")) {
                styleNumber ++;
                if(styleNumber>=styleImages.length)
                    styleNumber = 0;
                setStyleImagePath(styleImages[styleNumber].getPath());
                previewStyleImage = loadStyle(styleImagePath);
                if(mainImagePath!=null){
                    previewBlendedImage = blendImage(previewImage,previewStyleImage,percentStyle);
                    smallBlendedCanvasPanel.setImage(previewBlendedImage);
                } else {
                    smallBlendedCanvasPanel.setImage(previewStyleImage);
                }
            } else if (evtName.equals("runStyle")) {
                messageText.setText("running");
                setStyleSize( scaleValues[styleSizeList.getSelectedIndex()]);
                int fullStopPos = mainImagePath.lastIndexOf(".");
                int lastSlashPos = mainImagePath.lastIndexOf("\\")+1;
                String imageName = mainImagePath.substring(lastSlashPos, fullStopPos);
                fullStopPos = styleImagePath.lastIndexOf(".");
                lastSlashPos = styleImagePath.lastIndexOf("\\")+1;
                String styleImageName = styleImagePath.substring(lastSlashPos, fullStopPos);
                String suggestedFileName = imageName + " " + styleImageName + " " + getStyleSize() + " " + getPercentStyle();
                runStyle(suggestedFileName);
            } else if (evtName.equals("saveImage")) {
                try {
                    int fullStopPos = mainImagePath.lastIndexOf(".");
                    int lastSlashPos = mainImagePath.lastIndexOf("\\")+1;
                    String imageName = mainImagePath.substring(lastSlashPos, fullStopPos);
                    fullStopPos = styleImagePath.lastIndexOf(".");
                    lastSlashPos = styleImagePath.lastIndexOf("\\")+1;
                    String styleImageName = styleImagePath.substring(lastSlashPos, fullStopPos);
                    String suggestedFileName = imageName + " " + styleImageName + " " + getStyleSize() + " " + getPercentStyle() + ".jpg";
                    saveOutputImage(suggestedFileName);
                } catch (Exception e) {
                    saveOutputImage("unknown.jpg");
                }
            } else {

            }
        }
    }
    private BufferedImage loadImage(){
        String selDir = System.getProperty("user.dir") + File.separator + "testimages" + File.separator;
        FileChooser dc = new FileChooser(new JFrame(), true,
                "open", "jpg", "JPEG Files", selDir);
        dc.setVisible(true);
        setMainImagePath(dc.getFilePath());
        try {
            BufferedImage bi = ImageIO.read(new File(getMainImagePath()));
            BufferedImage smallImage = ImageUtility.resizeImage(bi, 1280,1280
                    , this);
            setImage(smallImage);
            return bi;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private BufferedImage loadStyle(String styleImagePath){
        try {
            BufferedImage bi = ImageIO.read(new File(styleImagePath));
            bi = ImageUtility.resizeImageToWandH(bi, previewSize, previewSize, this);
            smallCanvasPanel.setImage(bi);
            return bi;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveOutputImage(String suggestedFileName){
        String selDir = System.getProperty("user.dir") + File.separator + "output" + File.separator;
        FileChooser dc = new FileChooser(new JFrame(), true,"save", "jpg", "JPEG Files", selDir, suggestedFileName);
        dc.setVisible(true);
        String styleOutputFileName = dc.getFilePath();
        if (!styleOutputFileName.endsWith(".jpg")) {
            styleOutputFileName = styleOutputFileName + ".jpg";
        }
        try {
            ImageIO.write(getOutputImage(),"jpeg", new File(styleOutputFileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public BufferedImage getOutputImage() {
        return outputImage;
    }

    public void setOutputImage(BufferedImage outputImage) {
        this.outputImage = outputImage;
    }

    public Integer getPercentStyle() {
        return percentStyle;
    }

    public void setPercentStyle(Integer percentStyle) {
        this.percentStyle = percentStyle;
    }

    public Integer getStyleSize() {
        return styleSize;
    }

    public void setStyleSize(Integer styleSize) {
        this.styleSize = styleSize;
    }

    private void runStyle(String suggestedName){
        messageText.setText("running...");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        outputImage = StylizeBlendedModel.stylize(getMainImagePath(), getStyleImagePath(), getStyleSize(), getPercentStyle());
        BufferedImage smallImage = ImageUtility.resizeImage(outputImage, 1280,1280, this);
        setImage(smallImage);
        messageText.setText("complete " + suggestedName);
    }
    public BufferedImage getImage() {
        return canvasPanel.getImage();
    }

    public void setImage(BufferedImage image){
        canvasPanel.setImage(image);
    }

    public String getMainImagePath() {
        return mainImagePath;
    }

    public void setMainImagePath(String mainImagePath) {
        this.mainImagePath = mainImagePath;
    }

    public String getStyleImagePath() {
        return styleImagePath;
    }

    public void setStyleImagePath(String styleImagePath) {
        this.styleImagePath = styleImagePath;
    }
    private void scaleValReleased(MouseEvent mouseEvent){
        percentStyle = percentStyleSlider.getValue();
        if(mainImagePath!=null){
            previewBlendedImage = blendImage(previewImage,previewStyleImage,percentStyle);
            smallBlendedCanvasPanel.setImage(previewBlendedImage);
        } else {
            smallBlendedCanvasPanel.setImage(previewStyleImage);
        }
    }
    /**
     * return theColorPixel representation of color at x y coordinates
     *
     * @param x
     * @param y
     * @return
     */
    public Color getColorPixel(int x, int y) {
        int avgr = 0;
        int avgg = 0;
        int avgb = 0;
        int avga = 0;
        int[] pixels = new int[1];
        Color sample = null;
        try {
            if (sourceImage != null) {
                PixelGrabber pix = new PixelGrabber(sourceImage, x, y, 1, 1, pixels, 0, 1);
                pix.grabPixels();
                int pixel = pixels[0];
                avga = ((pixel >> 24) & 0xff);
                avgr = ((pixel >> 16) & 0xff);
                avgg = ((pixel >> 8) & 0xff);
                avgb = ((pixel) & 0xff);
                sample = new Color(avgr, avgg, avgb, avga);
                System.out.println(" a= " + avga + " r= " + avgr + " g= " + avgg + " b= " + avgb);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sample;
    }

    public BufferedImage blendImage (BufferedImage srcImage, BufferedImage blendImage, Integer ratio){
        BufferedImage blendedImage = new BufferedImage(previewSize, previewSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = blendedImage.createGraphics();

        for (int y = 0; y < previewSize;y++){
            for (int x = 0; x < previewSize;x++){
                Color srcColor = new Color(srcImage.getRGB(x,y));
                Integer srcR = srcColor.getRed();
                Integer srcG = srcColor.getGreen();
                Integer srcB = srcColor.getBlue();
                Color blendColor = new Color(blendImage.getRGB(x,y));
                Integer blendR = blendColor.getRed();
                Integer blendG = blendColor.getGreen();
                Integer blendB = blendColor.getBlue();

                float blendRatio = ((100.0f-ratio.floatValue())/100.0f);
                float srcRatio =  (ratio.floatValue()/100.0f);

                int newR =checkValid((int)((srcR.floatValue()*srcRatio) + (blendR.floatValue()*blendRatio)));
                int newG =checkValid((int)((srcG.floatValue()*srcRatio) + (blendG.floatValue()*blendRatio)));
                int newB =checkValid((int)((srcB.floatValue()*srcRatio) + (blendB.floatValue()*blendRatio)));
                Color blendedColor = new Color(newR,newG,newB);
                graphics2D.setColor(blendedColor);
                graphics2D.fillRect(x,y,1,1);

            }
        }
        return blendedImage;
    }
    private int checkValid(int val)
    {
        if (val > 255)
            val = 255;
        else if (val < 0)
            val = 0;
        return val;
    }
    /**
     *
     *
     * @return the canvasWidth
     */
    private int getCanvasWidth() {
        return canvasWidth;
    }

    /**
     * @param canvasWidth the canvasWidth to set
     */
    private void setCanvasWidth(int canvasWidth) {
        this.canvasWidth = canvasWidth;
    }

    /**
     * @return the canvasHeight
     */
    private int getCanvasHeight() {
        return canvasHeight;
    }

    /**
     * @param canvasHeight the canvasHeight to set
     */
    private void setCanvasHeight(int canvasHeight) {
        this.canvasHeight = canvasHeight;
    }

    /**
     * Exit the Application
     */
    private void exitForm(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_exitForm
        System.exit(0);
    }//GEN-LAST:event_exitForm

    public static void main (String[] params){
        MultiPanelImageDisplayer mid = new MultiPanelImageDisplayer(2560,1280);
        mid.setVisible(true);
    }
}
