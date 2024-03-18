package org.vaadin.example;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.List;
import java.util.Map;

/*
* 	Input handling for class diagram:
* 	Read (Identify), Map relations, Compute overall relation matrix, Compute pattern vector;
* 	Input: class diagram InputStream, Output: pattern vector.
* */
public class InputHandling 
{
    public static ArrayList<Integer[]> classCoordinates;
    public static ArrayList<Integer[]> relationCoordinates;
    public static Map<String, String> relationLabel = new HashMap<String, String>(); 		// Arrow label dictionary 
    public static Map<String, Integer[]>  namedClassCoordinates;
    public static Map<String, List<Integer[]>>  labelledRelationCoordinates;
    public static Map<Integer[], List<double[]>>  relationAdditionalAttribute;
    public static Map<String, List<Integer[]>>  mappedRelation;
    public static Map<String, List<String[]>>  namedMappedRelation;
    public static Map<String, int[][]> relationMatrixes;
    public static Map<String, Integer> matrixRootValue = new HashMap<String, Integer>(); 	// Relation type's root value dictionary
    public static int[][] overallMatrix;
    public static int[] designPatternVector;
	public String errorMessage = "";

    public InputHandling() {
        classCoordinates = new ArrayList<Integer[]>();
        relationCoordinates = new ArrayList<Integer[]>();
        namedClassCoordinates = new HashMap<String, Integer[]>();
        labelledRelationCoordinates = new HashMap<String, List<Integer[]>>();
        relationAdditionalAttribute = new HashMap<Integer[], List<double[]>>();
        mappedRelation = new HashMap<String, List<Integer[]>>();
        namedMappedRelation = new HashMap<String, List<String[]>>();
        relationMatrixes = new HashMap<String, int[][]>();
        
		/* Arrow Label Dictionary */
		relationLabel.put("&lt;-", "Association One-to-One multiplicity");
		relationLabel.put("&lt;&lt;-", "Generalization One-to-Many multiplicity");
		relationLabel.put("&lt;&lt;.", "Generalization One-to-Many multiplicity");
		relationLabel.put("&lt;&lt;&lt;&lt;-", "Association One-to-One multiplicity");
		relationLabel.put("&lt;&lt;&lt;&lt;&lt;-", "Association One-to-One multiplicity");
		
		/* Relation Matrix Root Value Dictionary */
		matrixRootValue.put("Association One-to-One multiplicity", 2);
		matrixRootValue.put("Association One-to-Many multiplicity", 3);
		matrixRootValue.put("Generalization One-to-Many multiplicity", 5);
    }
    
    public void parseFile(InputStream streams) throws Exception 
    {
        Scanner scan = new Scanner(streams);
        boolean isClass = false, isRelation = false;
        List<Integer> currentCoordinates = new ArrayList<>();
        int classCount = 0, relationCount = -1;

        while (scan.hasNext()) {
            String line = scan.nextLine().trim();

            /* id handling */
            // When reach new id type, previous id type set to false
            if (line.contains("<id>UMLClass</id>")) {
                isClass = true;
                isRelation = false;
            } else if (line.contains("<id>Relation</id>")) {
                isRelation = true;
                isClass = false;
                relationCount++;
            } else if (line.contains("<id>")) { // All other elements
                isRelation = false;
                isClass = false;
                continue;
            }
            /* coordinates handling */
            if (line.startsWith("<coordinates>")) {
            	// Start new coordinates, remove previous coordinates from currentCoordinates
                currentCoordinates.clear();
            } else if (line.startsWith("</coordinates>")) { 
            	// End of coordinates (x,y,w,h): 
                if (isClass) { // if is class id, add to classCoordinates
                    classCoordinates.add(currentCoordinates.toArray(new Integer[0]));
                } else if (isRelation) { // if is relation id, add to relationCoordinates
                    relationCoordinates.add(currentCoordinates.toArray(new Integer[0]));
                }
            } else if (line.startsWith("<x>") || line.startsWith("<y>") || line.startsWith("<w>") || line.startsWith("<h>")) {
            	// In coordinates (x,y,w,h):
            	// Extract numeric values using regex
                Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?");
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    currentCoordinates.add(Integer.parseInt(matcher.group()));
                }
            }
            
            /* panel_attributes handling */
            if (line.startsWith("<panel_attributes>")) 
            {
            	if (isRelation) 
            	{
            		String arrow;
            		if (line.contains("</panel_attributes>"))
            			arrow = line.substring(line.indexOf('=')+1, line.lastIndexOf('<'));
            		else
            			arrow = line.substring(line.indexOf('=')+1); // Get the arrow type = relation type
            		
            		String label = relationLabel.get(arrow); // Get the arrow label        		

            		if (label != null)
            		{
                		// Find </panel_attributes>
                		while (scan.hasNext()&& !line.contains("</panel_attributes>")) 
                		{
                            line = scan.nextLine().trim();                     
                            if (line.startsWith("m1"))
                            {
                            	String m1;
                            	if (line.contains("</panel_attributes>"))
                        			m1 = line.substring(line.indexOf('=')+1, line.lastIndexOf('<'));
                        		else
                        			m1 = line.substring(line.indexOf('=')+1);
                            	
                        		if (isInteger(m1))
                        			label = label+";m1One-to-One multiplicity";
                        		else
                        			label = label+";m1One-to-Many multiplicity";
                            }
                            else if (line.startsWith("m2"))
                            {
                            	String m2;
                            	if (line.contains("</panel_attributes>"))
                        			m2 = line.substring(line.indexOf('=')+1, line.lastIndexOf('<'));
                        		else
                        			m2 = line.substring(line.indexOf('=')+1);
                            	
                        		if (isInteger(m2))
                        			label = label+";m2One-to-One multiplicity";
                        		else
                        			label = label+";m2One-to-Many multiplicity";
                            }
                		}
                		
                		// If the key is not present, create a new list
                		labelledRelationCoordinates.putIfAbsent(label, new ArrayList<>());
                        // Map relation coordinates with arrow label
                		labelledRelationCoordinates.get(label).add(relationCoordinates.get(relationCount)); 
            		}
					else
						errorMessage = errorMessage + "Relation arrow not in dictionary, please check your class diagram arrow type.\n";

            	}
            	else if (isClass) {
            		String className;
            		if (line.contains("</panel_attributes>"))
            			className = line.substring(line.indexOf('>')+1, line.lastIndexOf('<'));
            		else
            			className = line.substring(line.indexOf('>')+1); // Get the class name
            		namedClassCoordinates.put(className.replaceAll("[^a-zA-Z0-9_]", ""), classCoordinates.get(classCount));
            		classCount++;
            	}		
            }
            
            /* additional_attributes (aa) handling -- relationAdditionalAttribute */
            if (line.startsWith("<additional_attributes>")) {
            	if (isRelation) {
            		String values;
            		if (line.contains("</additional_attributes>"))
            			values = line.substring(line.indexOf('>')+1, line.lastIndexOf('<'));
            		else
            			values = line.substring(line.indexOf('>')+1); // Get values to determine start point and end point of relation
            		
            		// Splitting the values with ';' delimiter
            		String[] additionalAttributes = values.split(";");

            		// Combining every two elements into pairs separated by ','
            		List<double[]> aaList = new ArrayList<>();
//            		double startX, startY, endX, endY;
            		for (int i = 0; i < additionalAttributes.length - 1; i += 2) 
            		{
            			double x = 0, y = 0;
            			try
            			{
            			   x = Double.parseDouble(additionalAttributes[i]);
            			   y = Double.parseDouble(additionalAttributes[i+1]);
            			}
            			catch(NumberFormatException e)
            			{
            				e.printStackTrace();
            				errorMessage = errorMessage + "Additional attributes not a double.\n";
            			}
            			aaList.add(new double[] {x,y});
            		}
            		
            		// First element in aaList for start point; Last element in aaList for end point;
            		List<double[]> startEndAA = new ArrayList<>();
            		startEndAA.add(aaList.get(0));
            		startEndAA.add(aaList.get(aaList.size()-1));
            		
            		Integer[] currentCoordinate = relationCoordinates.get(relationCount);
            		relationAdditionalAttribute.put(currentCoordinate, startEndAA);
            	}	
            }         
        }
    }

	public String getErrorMessage() {
		return errorMessage;
	}

    public boolean isInteger(String string) 
    {
        try {
            Integer.valueOf(string);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /* 
     * Update relation coordinates in labelledRelationCoordinates to store relation's start and end point.
     * Use relationAdditionalAttribute to find relation's start and end point.
     * 
     * output: labelledRelationCoordinates = label : List<RelationStartEndPoints>; RelationCoord = [startX, startY, endX, endY]
     * 
     * */
    public Map<String, List<Integer[]>> updateRelationStartEndPoint(Map<String, List<Integer[]>> labelledRelationCoordinates, Map<Integer[], List<double[]>> relationAdditionalAttribute) throws Exception
    {
    	Map<String, List<Integer[]>> newLabelledRelationCoordinates = new HashMap<String, List<Integer[]>>();
    	
    	for (Map.Entry<String, List<Integer[]>> entry : labelledRelationCoordinates.entrySet()) 
    	{
    		String key = entry.getKey();
            List<Integer[]> values = entry.getValue();
            
            String[] relations = key.split(";");
            
            // Convert relationCoord to [startX, startY, endX, endY]
            for (Integer[] coordinate : values) 
            {
            	List<double[]> add_attr = relationAdditionalAttribute.get(coordinate);
            	
            	// add_attr.get(1) = start point, add_attr.get(0) = end point
        		int x = coordinate[0], y = coordinate[1];
            	Integer startX = (int) (x+add_attr.get(1)[0]), startY = (int) (y+add_attr.get(1)[1]);
            	Integer endX = (int) (x+add_attr.get(0)[0]), endY = (int) (y+add_attr.get(0)[1]);
            	
            	coordinate[0] = startX; 
            	coordinate[1] = startY; 
            	coordinate[2] = endX;
            	coordinate[3] = endY;
            	
        		Integer[] backwardCoordinate = Arrays.copyOf(coordinate, coordinate.length);
        		backwardCoordinate[0] = endX; 
        		backwardCoordinate[1] = endY; 
        		backwardCoordinate[2] = startX;
        		backwardCoordinate[3] = startY;
            	
        		String forwardLabel = relations[0], backwardLabel = relations[0];
            	if (relations.length > 1 && !key.contains("Generalization"))
            	{
            		for (int i = 1; i < relations.length; i++)
                	{
            			
                    	if (relations[i].contains("m2")) 
                    		backwardLabel = "Association " + relations[i].substring(relations[i].indexOf("2")+1);
                    	else
                    		forwardLabel = "Association " + relations[i].substring(relations[i].indexOf("1")+1);
                	}
            	}
            	
        		// If the key is not present, create a new list
        		newLabelledRelationCoordinates.putIfAbsent(forwardLabel, new ArrayList<>());
        		newLabelledRelationCoordinates.putIfAbsent(backwardLabel, new ArrayList<>());
        		newLabelledRelationCoordinates.get(forwardLabel).add(coordinate);
        		newLabelledRelationCoordinates.get(backwardLabel).add(backwardCoordinate);
            }
        }
    	return newLabelledRelationCoordinates;
    }
    
    /* 
     * Map relations start end points to nearest classes
     * 
     * */
    public void mapRelationToClasses(Map<String, List<Integer[]>> labelledRelationCoordinates, ArrayList<Integer[]> classCoordinates, Map<String, List<Integer[]>>  mappedRelation) throws Exception
    {
    	for (Integer[] coordinate : classCoordinates)
    	{
    		// Class coordinate
    		// let (x, y, width, height) become (min-x, min-y, max-x, max-y)
    		coordinate[2] = coordinate[0]+coordinate[2];
    		coordinate[3] = coordinate[1]+coordinate[3];
    	}
    	
    	for (Map.Entry<String, List<Integer[]>> entry : labelledRelationCoordinates.entrySet()) 
    	{
          String key = entry.getKey(); // relation label
          List<Integer[]> values = entry.getValue(); // relation coordinates
          
          /* Map relations to classes */
          // 1 relation type can have many relations
          for (Integer[] relation : values) 
          { 
			// Relation coordinate [startX, startY, endX, endY]
			// Start point = (0, 1); End point = (2, 3)
			int[] start = {relation[0], relation[1]};
			int[] end = {relation[2], relation[3]};
			
			// No restriction for self loop, start point and end point is treated separately
			// If more than 1 nearest class to the relation, throw exception.
        	try 
        	{            
    			int startClassIndex = nearestClass(classCoordinates, start[0], start[1]);
    			int endClassIndex = nearestClass(classCoordinates, end[0], end[1]);
    			if (startClassIndex != -1 && endClassIndex != -1) 
    			{	
    				// Check if this class pair, had an existing relation mapping where mappedRelation's key == key
    				// If 'Generalization', only check 1 key; 'Association' check 2 keys
    				boolean exists = false;
    				String searchKey = null;
    				if (key.startsWith("Generalization"))
    					searchKey = key;
    				else if (key.equals("Association One-to-One multiplicity"))
    					searchKey = "Association One-to-Many multiplicity";
    				else if (key.equals("Association One-to-Many multiplicity"))
    					searchKey = "Association One-to-One multiplicity";
    				
    				if (mappedRelation.get(searchKey) != null) // Search key exists, check existing match in its values
    				{
	    				for (Integer[] array : mappedRelation.get(searchKey)) 
	    				{
					        if (Arrays.equals(array, new Integer[]{startClassIndex, endClassIndex})) {
					            exists = true;
					            break;
					        }
	    				}
	    				
        				if (exists) 
        				    throw new Exception("Cannot have more than same type of relationship between 2 classes.");
        				else {
            				// If the key is not present, create a new list
            				mappedRelation.putIfAbsent(key, new ArrayList<>());
                            // Map class indexes to relation label
            				mappedRelation.get(key).add(new Integer[]{startClassIndex, endClassIndex});
    		            }
        				
    				} else if (mappedRelation.get(key) != null) // Key exists, check existing match in its values
    				{
	    				for (Integer[] array : mappedRelation.get(key)) 
	    				{
					        if (Arrays.equals(array, new Integer[]{startClassIndex, endClassIndex})) {
					            exists = true;
					            break;
					        }
	    				}
	    				
        				if (exists) 
        				    throw new Exception("Cannot have more than same type of relationship between 2 classes.");
        				else {
            				// If the key is not present, create a new list
            				mappedRelation.putIfAbsent(key, new ArrayList<>());
                            // Map class indexes to relation label
            				mappedRelation.get(key).add(new Integer[]{startClassIndex, endClassIndex});
    		            }
        				
    				} 
    				else if (mappedRelation.get(searchKey) == null && mappedRelation.get(key) == null)
    				{ // Key & searchKey not exists = class pair no existing match
        				// If the key is not present, create a new list
        				mappedRelation.putIfAbsent(key, new ArrayList<>());
                        // Map class indexes to relation label
        				mappedRelation.get(key).add(new Integer[]{startClassIndex, endClassIndex});
    				}

    			}    			
        	}catch (Exception e)
        	{
        		e.printStackTrace();
				errorMessage = errorMessage + "Cannot have more than same type of relationship between 2 classes.\n";
        	}
          }
    	}
    }
    
    /* 
     * Substitute class index with class name in mappedRelation
     * For display purpose.
     * 
     * */
    public void classNameRelationMapping (ArrayList<Integer[]> classCoordinates, Map<String, Integer[]>  namedClassCoordinates, Map<String, List<Integer[]>>  mappedRelation) 
    {
		for (Map.Entry<String, List<Integer[]>> entry : mappedRelation.entrySet()) 
		{
            String key = entry.getKey();
            List<Integer[]> values = entry.getValue();

            for (Integer[] array : values) 
            {
            	Integer[] startClassCoordinate = classCoordinates.get(array[0]);
            	Integer[] endClassCoordinate = classCoordinates.get(array[1]);
                String startClass = findClassName(startClassCoordinate, namedClassCoordinates); 
                String endClass = findClassName(endClassCoordinate, namedClassCoordinates);
                
				// If the key is not present, create a new list
                namedMappedRelation.putIfAbsent(key, new ArrayList<>());
                // Map class names to relation label
                if (startClass != null && endClass != null)
                namedMappedRelation.get(key).add(new String[]{startClass, endClass});
            }
        }
    }
    
    public String findClassName(Integer[] coordinate, Map<String, Integer[]> namedClassCoordinates) 
    {    	    
		for (Map.Entry<String, Integer[]> entry : namedClassCoordinates.entrySet()) 
		{
            String key = entry.getKey();
            Integer[] values = entry.getValue();

            if (Arrays.equals(values, coordinate))
            	return key;
        }
		// Not found
    	return null;
    }
    /* 
     * Calculate nearest class.
     * Used for relation mapping.
     * 
     * */
    public int nearestClass(ArrayList<Integer[]> classCoordinates, int x, int y) throws Exception 
    {
        double minDistance = Double.MAX_VALUE;
        double distance;
        int nearestClassIndex = -1;

        // Iterate through all classes
        for (int i = 0; i < classCoordinates.size(); i++)
    	{
			Integer[] coordinate = classCoordinates.get(i);
            // Calculate distance to the boundary of the current class
        	// (x,y) = relation coordinate; coordinate = [minX, minY, maxX, maxY]
        	int minX = coordinate[0], minY = coordinate[1], maxX = coordinate[2], maxY = coordinate[3];
        	if (x < minX) {
                if (y < minY)
                	distance = Math.sqrt((minX - x) * (minX - x) + (minY - y) * (minY - y));
                else if (y > maxY)
                	distance = Math.sqrt((minX - x) * (minX - x) + (y - maxY) * (y - maxY));
                else
                	distance = minX - x;
            } else if (x > maxX) {
                if (y < minY)
                	distance = Math.sqrt((x - maxX) * (x - maxX) + (minY - y) * (minY - y));
                else if (y > maxY)
                	distance = Math.sqrt((x - maxX) * (x - maxX) + (y - maxY) * (y - maxY));
                else
                	distance = x - maxX;
            } else {
                if (y < minY)
                	distance = minY - y;
                else if (y > maxY)
                	distance = y - maxY;
                else
                	distance = 0; // Inside the class range
            }
           
    		if (distance < minDistance) 
    		{ // If this class is closer than the previous closest class
                minDistance = distance; // Update the minimum distance
                nearestClassIndex = i; // Update the nearest class
            }
        }
    	return nearestClassIndex;
    }
	
    /* 
     * Construct 1 matrix for each relation.
     * Then, Construct overall relation matrix from all relation matrixes
     * 
     * */
    public void constructOverallRelationMatrix() 
    {
       	// Construct n*n matrix for each relation, n is the number of classes
    	int matrixSize = classCoordinates.size();
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
            	int row = value[0], column = value[1];
            	relationMatrix[row][column] = 1;
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

			if (matrixRootValue.containsKey(key))
			{
				int rootValue = matrixRootValue.get(key);
				for (int row = 0; row < matrixSize; row++) // row index
				{
					for (int column = 0; column < matrixSize; column++) // column index
					{
						overallMatrix[row][column] = overallMatrix[row][column] * (int) Math.pow(rootValue, values[row][column]);
					}
				}
			}
			else
				errorMessage = errorMessage + "Relation root value not found, please check your class diagram relation mapping.\n";

        }
    }
    
    /* Convert overall matrix to vector for detection */
    public int[] getDesignPatternVector()
    {
    	//Flatten 2D array to 1D array
    	designPatternVector = new int[overallMatrix.length*overallMatrix.length];

        int index = 0;
        for (int row = 0; row < overallMatrix.length; row ++) 
        {
			for (int column = 0; column < overallMatrix.length; column ++)
			{                           
				designPatternVector[index] = overallMatrix[row][column];
				index++;
			}
        }
    	return designPatternVector;
    }
    
    public void display(int[] vector)
    {
		System.out.println("Element in named class coordinates");
		for (Map.Entry<String, Integer[]> entry : namedClassCoordinates.entrySet()) {
            String key = entry.getKey();
            Integer[] values = entry.getValue();
            System.out.println("Key: " + key);
            System.out.println("  " + Arrays.toString(values));
        }
		System.out.println();
		
		System.out.println("Element in class coordinates");
		for (Integer[] UMLClass : classCoordinates) {
			for (int coordinate : UMLClass) {
	            System.out.print(coordinate + " ");
	        }
			System.out.println();
		}
		System.out.println();
		
		System.out.println("Element in relation coordinates");
		for (Integer[] UMLRelation : relationCoordinates) {
			for (int coordinate : UMLRelation) {
	            System.out.print(coordinate + " ");
	        }
			System.out.println();
        }
		
		for (Map.Entry<String, List<Integer[]>> entry : labelledRelationCoordinates.entrySet()) {
            String key = entry.getKey();
            List<Integer[]> values = entry.getValue();

            System.out.println("Key: " + key);
            for (Integer[] array : values) {
                System.out.println("  " + Arrays.toString(array));
            }
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
        
        System.out.println("Design Pattern Vector:");
        System.out.print(Arrays.toString(vector));
        System.out.println();
        System.out.println();
    }
    
    public int[] readCD(InputStream stream, InputHandling handler) throws Exception
    {
    	int[] vector = null;
    	
    	try {
    		handler.parseFile(stream);
    		labelledRelationCoordinates = handler.updateRelationStartEndPoint(labelledRelationCoordinates, relationAdditionalAttribute);
			handler.mapRelationToClasses(labelledRelationCoordinates, classCoordinates, mappedRelation);
			handler.classNameRelationMapping (classCoordinates, namedClassCoordinates, mappedRelation);
			handler.constructOverallRelationMatrix();
			vector = handler.getDesignPatternVector();
			handler.display(vector);
		} catch (Exception e) {
			e.printStackTrace();
		}
	    
		return vector;
    }
    
//	public static void main(String[] args) throws Exception 
//	{
//		/* System workflow */
//		InputHandling handler = new InputHandling();
//		
//		File CDFile = new File("src/data/Adapter/Adapter1.uxf");
//		InputStream CDInputStream = new FileInputStream(CDFile);
//    	int[] vector = handler.readCD(CDInputStream);
////		handler.readCDFile("src/data/XML and Design Patterns/Composite - Copy.txt");
////		handler.readCD("src/data/Adapter/Adapter1.uxf");
////		handler.readCD("src/data/XML and Design Patterns");
//	}
}
