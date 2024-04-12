package org.vaadin.example;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Detector
{
	public static int offset;
	public static Map<Integer, String> relationRootValue = new HashMap<>();
	public static Map<String, Map<int[], ArrayList<String>>> programs;

	public Detector()
	{
		programs = new HashMap<>();
		relationRootValue.put(2, "Association One-to-One Multiplicity");
		relationRootValue.put(3, "Association One-to-Many Multiplicity");
		relationRootValue.put(5, "Generalization One-to-Many Multiplicity");
	}
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
					codeMagnitude = codeMagnitude + (codeVector[i+j] * codeVector[i+j]);
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
//		InputHandling CDHandler = new InputHandling();
//		Map<String, int[]> designPatternVectors = CDHandler.readCD(CDPath);
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
	
	public Map<String, ArrayList<InputStream>> unzipFile(File file, String fileName, String extension) throws IOException
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
		        	try (InputStream inputStream = zipFile.getInputStream(entry))  // automatically close resource declared within try-with-resource block
		        	{
		        		 // Extract the file name and directory from the zip entry
		                String entryName = entry.getName();
		                if (entryName.endsWith(extension))
		                {
			        		String[] parts = entryName.split("/");
							String key;
			                if (parts.length > 1) 	// Folders in zip
			                {
			                	if (extension.equals(".uxf")) 		// class diagram --> file name
									key = parts[parts.length-1];
			                	else								// java program --> folder name
									key = parts[parts.length-2];
			                }
							else { 		// Files in zip
								if (extension.equals(".uxf")) 		// class diagram --> file name
									key = entryName;
								else								// java program --> folder name
									key = fileName;
							}

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
							zip.putIfAbsent(key, new ArrayList<>());
							// Map file to its folder
							zip.get(key).add(byteArrayInputStream);
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
		
		if (javaName.endsWith(".zip")) // zip file
		{
			Map<String, ArrayList<InputStream>> javaZip1;
			Map<String, ArrayList<InputStream>> javaZip2;
			Map<String, ArrayList<InputStream>> javaZip3;
			javaZip1 = unzipFile(javaFile, javaName, ".java");
			javaZip2 = unzipFile(javaFile, javaName, ".java");
			javaZip3 = unzipFile(javaFile, javaName, ".java");
			
			// Process javaZip map -- 1 entry == 1 folder == 1 program
			for (Map.Entry<String, ArrayList<InputStream>> entry : javaZip1.entrySet())
			{
	            String key = entry.getKey();
	            ArrayList<InputStream> streams1 = entry.getValue();
	            ArrayList<InputStream> streams2 = javaZip2.get(key);
	            ArrayList<InputStream> streams3 = javaZip3.get(key);
				Map<int[], ArrayList<String>> program = javaHandler.readJava(streams1, streams2, streams3);
				for (int[] vector : program.keySet())
				{
					System.out.println("Java vector: " + Arrays.toString(vector));
					if (vector.length != 0)
						programs.put(key, program);
				}
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
			Map<int[], ArrayList<String>> program = javaHandler.readJava(streams1, streams2, streams3);
			for (int[] vector : program.keySet())
			{
				System.out.println("Java vector: " + Arrays.toString(vector));
				if (vector.length != 0)
					programs.put(javaName, program);
			}
		}
		
		// CD handling
		InputHandling CDHandler;
		Map<String, int[]> designPatternVectors = new HashMap<String, int[]>();
		Map<String, String> errorMessage = new HashMap<>();

		if (CDName.endsWith(".zip")) // zip file
		{
			Map<String, ArrayList<InputStream>> CDZip;
			CDZip = unzipFile(CDFile, CDName, ".uxf");
			
			// Process CDZip map -- 1 entry == 1 uxf == 1 pattern
			for (Map.Entry<String, ArrayList<InputStream>> entry : CDZip.entrySet()) 
			{
	            String key = entry.getKey();
	            ArrayList<InputStream> streams = entry.getValue();
	            for (InputStream stream : streams)
	            {
					CDHandler = new InputHandling();
	            	int[] vector = CDHandler.readCD(stream, CDHandler);
					if (!CDHandler.getErrorMessage().isEmpty())
						errorMessage.put(key, CDHandler.getErrorMessage());
					if (vector.length != 0)
	            		designPatternVectors.put(key, vector);
	            }
	        }
		}else if (CDName.endsWith(".uxf")) // single CD file
		{
			CDHandler = new InputHandling();
			InputStream CDInputStream = new FileInputStream(CDFile);
        	int[] vector = CDHandler.readCD(CDInputStream, CDHandler);
			if (!CDHandler.getErrorMessage().isEmpty())
				errorMessage.put(CDName, CDHandler.getErrorMessage());
			if (vector.length != 0)
        		designPatternVectors.put(CDName, vector);
		}
		
		/* Display */
		System.out.println("Code Vectors: ");

		for (Map.Entry<String, Map<int[], ArrayList<String>>> entry : programs.entrySet())
		{
			String key = entry.getKey(); 		// Program name
			Map<int[], ArrayList<String>> vectorAndClassNames = entry.getValue();

			for (int[] vector : vectorAndClassNames.keySet())
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
		Map <String, String> output = new HashMap<String, String>();
		if (!errorMessage.isEmpty())
			output.putAll(errorMessage);
		else if (!designPatternVectors.isEmpty() && !programs.isEmpty())
		{
			Map<String, ArrayList<int[]>> patternMaxSimilarCodeVector = new HashMap<>();
			Map<String, Map<int[], ArrayList<String>>> patternMaxSimilarCodeClassNames = new HashMap<>();
			for (Map.Entry<String, int[]> patternEntry : designPatternVectors.entrySet())
			{
				double maxSimilarScore = 0.0;
				int mostSimilarOffset = 0;
				String mostSimilarCode = "";
				Map<String, String>	outputText = new HashMap<>();
				
				String patternKey = patternEntry.getKey();
				int[] designPatternVector = patternEntry.getValue();
				int[] maxSimilarCodeVector = new int []{};
				int[] maxSimilarCodeOffset = new int []{};
				Map<int[], ArrayList<String>> maxSimilarCodeClassNames = new HashMap<>();

				System.out.println("Design Pattern Vector: "+patternKey+": "+ Arrays.toString(designPatternVector));
				for (Map.Entry<String, Map<int[], ArrayList<String>>> codeEntry : programs.entrySet())
				{
					String codeKey = codeEntry.getKey(); 		// Program name
					Map<int[], ArrayList<String>> vectorAndClassNames = codeEntry.getValue();

					int[] codeVector = new int[0];
					for (int[] vector : vectorAndClassNames.keySet())	// Should only contain 1 vector
						codeVector = vector;
					System.out.println("Code Vector: "+codeKey+": "+ Arrays.toString(codeVector));

					ArrayList<Double> similarityScores = computeSimilarityScore(designPatternVector, codeVector);
					if (similarityScores == null)
						outputText.put(codeKey, "Code graph smaller than design pattern graph.");
					else 
					{
						// Find max similarity offset within a program
						System.out.println("Similarity score for offset (0 - " + offset + "):");
						for(double score : similarityScores)
						{
							System.out.println(score);
						}
						System.out.println("Maximum similarity score for "+patternKey+" pattern in "+codeKey+" = " + Collections.max(similarityScores)+"; offset: "+similarityScores.indexOf(Collections.max(similarityScores)));
						System.out.println();

						// Find max similarity program among all programs
						if (Collections.max(similarityScores) > maxSimilarScore)
						{
							maxSimilarScore = Collections.max(similarityScores);
							mostSimilarOffset = similarityScores.indexOf(maxSimilarScore);
							mostSimilarCode = codeKey;
							outputText.put(codeKey,"Most similar code: "+mostSimilarCode+"\nSimilarity score: " + maxSimilarScore +"\nOffset: "+mostSimilarOffset);
							maxSimilarCodeVector = codeVector;
							maxSimilarCodeOffset = new int[] {mostSimilarOffset};
							maxSimilarCodeClassNames = vectorAndClassNames;
						}
					}
				}
				if (mostSimilarCode.isEmpty()) // All code graph's similarityScores == null
					output.put(patternKey, "Code graph smaller than design pattern graph.");
				else
				{
					patternMaxSimilarCodeVector.putIfAbsent(patternKey, new ArrayList<>());
					patternMaxSimilarCodeVector.get(patternKey).add(designPatternVector);
					patternMaxSimilarCodeVector.get(patternKey).add(maxSimilarCodeVector);
					patternMaxSimilarCodeVector.get(patternKey).add(maxSimilarCodeOffset);

					patternMaxSimilarCodeClassNames.put(patternKey, maxSimilarCodeClassNames);
					String positionOutput = displayCodeNamedMappedRelation(patternMaxSimilarCodeVector, patternMaxSimilarCodeClassNames);
					String completeOutput = outputText.get(mostSimilarCode)+"\n"+positionOutput;
					output.put(patternKey, completeOutput);
				}
			}
		}
		else if (designPatternVectors.isEmpty())
			output.put(CDName, "No class found in class diagram.");
		else if (programs.isEmpty())
			output.put(javaName, "No class found in java program.");

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

	// Back propagation - display selected code's relation mapping (position)
	public String displayCodeNamedMappedRelation(Map<String, ArrayList<int[]>> patternMaxSimilarCodeVector, Map<String, Map<int[], ArrayList<String>>> patternMaxSimilarCodeClassNames)
	{
		String output = "";
		// patternMaxSimilarCodeVector = {Key: patternName, Value: [designPatternVector, MostSimilarCodeVector, MostSimilarCodeOffset]}
		for (Map.Entry<String, ArrayList<int[]>> outputEntry : patternMaxSimilarCodeVector.entrySet())
		{
			String pattern = outputEntry.getKey();
			ArrayList<int[]> mostSimilar = outputEntry.getValue();
			int patternVectorLength = mostSimilar.get(0).length;
			int[] codeVector = mostSimilar.get(1);
			int codeVectorLength = codeVector.length;
			int offset = mostSimilar.get(2)[0];
			int matrixSize = (int) Math.sqrt(codeVectorLength);
			int[][] mostSimilarCodeMatrix = new int[matrixSize][matrixSize];

			// Clean code vector
			for (int i = 0; i < codeVectorLength; i++)
			{
				if (i < offset || i >= (offset + patternVectorLength)) // Clear other values to 1
					codeVector[i] = 1;
			}
			System.out.println(Arrays.toString(codeVector));

			// Construct clean code matrix
			int vectorIndex = 0;
			for (int row = 0; row < matrixSize; row++)
			{
				for (int column = 0; column < matrixSize; column++)
				{
					mostSimilarCodeMatrix[row][column] = codeVector[vectorIndex];
					vectorIndex++;
				}
			}
			System.out.println("Clean code matrix:");
			for (int[] row : mostSimilarCodeMatrix)
			{
				for (int element : row) {
					System.out.print(element + " ");
				}
				System.out.println(); // Move to the next line after printing each row
			}
			System.out.println();

			// Find relations
			Map<String, List<Integer[]>> mappedRelation = new HashMap<>();
			for (int row = 0; row < matrixSize; row++)
			{
				for (int column = 0; column < matrixSize; column++)
				{
					if (mostSimilarCodeMatrix[row][column] != 1) // Have relation
					{
						ArrayList<String> relations = new ArrayList<>();
						// Association 1..1 == 2; Association 1..* == 3; Generalization 1..1 == 5;
						// possible values: 2, 3, 5, 10, 15;
						int relationValue = mostSimilarCodeMatrix[row][column];
						if (relationRootValue.containsKey(relationValue)) 			// Single relation
							relations.add(relationRootValue.get(relationValue));
						else	// Multi relations
						{
							for (Integer rootValue : relationRootValue.keySet())
							{
								if (relationValue % rootValue == 0)		// Consists this relation
									relations.add(relationRootValue.get(rootValue));
							}
						}

						// Map relations to class index
						for (String relation : relations)
						{
							// If the key is not present, create a new list
							mappedRelation.putIfAbsent(relation, new ArrayList<>());
							// Map class index to relation label
							mappedRelation.get(relation).add(new Integer[]{row, column});
						}
					}
				}
			}
			System.out.println("Relation mapped to class index:");
			for (Map.Entry<String, List<Integer[]>> entry : mappedRelation.entrySet()) {
				String key = entry.getKey();
				List<Integer[]> values = entry.getValue();

				System.out.println("Key: " + key);
				for (Integer[] array : values) {
					System.out.println("  " + Arrays.toString(array));
				}
			}
			System.out.println();

			// Get mappedRelation's class names (sync through patternName, 1 pattern should only have 1 most similar code)
			// patternMaxSimilarCodeVector = {Key: patternName, Value: [designPatternVector, MostSimilarCodeVector, MostSimilarCodeOffset]}
			// patternMaxSimilarCodeClassNames = {Key: patternName, Value: {Key: MostSimilarCodeVector, Values: MostSimilarCodeClassNames}}
			Map<int[], ArrayList<String>> vectorAndClassNames = patternMaxSimilarCodeClassNames.get(pattern);
			ArrayList<String> classNames = null;
			for (Map.Entry<int[], ArrayList<String>> vectorAndClassNamesEntry : vectorAndClassNames.entrySet())
				classNames = vectorAndClassNamesEntry.getValue();

			Map<String, List<String[]>>  namedMappedRelation = new HashMap<>();
			for (Map.Entry<String, List<Integer[]>> mappedRelationEntry : mappedRelation.entrySet())
			{
				String relationName = mappedRelationEntry.getKey();
				List<Integer[]> classPairs = mappedRelationEntry.getValue();

				for (Integer[] classPair : classPairs)		// 1 class pair = 1 relationship
				{
					String startClassName = classNames.get(classPair[0]);
					String endClassName = classNames.get(classPair[1]);

					namedMappedRelation.putIfAbsent(relationName, new ArrayList<>());
					namedMappedRelation.get(relationName).add(new String[] {startClassName, endClassName});
				}
			}

			// Display namedMappedRelation
			System.out.println("Pattern: "+pattern);
			output = "Position: ";
			for (Map.Entry<String, List<String[]>> namedMappedRelationEntry : namedMappedRelation.entrySet())
			{
				String relationName = namedMappedRelationEntry.getKey();
				List<String[]> relationClasses = namedMappedRelationEntry.getValue();

				System.out.println("Key: " + relationName);
				output = output + "\n" + relationName;
				for (String[] array : relationClasses) {
					System.out.println("  " + Arrays.toString(array));
					output = output + "\n" + Arrays.toString(array);
				}
				output = output + "\n";
			}
			System.out.println();
		}
		return output;
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
