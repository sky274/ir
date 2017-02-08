package ir.classifiers;

import java.io.*;
import java.util.*;

import ir.vsr.*;
import ir.utilities.*;


public class KNN extends Classifier {


  /**
   * Number of categories
   */
  int numCategories;

  InvertedIndex index;

  int k;

  HashMap<File, Integer> categoryHash;

  /**
   * Name of classifier
   */
  public static final String name = "KNN";

/**
   * Create a KNN classifier with these attributes
   *
   * @param categories The array of Strings containing the category names
   * @param debug      Flag to turn on detailed output
   */
  //The KNN constructor takes in neighbors which represent K number of neighbors.
  public KNN(String[] categories, int k ) {
    this.categories = categories;
    this.k = k;
    numCategories = categories.length;
  }

   /**
   * Returns the name
   */
  public String getName() {
    return name ;
  }


/**
   * Trains the classifier on the training examples
   *
   * @param trainingExamples a list of Example objects that will be used
   *                         for training the classifier
   */

  public void train(List<Example> trainingExamples){
    
    categoryHash = new HashMap<File, Integer>();  
    
    for(int i= 0; i< trainingExamples.size(); i++){
      // this hashmap maps file to its category
      categoryHash.put(trainingExamples.get(i).getDocument().file,trainingExamples.get(i).getCategory());
    }
    // call the inverted index
    index = new InvertedIndex(trainingExamples);
  }


 /**
   * Returns true if the predicted category of the test example matches the correct category,
   * false otherwise
   */

  public boolean test(Example testExample){
      
    HashMapVector queryVector = testExample.getHashMapVector();      
      
    Retrieval[] retrievals = index.retrieve(queryVector);
   
     Retrieval[] kRetrievals= new Retrieval[k];

    int predictedCategory;
     
    // if the test example has no nearest neighbor to compare the return random between 0 ,1,2    
    if(retrievals.length == 0){
      Random random= new Random();
      predictedCategory= random.nextInt(numCategories);
    }
    else{
      //speical case when there are more retrievals than k
      if(retrievals.length >k){ 
        for(int i= 0; i< k; i++){
          kRetrievals[i]= retrievals[i];
        }
      }
     else{
      kRetrievals = retrievals;
     }
      
      //mapping of catefory to its counter
      HashMap<Integer,Integer> countCategories= new HashMap<>();
      for (int i= 0; i< kRetrievals.length; i++){
        File file= kRetrievals[i].docRef.file;
        // find the category of the top retrievals
        int category= categoryHash.get(file);

        // if .get(category) returned null, the key does not have a value so give it a 0 
        if(countCategories.get(category)== null)
          countCategories.put(category, 0);
        else{
          //ensures that .get(category) returns atleast a 0 so just increment the value
          int counter= countCategories.get(category);
          counter++;
          countCategories.put(category, counter);
        }      
      }      

      predictedCategory= getBestCategory(countCategories);
    }
    return testExample.getCategory() == predictedCategory ;
  }

// helper method that return the best category bt performing random tie breakers
private int getBestCategory(Map<Integer, Integer> countCategories){
  ArrayList<Integer> bestCategory= new ArrayList<>();
      
      int best= -1;
      //get the entries from the map that contains categories to counters
      for(Map.Entry<Integer,Integer> entry: countCategories.entrySet()){
        int category = entry.getKey();
        int counter = entry.getValue();

        //this is the case for there is a new best category.
        if(counter > best){
          bestCategory.clear();
          bestCategory.add(category);
          best = counter;
        }
        //this is the case when there are more than one best category
        if(counter == best)
          bestCategory.add(category);         
      }
      // return random index but return index 0 if there is only one best category
      int index= 0;
      if(bestCategory.size() != 1){
        index = random.nextInt(bestCategory.size());
      }
      return bestCategory.get(index);
}
}