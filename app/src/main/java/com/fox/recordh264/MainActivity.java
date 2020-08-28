package com.fox.recordh264;

import android.app.ActivityManager;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.app.AlertDialog;

import java.io.IOException;
import java.util.HashMap;
import com.unity3d.player.UnityPlayerActivity;
import android.media.MediaCodecList;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;

public class MainActivity extends UnityPlayerActivity {


    private AvcEncoder avcCodec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("Unity:onCreate");
        super.onCreate(savedInstanceState);

        ActivityManager am =(ActivityManager) getSystemService(this.ACTIVITY_SERVICE);
        ConfigurationInfo info = am.getDeviceConfigurationInfo();
        System.out.println(info.reqGlEsVersion);
    }

    public String ShowDialog(final String _title, final String _content) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(_title).setMessage(_content).setPositiveButton("Down", null);
                builder.show();
            }
        });

        return "This is Return value";
    }

    public String Test(String text) {
        System.out.println("Unity:" + text);
        return text;
    }


    public void Init(int framerate, int width, int height, String path) {

        avcCodec = new AvcEncoder(width, height, framerate, path);
        avcCodec.StartEncoderThread();
    }

    public void endThread() {
        if (avcCodec != null)
            avcCodec.StopThread();
    }

    //TODO:这里是两帧进入
    public void pushOneFrame(byte[] buffer, int width, int height) {
        if (AvcEncoder.YUVQueue.size() >= 10) {
            AvcEncoder.YUVQueue.poll();
            System.out.println("Unity:poll frame");
        }
        //System.out.println("Unity:pushOneFrame");
//
//        int oneFrameLeght=width*height*3/2;
//        byte[] frame1=new byte[oneFrameLeght];
//        byte[] frame2=new byte[oneFrameLeght];
//        System.arraycopy(buffer,0,frame1,0,frame1.length);
//        System.arraycopy(buffer,frame1.length,frame2,0,frame2.length);
//        YUVQueue.add(frame1);
//        YUVQueue.add(frame2);

        //AvcEncoder.YUVQueue.add(buffer);
    }


    public String GetEncodeInfo() {

        HashMap<String,String> hashMap=new HashMap<String, String>();

        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            if (!info.isEncoder()) {
                continue;
            }
            String[] types = info.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                String mine = types[j];
                System.out.println("Unity: mine:"+mine);

                String ttypes="";

                try {
                    MediaCodecInfo.CodecCapabilities capabilities = info.getCapabilitiesForType(mine);
                    for (int k = 0; k < capabilities.colorFormats.length; k++) {
                        int type = capabilities.colorFormats[k];
                        System.out.println("Unity: type:"+type);
                        ttypes+=(type+";");
                    }
                } catch (Exception e) {

                }
                hashMap.put(mine,ttypes);
            }
        }

        return hashMap.toString();

    }



    public void  MuxerMp4(String h264Path,String mp4Path,int width,int height,int rate)
    {
        try {
            MediaMuxer muxer = new MediaMuxer(mp4Path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            MediaFormat mediaFormat=MediaFormat.createAudioFormat("video/avc",width,height);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE,rate);
            int  videoTrackIndex=muxer.addTrack(mediaFormat);
            muxer.start();






        }catch (IOException e)
        {
            e.printStackTrace();
        }
    }



}



