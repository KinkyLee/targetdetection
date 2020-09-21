//package com.sds.targetdetection;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import static java.lang.Math.sqrt;

class TargetDetection {
    static final float DEFAULT_THRESHOLD = 10.0f;
    static final int DEFAULT_REGION = 10;

    private float mThreshold;
    float mRegion;
    int widthNarrowedDown;
    int heightNarrowedDown;

    class TargetNode implements Cloneable{
        int iRegion;
        int iLuminance;
        double fPositionY;
        double fPositionX;

        @Override
        protected TargetNode clone() throws CloneNotSupportedException  {
             return (TargetNode)super.clone();
        }
    }

    final static int DEFAULT_WIDTH_NARROWED_DOWN = 400;
    final static int DEFAULT_HEIGHT_NARROWED_DOWN = 300;
    public final static int CAPACITY_SIZE = 500;
    int mTargetNum;
    TargetNode[] mEigenList;
    int width;
    int height;
    
    TargetDetection(int width, int height, float _threshold, int _region, int _widthNarrowedDown, int _heightNarrowedDown) {
    	this.width = width;
    	this.height = height;
        this.mThreshold = _threshold;
        this.mRegion = _region;
        this.widthNarrowedDown = _widthNarrowedDown;
        this.heightNarrowedDown = _heightNarrowedDown;
    }

    private void labelingCluster(int[] iData, int height, int width)
    {
        int[][] Flag = new int[2][width];

        int i,j,k;
        int ClusterNum = 0;

        double LocalPointYSum;
        double LocalPointXSum;

        TargetNode pNode;
        TargetNode pNodelabel1;
        TargetNode pNodelabel2;
        TargetNode pNodelabel;

        int label1;
        int label2;
        int label;

        mTargetNum = 0;

        for ( i = 0; i < height; i++)
        {
            for ( j = 0; j < width; j++)
            {
                label1 = 0;
                label2 = 0;
                if (iData[i*width+j] > 0)
                {
                    if ( (i-1) >= 0 && (j-1) >= 0 && iData[(i-1)*width+j-1] > 0 )
                        label1 = Flag[0][j-1];

                    if ( (i-1) >= 0 && iData[(i-1)*width+j] > 0)
                        label1 = Flag[0][j];

                    if ( (i-1) >= 0 && (j+1) < width && iData[(i-1)*width+j+1] > 0)
                        label2 = Flag[0][j+1];

                    if ( (j-1) >= 0 && iData[i*width+j-1] > 0)
                        label1 = Flag[1][j-1];

                    if ( label1== 0 && label2 == 0 )
                    {
                        if((mTargetNum > 499)||(ClusterNum > 499))
                            break;

                        ClusterNum ++;
                        Flag[1][j] = ClusterNum;
                        LocalPointXSum = (j) * iData[i*width+j];
                        LocalPointYSum = (i) * iData[i*width+j];

                        pNode = mEigenList[ClusterNum-1];
                        pNode.fPositionX = LocalPointXSum;
                        pNode.fPositionY = LocalPointYSum;
                        pNode.iRegion = 1;
                        pNode.iLuminance = iData[i*width+j];

                        mTargetNum++;
                        continue;
                    }

                    if (label1 > 0 && label2 >0 && label1 != label2)
                    {
                        Flag[1][j] = label1;
                        if(i > 0)
                        {
                            for(k = 0; k < width; k++)
                                if(Flag[0][k] == label2)
                                    Flag[0][k] = label1;
                        }
                        for(k = 0; k < j; k++)
                            if(Flag[1][k] == label2)
                                Flag[1][k] = label1;

                        pNodelabel1 = mEigenList[label1-1];
                        pNodelabel2 = mEigenList[label2-1];


                        LocalPointXSum = (j) * iData[i*width+j];
                        LocalPointYSum = (i) * iData[i*width+j];

                        pNodelabel1.fPositionX = pNodelabel2.fPositionX + pNodelabel1.fPositionX + LocalPointXSum;
                        pNodelabel1.fPositionY = pNodelabel2.fPositionY + pNodelabel1.fPositionY + LocalPointYSum;
                        pNodelabel1.iRegion = pNodelabel2.iRegion + pNodelabel1.iRegion + 1;
                        pNodelabel1.iLuminance = pNodelabel2.iLuminance + pNodelabel1.iLuminance + iData[i*width+j];

                        pNodelabel2.iRegion = 0;
                        continue;
                    }

                    label = label1;
                    if (label2 > 0)
                    {
                        label = label2;
                    }
                    Flag[1][j] = label;
                    LocalPointXSum = (j) * iData[i*width+j];
                    LocalPointYSum = (i) * iData[i*width+j];

                    pNodelabel = mEigenList[label-1];
                    pNodelabel.fPositionX = pNodelabel.fPositionX + LocalPointXSum;
                    pNodelabel.fPositionY = pNodelabel.fPositionY + LocalPointYSum;
                    pNodelabel.iRegion = pNodelabel.iRegion + 1;
                    pNodelabel.iLuminance = pNodelabel.iLuminance + iData[i*width+j];
                }
            }

            if((mTargetNum > 499)||(ClusterNum > 499))
                break;

            System.arraycopy(Flag[1], 0, Flag[0], 0, width);
        }

        i = 0;
        int point = 0;
        while(i < mTargetNum)
        {
            if( mEigenList[point].iRegion <=1 || mEigenList[point].fPositionY < 1)
            {
                try {
                    mEigenList[point] = mEigenList[mTargetNum -1].clone();
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
                mTargetNum--;
            }
            else
            {
                point++;
                i++;
            }
        }

        i = 0;
        point = 0;

        while(i < mTargetNum)
        {
            pNode = mEigenList[point];
            pNode.fPositionX = (pNode.fPositionX * 10 / pNode.iLuminance ) / 10;
            pNode.fPositionY = (pNode.fPositionY * 10 / pNode.iLuminance ) / 10;
            pNode.iLuminance = (int) Math.round(((double) pNode.iLuminance)/(double) pNode.iRegion);
            point++;
            i++;
        }
    }

    private float std2(int[] iData, int height, int width, double pixMeanValue)
    {
        int i,j;
        double pixValueSum = 0;

        for (i=0; i<height; i++)
            for (j=0; j<width; j++)
                pixValueSum += (iData[i*width+j]-pixMeanValue)*(iData[i*width+j]-pixMeanValue);

	    return (float)sqrt(pixValueSum/(double)(width*height));
    }

    private float mean2(int[] iData, int height, int width)
    {
        int i,j;
        double pixValueSum = 0;

        for (i=0; i<height; i++)
            for (j=0; j<width; j++)
                pixValueSum += iData[i*width+j];

        return (float) (pixValueSum / (double)(width * height));
    }

    private void open_window(int[] iData, int height, int width, int[] oData)
    {
        int wndsize_h = height - heightNarrowedDown;
        int wndsize_w = width - widthNarrowedDown;

        for(int i=0; i<wndsize_h; i++) {
            for (int j = 0; j < wndsize_w; j++) {
                oData[i * wndsize_w + j] = iData[(i + heightNarrowedDown/2) * width + j + widthNarrowedDown/2];
            }
        }
    }

    private void separate(int[] iData, int[] oResult, int height, int width, double threshold)
    {
        for(int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                oResult[i * width + j] = (iData[i * width + j] > threshold) ? iData[i * width + j] : 0;
            }
        }
    }
    
    @SuppressWarnings("resource")
	public int[] filetoByteArray(String filename) throws IOException {
    	 
        FileChannel fc = null;
        try {
            fc = new RandomAccessFile(filename, "r").getChannel();
            MappedByteBuffer byteBuffer = fc.map(MapMode.READ_ONLY, 0,
                    fc.size()).load();
            System.out.println(byteBuffer.isLoaded());
            byte[] result = new byte[(int) fc.size()];
            int [] ret = new int[(int) fc.size()];
            if (byteBuffer.remaining() > 0) {
                // System.out.println("remain");
                byteBuffer.get(result, 0, byteBuffer.remaining());
            }
            int i = 0;
            for(byte b : result) {
            	ret[i] = result[i];
            	i++;
            }
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                fc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void targetDetect(String fileName) throws IOException
    {
        mEigenList = new TargetNode[CAPACITY_SIZE];
        for(int i=0; i<CAPACITY_SIZE; i++) {
            mEigenList[i] = new TargetNode();
        }
        mTargetNum = 0;      

        int w = this.width;
        int h = this.height;

        int[] curImage = filetoByteArray(fileName);
        
        int wndsize_h = h- heightNarrowedDown;
        int wndsize_w = w - widthNarrowedDown;
        int[] w_image = new int[wndsize_h * wndsize_w];

        open_window(curImage, h, w, w_image);

        float pixMeanValue;
        float pixSigmaValue;


        pixMeanValue = mean2(w_image,wndsize_h,wndsize_w);
        pixSigmaValue = std2(w_image,wndsize_h,wndsize_w, pixMeanValue);

        float threshold = pixMeanValue + mThreshold * pixSigmaValue;

        separate(w_image, w_image, wndsize_h, wndsize_w, threshold);

        labelingCluster(w_image, wndsize_h, wndsize_w);
    }
}
