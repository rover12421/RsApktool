package complile.obfascator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

public class TrapPicker {

	private boolean hasModify;
	private int trapCount;
	public static  HashMap<String, Trap> trapMap;
	private String apkTmpPath;
	
	public void pickFieldFile(String apkTmpPath) {

		trapCount = 10;
		this.apkTmpPath = apkTmpPath;
		trapMap = new HashMap<>();
		
		fileLooper(new File(apkTmpPath+"/smali"));
	}
	
	
	
	
	
	public void fileLooper(File file){
		
		File[] files = file.listFiles();

		for (File f : files) {
			
			if(trapMap.size() >= trapCount) return;
			
			if (f.isDirectory()) {
				fileLooper(f);
			} else if (f.isFile()) {
				copyFieldFile(f);
			}

		}
		
	}
	
	
	public Trap initClassInfo(File smaliFile){
		
		Trap mTrap = new Trap();
		
		String absolutePath = smaliFile.getAbsolutePath();
		StringBuilder split1 = new StringBuilder("smali");
		StringBuilder split2 = new StringBuilder(".smali");
		
		//like  /Users/bigpie/RsApktool/brut.apktool/apktool-cli/muzhiwan.com_com.apesoup.spacegurufull/smali/com/apesoup/a/a/a.smali
		//to    com/apesoup/a/a/a
		
		
		String mClassPathInSmali = absolutePath.split(split2.toString())[1]; 
		
		String rawClassName = "L" + mClassPathInSmali.substring(1).replace("\\", "/");
		String mClassName = rawClassName + "__";
		
		int lastIndex = absolutePath.lastIndexOf(split2.toString());
		String finalClassPath = absolutePath.substring(0, lastIndex) + "__.smali";
		
		int largeNum = new Random().nextInt(500000);
		
		mTrap.setChangeClassName(mClassName);
		mTrap.setRawClassName(rawClassName);
		mTrap.setFinalClassPath(finalClassPath);
		mTrap.setObfascatorNum(largeNum);
		
		return mTrap;
	}
	
	
	public void replaceClassName(StringBuilder sb){
		
		
	}

	public void copyFieldFile(File file) {

		BufferedReader br = null;
		FileWriter fileWriter = null;

		if (file.getName().contains(".smali") && !file.getName().contains("$")) {
			try {
				String line;
				boolean start_read_instance_fields = false;
				String conciseIdentity = null;
				StringBuilder sb = new StringBuilder();
				Trap currentTrap = null;

				br = new BufferedReader(new FileReader(file));

				while ((line = br.readLine()) != null) {
					

					if (line.startsWith(".class")) {
						conciseIdentity = line.split("L")[1];
					} else if (line.equals("# instance fields")) {
						start_read_instance_fields = true;
					} else if (line.equals("# direct methods")) {
						start_read_instance_fields = false;
					}else if (line.equals("# virtual methods")) {
						start_read_instance_fields = false;
					}

					if (start_read_instance_fields
							&& !line.equals("# instance fields")
							&& !line.equals("")) {
						
							currentTrap = initClassInfo(file);
						
						int start = line.lastIndexOf(" ");
						String fieldLine = line.substring(start).trim();
						if (fieldLine.contains(";")) {
							fieldLine = fieldLine.replace(";", "");
							conciseIdentity = currentTrap.getChangeClassName() + ";->"
									+ fieldLine + ";";
							currentTrap.setConciseIdentity(conciseIdentity);
							trapMap.put(conciseIdentity, currentTrap);
						}
					}
					sb.append(line).append("\n");
				}
				if(currentTrap!=null){
					
					String contentTemp = sb.toString().replace(currentTrap.getRawClassName(), currentTrap.getChangeClassName());
					sb.delete(0, sb.length()).append(contentTemp);
					
					fileWriter = new FileWriter(currentTrap.getFinalClassPath());
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
