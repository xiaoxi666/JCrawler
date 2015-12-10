package com.youdao.dict.util;

import com.google.gson.Gson;
import com.youdao.dict.souplang.SoupLang;

import java.io.FileInputStream;
import java.util.*;

/**
 * Created by Administrator on 2015/11/6.
 */
public class TypeDictHelper {
    private static Map<String, String> typeDict = new HashMap<String, String>();
    private static Set<String> typeSet = new HashSet<String>();
    static {
        Properties dictProp=new Properties();
        try
        {
            dictProp.load(TypeDictHelper.class.getClassLoader().getResourceAsStream("conf/typesDict.properties"));
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        Iterator<Map.Entry<Object, Object>> ppsi = dictProp.entrySet().iterator();
        while(ppsi.hasNext())
        {
            Map.Entry<Object,Object> e = ppsi.next();
            String strKey = (String)e.getKey();
            typeSet.add(strKey);
            String strValue = (String)e.getValue();
            if(strKey == null || strValue == null || strValue.equals("")) continue;
            String[] words = strValue.split(",");
            for(String s: words){
                typeDict.put(s.toLowerCase(), strKey);
            }
        }
    }

    public static boolean rightTheType(String orgType){
        if(orgType == null || orgType.equals("")) return  false;
        return typeSet.contains(orgType);
    }

    public static String getType(String key, String defult){
        if(typeDict.containsValue(key)) return key;//正好是app频道之一。返回

        String lkey = key.toLowerCase();

        String value = typeDict.get(lkey);
        if(value != null && !value.equals(key)) return value;



        //拆开来匹配
        for(String s: lkey.split(" ")){
            value = typeDict.get(s);
            if(value != null && !value.equals(s)) return value;
        }
        return defult;
    }

    public static String getMoreInfo(String key){
        if(!rightTheType(key)){
            Map<String, String> map = new HashMap<String, String>();
            map.put("orgType", key);
            String moreinfo = new Gson().toJson(map);
            return moreinfo;
        }
        return "";
    }

    public static void updateType(){

    }

    public static void main (String[] args){

    }
}
