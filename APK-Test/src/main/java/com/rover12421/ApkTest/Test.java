package com.rover12421.ApkTest;

import brut.apktool.Main;


public class Test {
    public static void main(String[] args) throws Exception {
		System.out.println("APK-Test Project");
		
		String path = "TestFile/";
		
		//dex file test
		String input = path + "test.dex";
		String output = path + "test.dex_out";		
//		Main.main(new String[]{"d", input, "-o", output});
		
		//伪加密测试		
		input = path + "jiamiFlag.apk";
		output = path + "jiamiFlag_out";
		Main.main(new String[]{"d", input, "-f", "-o", output});
		
	}
}
