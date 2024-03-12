package org.vaadin.example;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class Detector
{
	public static int offset;
	
	public ArrayList<Double> computeSimilarityScore(int[] designPatternVector, int[] codeVector)
	{
		offset = codeVector.length - designPatternVector.length;
		ArrayList<Double> similarityScores = new ArrayList<Double>();
		
		if (offset < 0) // Code graph smaller than design pattern graph
			return null;
		else
		{			
			// CCn = sum (f(x) * g(x)) / |f(x)| * |g(x)|
			// |A| = √((x*x) + (y*y) + (z*z)) + ...
			for (int j = 0; j <= offset; j++)
			{
				int sumProduct = 0;
				double similarityScore, designPatternMagnitude = 0, codeMagnitude = 0;
				for (int i = 0; i < designPatternVector.length; i++)
				{
					sumProduct = sumProduct + (designPatternVector[i] * codeVector[i+j]);
					designPatternMagnitude = designPatternMagnitude + (designPatternVector[i] * designPatternVector[i]);
					codeMagnitude = codeMagnitude + (codeVector[i] * codeVector[i]);
				}
				designPatternMagnitude = Math.sqrt(designPatternMagnitude);
				codeMagnitude = Math.sqrt(codeMagnitude);
				similarityScore = sumProduct / (designPatternMagnitude*codeMagnitude);
				similarityScores.add(similarityScore);
			}
		}
		return similarityScores;
	}
	
//	/*
//	 * Detect 1 code with many design patterns
//	 * 
//	 * Output: Most similar pattern + score + offset
//	 * */
//	public String detectMostSimilarPattern(String CDPath, String javaPath) throws Exception 
//	{
//		InputHandling CDHanlder = new InputHandling();
//		Map<String, int[]> designPatternVectors = CDHanlder.readCD(CDPath);
//		
//		// Java handler
//		JavaCodeHandling javaHandler = new JavaCodeHandling();
//		File javaFile = new File(javaPath);
//		InputStream javaInputStream = new FileInputStream(javaFile);
//		ArrayList<InputStream> javaStreams = new ArrayList<InputStream>();
//		javaStreams.add(javaInputStream);
//		int[] codeVector = javaHandler.readJava(javaStreams);
//		
//		System.out.println("Design Pattern Vectors: ");
//		for (int[] designPatternVector : designPatternVectors.values())
//			System.out.println(java.util.Arrays.toString(designPatternVector));
//		System.out.println();
//		System.out.println("Code Vector: ");
//		System.out.println(java.util.Arrays.toString(codeVector));
//		System.out.println();
//		
//		// Similarity score for each offset
//		Main obj = new Main();
//		double maxSimilarScore = 0.0;
//		int mostSimilarOffset = 0;
//		String mostSimilarPattern = "";
//		for (Map.Entry<String, int[]> entry : designPatternVectors.entrySet())
//		{
//			String key = entry.getKey();
//			int[] designPatternVector = entry.getValue();
//
//			System.out.println("Design Pattern Vector: "+java.util.Arrays.toString(designPatternVector));
//			ArrayList<Double> similarityScores = obj.computeSimilarityScore(designPatternVector, codeVector);
//			if (similarityScores == null)
//				System.out.println("Code graph smaller than design pattern graph.");
//			else 
//			{
//				System.out.println("Similarity score for offset (0 - " + offset + "):");
//				for(double score : similarityScores)
//				{
//					System.out.println(score);
//				}
//				System.out.println();
//				System.out.println("Maximum similarity score for pattern in "+key+" = " + Collections.max(similarityScores)+"; offset: "+similarityScores.indexOf(Collections.max(similarityScores)));
//				System.out.println();
//			}
//			
//			if (Collections.max(similarityScores) > maxSimilarScore)
//			{
//				maxSimilarScore = Collections.max(similarityScores);
//				mostSimilarOffset = similarityScores.indexOf(Collections.max(similarityScores));
//				mostSimilarPattern = key;
//			}
//		}
//		System.out.println();
//		return "Most similar pattern: "+mostSimilarPattern+"\nSimilarity score: " + maxSimilarScore +"\nOffset: "+mostSimilarOffset;
//	}
	
	public Map<String, ArrayList<InputStream>> unzipFile(File file, String extension) throws ZipException, IOException
	{
		Map<String, ArrayList<InputStream>> zip = new HashMap<String, ArrayList<InputStream>>();

		try (ZipFile zipFile = new ZipFile(file)) 
		{
		    Enumeration<? extends ZipEntry> entries = zipFile.entries();
		    while (entries.hasMoreElements()) 
		    {
		        ZipEntry entry = entries.nextElement();
		        
		        if (!entry.isDirectory()) // Entry is a file > check extension
		        {
		        	try (InputStream inputStream = zipFile.getInputStream(entry);)  // automatically close resource declared within try-with-resource block
		        	{
		        		 // Extract the file name and directory from the zip entry
		                String entryName = entry.getName();
		                if (entryName.endsWith(extension))
		                {
			        		String[] parts = entryName.split("/");
			                if (parts.length > 1) 
			                {
			                	String name;
			                	if (extension.equals(".uxf")) 		// class diagram --> file name
			                		name = parts[parts.length-1];
			                	else								// java program --> folder name
			                		name = parts[parts.length-2];
			                    
			                	// Read contents of the input stream into a byte array
			                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
			                    byte[] buffer = new byte[1024];
			                    int len;
			                    while ((len = inputStream.read(buffer)) > -1) {
			                        baos.write(buffer, 0, len);
			                    }
			                    baos.flush();

			                    // Create a ByteArrayInputStream from the byte array
			                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(baos.toByteArray());

			                    // If folder name is not present, create a new list
			                    zip.putIfAbsent(name, new ArrayList<>());
			                    // Map file to its folder
			                    zip.get(name).add(byteArrayInputStream);
			                }
		                }
		        	}
		            
		        }
		    }
		}	
		return zip;
	}
	
	/*
	 * Input: 1 (zip / java) file & 1 (zip / uxf)
	 * 
	 * Output: Most similar code (folderName) + score + offset
	 * */
	public String detectMostSimilarCode(File CDFile, String CDName, File javaFile, String javaName) throws Exception
	{
		/* Input handling */

		// Java handling
		JavaCodeHandling javaHandler = new JavaCodeHandling();
		Map<String, int[]> codeVectors = new HashMap<String, int[]>();
		
		if (javaName.endsWith(".zip")) // zip file
		{
			Map<String, ArrayList<InputStream>> javaZip1 = new HashMap<String, ArrayList<InputStream>>();
			Map<String, ArrayList<InputStream>> javaZip2 = new HashMap<String, ArrayList<InputStream>>();
			Map<String, ArrayList<InputStream>> javaZip3 = new HashMap<String, ArrayList<InputStream>>();
			javaZip1 = unzipFile(javaFile, ".java");
			javaZip2 = unzipFile(javaFile, ".java");
			javaZip3 = unzipFile(javaFile, ".java");
			
			// Process javaZip map -- 1 entry == 1 folder == 1 program
			for (Map.Entry<String, ArrayList<InputStream>> entry : javaZip1.entrySet())
			{
	            String key = entry.getKey();
	            ArrayList<InputStream> streams1 = entry.getValue();
	            ArrayList<InputStream> streams2 = javaZip2.get(key);
	            ArrayList<InputStream> streams3 = javaZip3.get(key);
	            int[] vector = javaHandler.readJava(streams1, streams2, streams3);
	            System.out.println("Java vector: "+ Arrays.toString(vector));
	            codeVectors.put(key, vector);
	        }
		}
		else if (javaName.endsWith(".java")) // single java file
		{
			ArrayList<InputStream> streams1 = new ArrayList<InputStream>();
			ArrayList<InputStream> streams2 = new ArrayList<InputStream>();
			ArrayList<InputStream> streams3 = new ArrayList<InputStream>();
			streams1.add(new FileInputStream(javaFile));
			streams2.add(new FileInputStream(javaFile));
			streams3.add(new FileInputStream(javaFile));
			int[] vector = javaHandler.readJava(streams1, streams2, streams3);
			codeVectors.put(javaFile.getName(), vector);
		}
		
		// CD handling
		InputHandling CDHandler = new InputHandling();
		Map<String, int[]> designPatternVectors = new HashMap<String, int[]>();
		
		if (CDName.endsWith(".zip")) // zip file
		{
			Map<String, ArrayList<InputStream>> CDZip = new HashMap<String, ArrayList<InputStream>>();
			CDZip = unzipFile(CDFile, ".uxf");
			
			// Process CDZip map -- 1 entry == 1 uxf == 1 pattern
			for (Map.Entry<String, ArrayList<InputStream>> entry : CDZip.entrySet()) 
			{
	            String key = entry.getKey();
	            ArrayList<InputStream> streams = entry.getValue();
	            for (InputStream stream : streams)
	            {
	            	int[] vector = CDHandler.readCD(stream);
	            	designPatternVectors.put(key, vector);
	            }
	        }
		}else if (CDName.endsWith(".uxf")) // single CD file
		{
			InputStream CDInputStream = new FileInputStream(CDFile);
        	int[] vector = CDHandler.readCD(CDInputStream);
        	designPatternVectors.put(CDFile.getName(), vector);
		}
		
		/* Display */
		System.out.println("Code Vectors: ");
		for (Map.Entry<String, int[]> entry : codeVectors.entrySet()) 
		{
			String key = entry.getKey();
			int[] vector = entry.getValue();
			
			System.out.println(""+key+": "+ Arrays.toString(vector));
		}	
		System.out.println();
		
		System.out.println("Design Pattern Vectors: ");
		for (Map.Entry<String, int[]> entry : designPatternVectors.entrySet()) 
		{
			String key = entry.getKey();
			int[] vector = entry.getValue();
			
			System.out.println(""+key+": "+ Arrays.toString(vector));
		}	
		System.out.println();
		
		/* Detection */ 
		// Similarity score for each offset
		Map <String, String> output = null;
		if (designPatternVectors != null && codeVectors != null)
		{
			output = new HashMap<String, String>();
			for (Map.Entry<String, int[]> patternEntry : designPatternVectors.entrySet())
			{
				double maxSimilarScore = 0.0;
				int mostSimilarOffset = 0;
				String mostSimilarCode = "";
				
				String patternKey = patternEntry.getKey();
				int[] designPatternVector = patternEntry.getValue();
	
				System.out.println("Design Pattern Vector: "+patternKey+": "+ Arrays.toString(designPatternVector));
				for (Map.Entry<String, int[]> codeEntry : codeVectors.entrySet())
				{
					String codeKey = codeEntry.getKey();
					int[] codeVector = codeEntry.getValue();
					
					System.out.println("Code Vector: "+codeKey+": "+ Arrays.toString(codeVector));
					ArrayList<Double> similarityScores = computeSimilarityScore(designPatternVector, codeVector);
					if (similarityScores == null)
						System.out.println("Code graph smaller than design pattern graph.");
					else 
					{
						System.out.println("Similarity score for offset (0 - " + offset + "):");
						for(double score : similarityScores)
						{
							System.out.println(score);
						}
						System.out.println("Maximum similarity score for "+patternKey+" pattern in "+codeKey+" = " + Collections.max(similarityScores)+"; offset: "+similarityScores.indexOf(Collections.max(similarityScores)));
						System.out.println();
						
						if (Collections.max(similarityScores) > maxSimilarScore)
						{
							maxSimilarScore = Collections.max(similarityScores);
							mostSimilarOffset = similarityScores.indexOf(Collections.max(similarityScores));
							mostSimilarCode = codeKey;
						}
					}
				}
				output.put(patternKey, "Most similar code: "+mostSimilarCode+"\nSimilarity score: " + maxSimilarScore +"\nOffset: "+mostSimilarOffset);
			}
		} else
			System.out.println("Pattern / code vector is null.");

		String stringOutput = "";
		if (!output.isEmpty())
		{
			for (Map.Entry<String, String> entry : output.entrySet())
			{
				stringOutput = stringOutput + entry.getKey()+"\n\n"+entry.getValue() + "\n\n\n";
			}
		}
		return stringOutput;
	}
	
//	public static void main(String[] args) throws Exception
//	{
//		/* Display */
//		String javaPath = "src/data/cleanCodeZip.zip";
//		File javaFile = new File(javaPath);
//		String CDPath = "src/data/patternZip.zip";
//		File CDFile = new File(CDPath);
//
//		Detector obj = new Detector();
//		obj.detectMostSimilarCode(CDFile, javaFile);
//	}

}
