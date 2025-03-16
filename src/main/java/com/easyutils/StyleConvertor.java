package com.easyutils;

public class StyleConvertor {

    public static String camelToSplitName(String camelName,String split){
        if(camelName==null|| camelName.isEmpty()){
            return camelName;
        }
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < camelName.length(); i++) {
            char ch = camelName.charAt(i);
            if(ch>='A' && ch<='Z') {
                if(i>0){
                    buf.append(split);
                }
                ch = Character.toLowerCase(ch);
            }
            buf.append(ch);
        }
        return buf.toString();
    }
}
