package com.fox.recordh264;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import com.fox.recordh264.decode.H264Decoder;
import com.fox.recordh264.file.H264ReadRunable;

import java.nio.ByteBuffer;

public class H264MuxerMp4 {

    String TAG = getClass().getSimpleName();

    private H264Decoder mVideoDecode;
    private Mp4Muxer mMp4Muxer;//=new Mp4Muxer(FileConstant.mp4FilePath);
    private MediaFormat vFormat;
    private Context context;

    private String h264Path;
    private String mp4Path;
    private int width;
    private int height;
    boolean startMuxer;
    private long startPts;
    private boolean haveGetSpsInfo;



    public  void MuxerMp4(Context context,String h264Path,String mp4Path,int width,int height,int rate)
    {
        Log.d("Unity:","MuxerMp4");
        this.context=context;
        this.h264Path=h264Path;
        this.mp4Path=mp4Path;
        this.width=width;
        this.height=height;
        mMp4Muxer=new Mp4Muxer(mp4Path);
        initVideoCodec();
        readLocalFile();
    }



    private void initVideoCodec() {
        mVideoDecode = new H264Decoder(context,width,height);
        mVideoDecode.initCodec();
        mVideoDecode.start();

        mVideoDecode.setH264DecoderListener(new H264Decoder.H264DecoderListener() {
            @Override
            public void ondecode(byte[] out, MediaCodec.BufferInfo info) {

            }

            @Override
            public void outputFormat(MediaFormat outputFormat) {
//                vFormat = outputFormat;
//                vFormat.setString(MediaFormat.KEY_MIME, "video/avc");
                vFormat = mVideoDecode.getMediaformat();
                startMuxer();
            }
        });
    }

    private void startMuxer() {
        if (startMuxer) {
            return;
        }
        if (vFormat != null ) {
            mMp4Muxer.addVideoTrack(vFormat);
            mMp4Muxer.start();
            startMuxer = true;
        }
    }



    private void readLocalFile() {
        H264ReadRunable h264ReadRunable = new H264ReadRunable(h264Path);
        h264ReadRunable.setH264ReadListener(new H264ReadRunable.H264ReadListener() {
            @Override
            public void onFrameData(byte[] datas) {
                mVideoDecode.decodeFrame(datas);
                //找sps和pps
                if ((datas[4] & 0x1f) == 7) {//sps
                    mVideoDecode.getMediaformat().setByteBuffer("csd-0", ByteBuffer.wrap(datas));
                    Log.d(TAG, "onFrameData:sps ");
                } else if ((datas[4] & 0x1f) == 8) {//pps
                    mVideoDecode.getMediaformat().setByteBuffer("csd-1", ByteBuffer.wrap(datas));
                    haveGetSpsInfo = true;
                } else if ((datas[4] & 0x1f) == 5) {
                    //第一帧为I帧
//                    haveGetSpsInfo = true;
//                    addMuxerVideoData(datas);
                }
                if (!startMuxer) {
                    return;
                }
                if (haveGetSpsInfo) {
                    Log.d(TAG, "onFrameData: -->datas[4]:" + datas[4]);
                    addMuxerVideoData(datas);
                    return;
                }


            }

            @Override
            public void onStopRead() {
                mVideoDecode.release();
                mMp4Muxer.stop();
                startMuxer=false;
                Log.d("Unity:","MuxerMp4  over");
            }
        });
        Thread readFileThread = new Thread(h264ReadRunable);
        readFileThread.start();
    }



    private void addMuxerVideoData(byte[] datas) {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        if (mMp4Muxer == null) return;
        bufferInfo.offset = 0;
        bufferInfo.size = datas.length;
        if ((datas[4] & 0x1f) == 5) {
            Log.d(TAG, "onDecodeVideoFrame: -->I帧");
            bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
        } else if ((datas[4] & 0x1f) == 7 || (datas[4] & 0x1f) == 8) {
            Log.d(TAG, "addMuxerVideoData: -->sps or pps");
            bufferInfo.flags = MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
        } else {
            bufferInfo.flags = 0;
        }
        ByteBuffer buffer = ByteBuffer.wrap(datas, bufferInfo.offset, bufferInfo.size);
        long pts = System.nanoTime() / 1000;
        if (startPts == 0) {
            startPts = pts;
        }
        bufferInfo.presentationTimeUs = System.nanoTime() / 1000 - startPts;
        mMp4Muxer.writeVideoData(buffer, bufferInfo);
    }
}
