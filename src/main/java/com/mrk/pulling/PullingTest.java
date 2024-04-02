package com.mrk.pulling;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jiangbaojun
 * @version v1.0
 * @workid 1861
 * @date 2021/12/13 14:30
 */
public class PullingTest {


    public static void main(String[] args) {
        String LOCAL_PATH = "D:/personal/photo_focus/wallpaper/";
//        String LOCAL_PATH = "C:/Users/BaojunJiang/Desktop/1/";
        if(args!=null && args.length==1){
            LOCAL_PATH = args[0];
        }
        if(!(LOCAL_PATH.endsWith("/") || LOCAL_PATH.endsWith("\\"))){
            LOCAL_PATH = LOCAL_PATH+"/";
        }
        System.out.println("file store path: "+LOCAL_PATH);
        try {
            pullBing(LOCAL_PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        try {
//            pull360(LOCAL_PATH);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    /**
     * 获得360 images
     * @date 2021/12/14 09:27
     * @return void
     */
    private static void pull360(String LOCAL_PATH) throws IOException {
        int count = 20;
        LOCAL_PATH = LOCAL_PATH+"360/";
        String API_PATH = "http://wallpaper.apc.360.cn/index.php?c=WallPaper&a=getAllCategories";
        String URL_PATH = "http://wallpaper.apc.360.cn/index.php?c=WallPaper&a=getAppsByCategory&cid=%s&start=0&count=%d";
        String httpContent = HttpUtls.getHttpContent(API_PATH);
        JSONObject jsonObject = JSON.parseObject(httpContent);
        JSONObject data = jsonObject.getJSONObject("data");
        for (String id : data.keySet()) {
            try {
                JSONObject object = data.getJSONObject(id);
                Object name = object.get("name");
                String URL_BATCH = String.format(URL_PATH, id, count);
                String imagesContent = HttpUtls.getHttpContent(URL_BATCH);
                JSONObject imagesObject = JSON.parseObject(imagesContent);
                JSONArray imagesArray = imagesObject.getJSONArray("data");
                System.out.println(name+"【"+URL_BATCH+"】");
                for (int i = 0; i < imagesArray.size(); i++) {
                    JSONObject images = (JSONObject)imagesArray.get(i);
                    String url = images.getString("url");
                    String imageId = images.getString("id");
                    String utag = images.getString("utag").replaceAll("[\\s\\\\/:\\.\\?\\&=]+", "");
                    if(utag.length()>100){
                        utag = utag.substring(0,50);
                    }
                    String dateStr = images.getString("create_time").split(" ")[0];
                    String fileName = LOCAL_PATH+name+"/"+dateStr+"_"+imageId+utag+".jpg";
                    System.out.println(fileName);
                    File file = new File(fileName);
                    if(!file.exists()){
                        FileUtils.copyURLToFile(new URL(url), file);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获得bing images
     * @date 2021/12/14 09:27
     * @return void
     */
    private static void pullBing(String LOCAL_PATH) throws IOException, ParseException {
        LOCAL_PATH = LOCAL_PATH+"bing/";
        Map<String,File> existFilesDateMap = getExistFilesDate(LOCAL_PATH);
        String BING_API = "https://cn.bing.com/HPImageArchive.aspx?format=js&idx=0&n=10&nc=1612409408851&pid=hp&FORM=BEHPTB&uhd=1&uhdwidth=3840&uhdheight=2160";
        String BING_URL = "https://cn.bing.com";
        String httpContent = HttpUtls.getHttpContent(BING_API);
        JSONObject jsonObject = JSON.parseObject(httpContent);
        JSONArray jsonArray = jsonObject.getJSONArray("images");
        String specialKey = "�";
        int size = jsonArray.size();
        for (int i = size-1; i >= 0; i--) {
            jsonObject = (JSONObject)jsonArray.get(i);
            String url = BING_URL + jsonObject.get("url");
            url = url.substring(0, url.indexOf("&"));
            String copyright = (String)jsonObject.get("copyright");
            String name = (String)jsonObject.get("title");
            String startDate = (String)jsonObject.get("startdate");
            Date date = new SimpleDateFormat("yyyyMMdd").parse(startDate);
            String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(date);
            File existFile = existFilesDateMap.get(dateStr);
            boolean specialName = false;
            if(copyright!=null && copyright.indexOf(" (")>1){
                name = copyright.substring(0, copyright.indexOf(" ("));
            }
            if(name.contains(specialKey)){
                specialName = true;
                name = name.replaceAll(specialKey, "");
            }
            if(existFile!=null){
                if(specialName && !existFile.getName().contains(specialKey)){
                    //已存在
                    continue;
                }else{
                    existFile.delete();
                }
            }
            String fileName = LOCAL_PATH+dateStr+"_"+name+".jpg";
            System.out.println(fileName);
            File file = new File(fileName);
            if(!file.exists()){
                FileUtils.copyURLToFile(new URL(url), file);
            }
        }
    }

    private static Map<String, File> getExistFilesDate(String localPath) {
        Map<String, File> map = new HashMap<>();
        File filePath = new File(localPath);
        if(!filePath.exists()){
            filePath.mkdirs();
        }
        Collection<File> files = FileUtils.listFiles(filePath, new String[]{"jpg", "jpeg"}, false);
        for (File file : files) {
            if(file.isDirectory()){
                continue;
            }
            String fileName = file.getName();
            String[] split = fileName.split("_");
            String dateStr = split[0];
            if(dateStr.length()==10){
                map.put(dateStr, file);
            }
        }
        return map;
    }
}
