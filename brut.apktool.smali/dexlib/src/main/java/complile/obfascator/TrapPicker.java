package complile.obfascator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class TrapPicker {

	private boolean hasModify;
	public static Trap mTrap = null;
	private boolean inited;
	
	public void pickFieldFile(String apkTmpPath) {

		mTrap = new Trap();
		
		fileLooper(new File(apkTmpPath+"/smali"));
	}
	
	
	
	
	
	public void fileLooper(File file){
		
		File[] files = file.listFiles();

		for (File f : files) {
			
			if(hasModify) return;
			
			if (f.isDirectory()) {
				fileLooper(f);
			} else if (f.isFile()) {
				copyFieldFile(f);
			}

		}
		
	}
	
	
	public void initClassInfo(File smaliFile){
		
		inited = true;
		
		String absolutePaht = smaliFile.getAbsolutePath();
		StringBuilder split1 = new StringBuilder("/smali/");
		StringBuilder split2 = new StringBuilder(".smali");
		
		//like  /Users/bigpie/RsApktool/brut.apktool/apktool-cli/muzhiwan.com_com.apesoup.spacegurufull/smali/com/apesoup/a/a/a.smali
		//to    com/apesoup/a/a/a
		String mClassPathInSmali = absolutePaht.split(split1.toString())[1].split(split2.toString())[0]; 
		
		String rawClassName = "L" + mClassPathInSmali/*.replace("/", ".")*/;
		String mClassName = rawClassName + "__";
//		absolutePaht.split(split2.toString())[0]+"_.smali";
		
		int lastIndex = absolutePaht.lastIndexOf(split2.toString());
		String finalClassPath = absolutePaht.substring(0, lastIndex) + "__.smali";;
		
		
		mTrap.setChangeClassName(mClassName);
		mTrap.setRawClassName(rawClassName);
		mTrap.setFinalClassPath(finalClassPath);
		
	}
	
	
	public void replaceClassName(StringBuilder sb){
		
		
	}

	public void copyFieldFile(File file) {

		BufferedReader br = null;
		FileWriter fileWriter = null;

		if (file.getName().contains(".smali")) {
			try {
				String line;
				boolean read_instance_fields = false;
				String conciseIdentity = null;
				StringBuilder sb = new StringBuilder();

				br = new BufferedReader(new FileReader(file));

				while ((line = br.readLine()) != null) {

					if (line.startsWith(".class")) {
						conciseIdentity = line.split("L")[1];
					} else if (line.equals("# instance fields")) {
						read_instance_fields = true;
					} else if (line.equals("# direct methods")) {
						read_instance_fields = false;
//						break;
					}

					if (read_instance_fields
							&& !line.equals("# instance fields")
							&& !line.equals("")) {
						
						if(!inited){
							initClassInfo(file);
						}
						
						int start = line.lastIndexOf(" ");
						String fieldLine = line.substring(start).trim();
						if (fieldLine.contains(";")) {
							fieldLine = fieldLine.replaceAll(";", "");
							conciseIdentity = "L" + conciseIdentity + "->"
									+ fieldLine + ";";
							mTrap.setConciseIdentity(conciseIdentity);
						}

					}

					
					sb.append(line).append("\n");
				}

				if(inited){
				String contentTemp = sb.toString().replaceAll(mTrap.getRawClassName(), mTrap.getChangeClassName());
				sb.delete(0, sb.length()).append(contentTemp);
				
				fileWriter = new FileWriter(mTrap.getFinalClassPath());
				fileWriter.write(sb.toString());
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {

				try {
					if (fileWriter != null) {

						fileWriter.flush();
						fileWriter.close();
						hasModify = true;
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				try {
					if (br != null) {
						br.close();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

}
