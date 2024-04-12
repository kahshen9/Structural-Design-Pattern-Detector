package org.vaadin.example;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;

/*
 * 	Input handling for Java program:
 * 	Read (Identify), Map relations, Compute overall relation matrix, Compute code vector;
 * 	Input: java program ArrayList<InputStream>, Output: code vector.
 * */
public class JavaCodeHandling 
{
	public static ArrayList<String> classesAndInterfaces;
    public static Map<String, List<Integer[]>> mappedRelation;
    public static Map<String, List<String[]>> namedMappedRelation;
    public static Map<String, Integer> matrixRootValue = new HashMap<String, Integer>(); 	// Relation type's root value dictionary
    public static Map<String, int[][]> relationMatrixes;
    public static int[][] overallMatrix;
    public static int[] codeVector;
    
    public JavaCodeHandling() 
    {
    	classesAndInterfaces = new ArrayList<String>();
        mappedRelation = new HashMap<String, List<Integer[]>>();
        namedMappedRelation = new HashMap<String, List<String[]>>();
        relationMatrixes = new HashMap<String, int[][]>();
        
		/* Relation Matrix Root Value Dictionary */
		matrixRootValue.put("Association One-to-One Multiplicity", 2);
		matrixRootValue.put("Association One-to-Many Multiplicity", 3);
		matrixRootValue.put("Generalization One-to-Many Multiplicity", 5);
    }
    
	/*
	 * Identify classes in java file
	 * */
	public void identifyClasses(InputStream inputStream)
	{
		Scanner scan = new Scanner(inputStream);
        while (scan.hasNext()) 
        {
            String line = scan.nextLine().trim();
            String processedLine = line.replaceAll("\"(?:\\\\\"|[^\"])*\"|(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)",""); // Remove comments and texts in " "
            if (processedLine.contains("class") || processedLine.contains("interface")) 
            {
            	ArrayList<String> tokens = new ArrayList<String>();
            	
        		StringTokenizer st1 = new StringTokenizer(processedLine, " ");
        		while (st1.hasMoreTokens())
        			tokens.add(st1.nextToken().replaceAll("[^a-zA-Z0-9_]", ""));
        		
        		int classIndex = tokens.indexOf("class");
        		int interfaceIndex = tokens.indexOf("interface");
        		
        		if (classIndex != -1)
        			classesAndInterfaces.add(tokens.get(classIndex+1));
        		else if (interfaceIndex != -1)
        			classesAndInterfaces.add(tokens.get(interfaceIndex+1));
            }
        }
        scan.close();
	}
	
	/*
	 * Run after identifyClasses(File file).
	 * Identify generalization keywords in java file
	 * Forward mapping generalization relation to classes.
	 * 
	 * Assume "public class IPhone4sCharger implements Charger, IPhone" in 1 line.
	 * */
	public void identifyGeneralization(ArrayList<String> classesAndInterfaces, InputStream inputStream)
	{
		Scanner scan = new Scanner(inputStream);
        while (scan.hasNext()) 
        {
            String line = scan.nextLine().trim();
            String processedLine = line.replaceAll("\"(?:\\\\\"|[^\"])*\"|(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)",""); // Remove comments and texts in " "
            if (processedLine.contains("extends") || processedLine.contains("implements")) 
            {
            	ArrayList<String> tokens = new ArrayList<String>();
            	
        		StringTokenizer st1 = new StringTokenizer(processedLine, " ");
        		while (st1.hasMoreTokens())
        		{
        			String token = st1.nextToken().replaceAll("[^a-zA-Z0-9_]", ""); // Remove {} from class / interface name
        			if (!token.equals(""))
        				tokens.add(token); 
        		}
        		
        		int extendIndex = tokens.indexOf("extends");
        		int implementIndex = tokens.indexOf("implements");
        		
        		if (extendIndex != -1)
        		{
        			int parentClassIndex = classesAndInterfaces.indexOf(tokens.get(extendIndex + 1));
        			int childClassIndex = classesAndInterfaces.indexOf(tokens.get(extendIndex - 1));
        			
    				// If the key is not present, create a new list
        			mappedRelation.putIfAbsent("Generalization One-to-Many Multiplicity", new ArrayList<>());
                    // Map class index to relation label
        			mappedRelation.get("Generalization One-to-Many Multiplicity").add(new Integer[]{childClassIndex, parentClassIndex});
        		}
        		
        		if (implementIndex != -1)
        		{
        			int parentIndex = implementIndex + 1;
        			int childClassIndex = classesAndInterfaces.indexOf(tokens.get(implementIndex - 1));
        			
        			for (int i = parentIndex; i < tokens.size(); i++)
        			{
            			int parentClassIndex = classesAndInterfaces.indexOf(tokens.get(i));
            			
        				// If the key is not present, create a new list
            			mappedRelation.putIfAbsent("Generalization One-to-Many Multiplicity", new ArrayList<>());
                        // Map class names to relation label
            			mappedRelation.get("Generalization One-to-Many Multiplicity").add(new Integer[]{childClassIndex, parentClassIndex});
        			}
        		}
            }
        }
        scan.close();
	}
	
	/*
	 * Run after identifyGeneralization(ArrayList<String> classesAndInterfaces, File file).
	 * Identify association relationships in java file
	 * Forward mapping association relation to classes.
	 * 
	 * */
	public void identifyAssociation(ArrayList<String> classesAndInterfaces, InputStream inputStream) throws Exception 
	{
		try (Scanner scan = new Scanner(inputStream)) 
		{
			int bracketCount = 0, classCount = 0; 
			ArrayList<String> currentClasses = new ArrayList<String>(); // store current class name
			while (scan.hasNext()) 
			{
				String line = scan.nextLine().trim();
			    String processedLine = line.replaceAll("\"(?:\\\\\"|[^\"])*\"|(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)",""); // Remove comments and texts in " "

				ArrayList<String> tokens = new ArrayList<String>();
				StringTokenizer st1 = new StringTokenizer(processedLine, " \t");
				while (st1.hasMoreTokens())
					tokens.add(st1.nextToken());
			    
			    if (processedLine.contains("class") || processedLine.contains("interface")) 
			    {
					int classIndex = tokens.indexOf("class");
					int interfaceIndex = tokens.indexOf("interface");
					
					String name = null;
					if (classIndex != -1)
						name = tokens.get(classIndex+1);
					else if (interfaceIndex != -1)
						name = tokens.get(interfaceIndex+1);        			
					
					try 
			    	{            
						if (name != null) 
						{
							name = name.replace("{", "");
			    			for (String actualName : classesAndInterfaces)
			    			{
			    				if (name.equals(actualName))
			    				{
			    					currentClasses.add(actualName);
			    				}
			    			}
						}
						else 
							throw new Exception("Class/Interface name cannot be null.");	
			    	}catch (Exception e)
			    	{
			    		e.printStackTrace();
			    		throw new Exception("Class/Interface name cannot be null.");
			    	}

					if (processedLine.contains("{")) // same row as 'class' / 'interface'
					{
						bracketCount++;
						classCount++;
					}
					else // public class Charger \n{	 (different row as 'class' / 'interface')
					{
						// skip empty line (fast forward to '{')
						while (scan.hasNext()) 
				        {
							line = scan.nextLine().trim();
							processedLine = line.replaceAll("\"(?:\\\\\"|[^\"])*\"|(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)","");
							st1 = new StringTokenizer(processedLine, " \t");
							tokens = new ArrayList<String>();

							while (st1.hasMoreTokens())
								tokens.add(st1.nextToken());

							if (processedLine.contains("{"))
							{
								bracketCount++;
								classCount++;

								if (tokens.indexOf("{") != tokens.size()-1)  // variable declaration same line as class's "{"
									variableCheck(currentClasses, classCount, processedLine, tokens);
								break;
							}
				        }
					}
					
					if (bracketCount == classCount && processedLine.contains("}")) // same row as 'class' / 'interface'
			        {
			        	bracketCount--;
			        	currentClasses.remove(classCount-1);
			        	classCount--;
			        }
					
			    }
			    else if (processedLine.contains("(") && tokens.indexOf("new") == -1 && !processedLine.contains("{")) // have '(' but no 'new' and '{' == method with '{' in next row / abstract method
			    	continue;
			    else if (processedLine.contains("{")) // '{' within the class (for method, if else, try catch etc)
			    {
			    	bracketCount++;
			    	if (processedLine.contains("}")) // '{}' in the same line
			        {
			    		bracketCount--;
			        }
			    	else // '{}' in different lines
			    	{
			    		// skip inner code (fast forward to '}')
						while (scan.hasNext()) 
				        {
				            line = scan.nextLine().trim();
				            processedLine = line.replaceAll("\"(?:\\\\\"|[^\"])*\"|(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)","");
				            if (processedLine.contains("{"))
				            	bracketCount++;
				            
				            if (processedLine.contains("}"))
				            	bracketCount--;
				            
				            if (bracketCount == classCount) // Method's "}" found, back in class scope
				            	break;
				        }
			    	}
			    }
			    else if (bracketCount == classCount && processedLine.contains("}")) // End of class
			    {
			    	bracketCount--;
			    	currentClasses.remove(classCount-1);
			    	classCount--;
			    }
			    else if (bracketCount == classCount && classCount > 0) // Variables check: (Within class scope)
			    {
					variableCheck(currentClasses, classCount, processedLine, tokens);
			    }
			}
		}
	}

	private void variableCheck(ArrayList<String> currentClasses, int classCount, String processedLine, ArrayList<String> tokens)
	{
		// Check if line contains other class names
		for (String name : classesAndInterfaces)
		{
			// line contains other class name
			if (!name.equals(currentClasses.get(classCount-1)) && processedLine.contains(name))
			{
				int otherClassIndex = classesAndInterfaces.indexOf(name);
				String currentClassName = currentClasses.get(classCount-1);
				int currentClassIndex = classesAndInterfaces.indexOf(currentClassName);
				String label = null;

				// Check variable type (association's multiplicity)
				boolean isList = false, found = false;
				for (int i = 0; i < tokens.size(); i++)
				{
					String word = tokens.get(i);
					//IPhone != IPhone4sCharger, IPhone4sCharger[< == IPhone4sCharger
					if (word.matches(".*[\\[\\]<>].*") && word.contains(name)) // Matches == List == 1..*
					{
						// List<className> check
						String[] clean = word.replaceAll("[\\[\\]<>]", " ").split(" ");
						for (String cleanWord : clean)
						{
							if (name.equals(cleanWord))
							{
								isList = true;
								found = true;
								break;
							}
						}
					}
					else if (word.equals(name))
					{
						found = true;

						if (i + 1 < tokens.size() && i > 0)
						{
							if (tokens.get(i+1).matches(".*[\\[\\]<>].*") || tokens.get(i-1).matches(".*[\\[\\]<>].*")) // className []
							{
								isList = true;
								break;
							}
							else if (tokens.get(i+1).matches("[;=]+")) // className = / className ; --> variable name != variable type
								found = false;
						}
					}
				}

				if (found && !isList) // 1..1
					label = "Association One-to-One Multiplicity";
				else if (found)// isList, 1..*
					label = "Association One-to-Many Multiplicity";

				if (label != null)
				{
					// If the key is not present, create a new list
					mappedRelation.putIfAbsent(label, new ArrayList<>());
					// Map class names to relation label
					mappedRelation.get(label).add(new Integer[]{otherClassIndex, currentClassIndex});
				}
			}
		}
	}

	/*
	 * Run after identifyGeneralization & identifyAssociation (ArrayList<String> classesAndInterfaces, File file).
	 * Backward mapping relation to classes through mappedRelation.
	 *
	 * */
	public void relationBackwardMapping(Map<String, List<Integer[]>> mappedRelation)
	{
		Map<String, List<Integer[]>> toAdd = new HashMap<String, List<Integer[]>>(); 
		for (Map.Entry<String, List<Integer[]>> entry : mappedRelation.entrySet()) 
		{			
			String key = entry.getKey();
            List<Integer[]> values = entry.getValue();
            
            for (Integer[] coordinate : values) 
            { 
	            if (key.startsWith("Generalization")) // Generalization only 1 type of multiplicity
	            {  
	            	Integer[] backwardCoordinate = Arrays.copyOf(coordinate, coordinate.length);
	            	backwardCoordinate[0] = coordinate[1];
	            	backwardCoordinate[1] = coordinate[0];
	            	
                	toAdd.putIfAbsent(key, new ArrayList<>());
                	toAdd.get(key).add(backwardCoordinate);
	            }
	            else // Association
	            {
	            	int occurenceCount = 0;
	            	String label;
                	for (int j = 0; j < values.size(); j++)
                	{
            			Integer[] otherCoordinate = values.get(j);
            			if (coordinate[1] == otherCoordinate[1])
            				occurenceCount++;
                	}
                	
                	if (occurenceCount > 1)
            			label = "Association One-to-Many Multiplicity";
            		else 
            			label = "Association One-to-One Multiplicity";
                	
        			// If the key is not present, create a new list
                	toAdd.putIfAbsent(label, new ArrayList<>());
                	toAdd.get(label).add(new Integer[]{coordinate[1], coordinate[0]});
	            }
            }
        }
	    
		// Update the original map with the updated entries
		for (Map.Entry<String, List<Integer[]>> entry : toAdd.entrySet())
		{
			String key = entry.getKey();
            List<Integer[]> values = entry.getValue();
            for (Integer[] coordinate : values)
            {
            	mappedRelation.putIfAbsent(key, new ArrayList<>());
            	mappedRelation.get(key).add(coordinate);
            }          
		}
	}
	
	/*
	 * Run after relationBackwardMapping (Map<String, List<Integer[]>> mappedRelation).
	 * Map each relation to its classNames.
	 *
	 * */
	public void classNameMapping(ArrayList<String> classesAndInterfaces, Map<String, List<Integer[]>> mappedRelation, Map<String, List<String[]>>  namedMappedRelation)
	{
		for (Map.Entry<String, List<Integer[]>> entry : mappedRelation.entrySet()) 
		{
            String key = entry.getKey();
            List<Integer[]> values = entry.getValue();

            for (Integer[] coordinate : values)
            {
				if (coordinate[0] != -1 && coordinate[1] != -1)
				{
					String startClassName = classesAndInterfaces.get(coordinate[0]);
					String endClassName = classesAndInterfaces.get(coordinate[1]);

					namedMappedRelation.putIfAbsent(key, new ArrayList<>());
					namedMappedRelation.get(key).add(new String[]{startClassName, endClassName});
				}
            }
        }
	}
	
	 /* 
     * Construct 1 matrix for each relation.
     * Then, Construct overall relation matrix from all relation matrixes
     * 
     * */
    public void constructOverallRelationMatrix() 
    {
       	// Construct n*n matrix for each relation, n is the number of classes
    	int matrixSize = classesAndInterfaces.size();
    	for (Map.Entry<String, List<Integer[]>> entry : mappedRelation.entrySet()) 
    	{
            String key = entry.getKey(); // relation label
            List<Integer[]> values = entry.getValue(); // relations map classes
            int[][] relationMatrix = new int[matrixSize][matrixSize];
            
            for (int i = 0; i < matrixSize; i++) 
            {
                Arrays.fill(relationMatrix[i], 0);
            }
            
            // Mark 1 if there is a relation mapping between the 2 classes
            // Starting class = row
            for (Integer[] value : values) // mapped relation
            {
				if (value[0] != -1 && value[1] != -1)
				{
					int row = value[0], column = value[1];
					relationMatrix[row][column] = 1;
				}
            }

            // Add to relationMatrix map; (Key = relation label, value = relation matrix)
            relationMatrixes.put(key, relationMatrix);
    	}
    	
    	// Combine all relation matrixes to 1 overall matrix
    	// Overall matrix: cell value is the product of each matrix's root value to the power of its corresponding cell; (root^old cell value)
    	overallMatrix = new int[matrixSize][matrixSize];
        for (int i = 0; i < matrixSize; i++) 
        {
            Arrays.fill(overallMatrix[i], 1); // Initialize all values to 1 (product of each matrix's root value to the power of its corresponding cell)
        }
        
    	for (Map.Entry<String, int[][]> entry : relationMatrixes.entrySet()) 
    	{
            String key = entry.getKey();
            int[][] values = entry.getValue(); // Relation Matrix
            int rootValue = matrixRootValue.get(key);
            
            for (int row = 0; row < matrixSize; row++) // row index
            {
                for (int column = 0; column < matrixSize; column++) // column index
                {
                	overallMatrix[row][column] = overallMatrix[row][column] * (int) Math.pow(rootValue, values[row][column]);
                }
            }
        }
    }
    
    /* Convert overall matrix to vector for detection */
    public int[] getCodeVector()
    {
    	//Flatten 2D array to 1D array
    	codeVector = new int[overallMatrix.length*overallMatrix.length];

        int index = 0;
        for (int row = 0; row < overallMatrix.length; row ++) 
        {
			for (int column = 0; column < overallMatrix.length; column ++)
			{                           
				codeVector[index] = overallMatrix[row][column];
				index++;
			}
        }
    	return codeVector;
    }
	
	/*
	 * Input handling
	 * */
	public Map<int[], ArrayList<String>> readJava(ArrayList<InputStream> streams1, ArrayList<InputStream> streams2, ArrayList<InputStream> streams3) throws Exception
	{
		JavaCodeHandling handler = new JavaCodeHandling();
		int[] vector;
		
		if (streams1 != null && streams2 != null && streams3 != null)
		{
			if (streams1.size() > 1) // More than 1 java file
			{
				// Identify class & interface
				for (InputStream stream : streams1) // 1 stream == 1 file
					handler.identifyClasses(stream);
				
				// Identify relationships
				for (InputStream stream : streams2)
					handler.identifyGeneralization(classesAndInterfaces, stream);
				
				for (InputStream stream : streams3)
					handler.identifyAssociation(classesAndInterfaces, stream);
			}
			else 
			{
				InputStream stream1 = streams1.get(0);
				InputStream stream2 = streams2.get(0);
				InputStream stream3 = streams3.get(0);
				handler.identifyClasses(stream1);
				handler.identifyGeneralization(classesAndInterfaces, stream2);
				handler.identifyAssociation(classesAndInterfaces, stream3);
			}
		}
		else 
			throw new Exception("InputStream is null.");

		//handler.relationBackwardMapping(mappedRelation);
		handler.classNameMapping(classesAndInterfaces, mappedRelation, namedMappedRelation);
		handler.constructOverallRelationMatrix();
		vector = handler.getCodeVector();
		handler.display(vector);

		Map<int[], ArrayList<String>> vectorAndClassNames = new HashMap<>();
		vectorAndClassNames.put(vector, classesAndInterfaces);
		return vectorAndClassNames;
	}
	
	/*
	 * Display
	 * */
	public void display(int[] vector)
	{
		System.out.println("\nClasses and Interfaces:");
		for (String name : classesAndInterfaces) 
		{
			System.out.println(name);
		}
		System.out.println();
		
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
		
		System.out.println("Relation mapped to class name:");
		for (Map.Entry<String, List<String[]>> entry : namedMappedRelation.entrySet()) {
            String key = entry.getKey();
            List<String[]> values = entry.getValue();

            System.out.println("Key: " + key);
            for (String[] array : values) {
                System.out.println("  " + Arrays.toString(array));
            }
            System.out.println();
        }
		System.out.println();
		
		System.out.println("Relation matrixes:");
		for (Map.Entry<String, int[][]> entry : relationMatrixes.entrySet()) {
            String key = entry.getKey();
            int[][] values = entry.getValue();

            System.out.println("Key: " + key);
            for (int[] row : values) {
                for (int element : row) {
                    System.out.print(element + " ");
                }
                System.out.println(); // Move to the next line after printing each row
            }
        }
		System.out.println();
		
		System.out.println("Overall matrix:");
        for (int[] row : overallMatrix) {
            for (int element : row) {
                System.out.print(element + " ");
            }
            System.out.println(); // Move to the next line after printing each row
        }
        System.out.println();
        
        System.out.println("Code Vector:");
        System.out.print(Arrays.toString(vector));
        System.out.println();
        System.out.println();
	}
	
	public static void main(String[] args) throws Exception 
	{
//		JavaCodeHandling handler = new JavaCodeHandling();
//		Map<String, int[]> codeVectors = new HashMap<String, int[]>();
//		
//		File javaFile = new File("src/data/codeZip.zip");
//		Map<String, ArrayList<InputStream>> javaZip1 = new HashMap<String, ArrayList<InputStream>>();
//		Map<String, ArrayList<InputStream>> javaZip2 = new HashMap<String, ArrayList<InputStream>>();
//		Map<String, ArrayList<InputStream>> javaZip3 = new HashMap<String, ArrayList<InputStream>>();
//		javaZip1 = handler.unzipFile(javaFile, ".java");
//		javaZip2 = handler.unzipFile(javaFile, ".java");
//		javaZip3 = handler.unzipFile(javaFile, ".java");
//		
//		// Process javaZip map -- 1 entry == 1 folder == 1 program
//		for (Map.Entry<String, ArrayList<InputStream>> entry : javaZip1.entrySet()) 
//		{
//            String key = entry.getKey();
//            System.out.println("Zip key: "+key);
//            ArrayList<InputStream> streams1 = entry.getValue();
//            ArrayList<InputStream> streams2 = javaZip2.get(key);
//            ArrayList<InputStream> streams3 = javaZip3.get(key);
//            int[] vector = handler.readJava(streams1, streams2, streams3);
//            System.out.println("Java vector: "+java.util.Arrays.toString(vector));
//            codeVectors.put(key, vector);
//        }
//		File[] directoryListing = javaFile.listFiles(File::isFile);
//		ArrayList<InputStream> streams1 = new ArrayList<InputStream>();
//		ArrayList<InputStream> streams2 = new ArrayList<InputStream>();
//		ArrayList<InputStream> streams3 = new ArrayList<InputStream>();
//		for (File child : directoryListing)
//		{
//			streams1.add(new FileInputStream(child));
//			streams2.add(new FileInputStream(child));
//			streams3.add(new FileInputStream(child));
//		}
//
//        int[] vector = handler.readJava(streams1, streams2, streams3);
//        System.out.print("Main: "+java.util.Arrays.toString(vector));
	}
}
