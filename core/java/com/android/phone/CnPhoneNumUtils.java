package com.android.phone;

public class CnPhoneNumUtils {
    static final String[] KNOW_PREFIX = new String[] {
    	"+86",
    	"0086",
    	"106", //小灵通发端消息前缀
    	"12520", //移动飞信,
    	"17951",//移动ip拨号
    	"17909",//电信ip拨号
    	"12593",
    	"17911"
    };
    
	public static String formatCnPhone(String phoneNum){
		if(phoneNum == null)
			return null;
		
		String num = phoneNum.replace("-","");
		
    	for(int i = 0; i < KNOW_PREFIX.length;i++){
    		if(num.startsWith(KNOW_PREFIX[i])){
    			num = num.substring(KNOW_PREFIX[i].length());
    			return num;
    		}
    	}    	
		return num;
    }
}
