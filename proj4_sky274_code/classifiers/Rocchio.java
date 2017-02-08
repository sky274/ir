
package ir.classifiers;

import java.io.*;
import java.util.*;

import ir.vsr.*;
import ir.utilities.*;


public class Rocchio extends Classifier {


  /**
   * Number of categories
   */
  int numCategories;

  // the flag for running neg
  boolean neg;

  // Map that keeps tract of vprototypes
  Map<Integer, HashMapVector> vectorPrototypes;

  //global hashMapVector to keep tract of the copied
  HashMapVector documentHMV;

   public static final String name = "Rocchio";

/**
   * Create a KNN classifier with these attributes
   *
   * @param categories The array of Strings containing the category names
   * @param debug      Flag to turn on detailed output
   */
  //The KNN constructor takes in neighbors which represent K number of neighbors.
  public Rocchio(String[] categories, boolean neg) {
    this.categories = categories;
    this.neg = neg;
    numCategories = categories.length;
  }

  /**
   * Returns the name
   */
  public String getName() {
    return name + (neg ? "Neg" : "");
  }

/**
   * Trains the classifier on the training examples
   *
   * @param trainingExamples a list of Example objects that will be used
   *                         for training the classifier
   */

  public void train(List<Example> trainingExamples){
    
    vectorPrototypes = new HashMap<Integer, HashMapVector>();
    
    for (int i=0; i< numCategories; i++){
      vectorPrototypes.put(i,new HashMapVector());
    }
    
    //tokenOcc has the token and number of documents the token occurs in
    // calculate the document frequency of each token and sotre that in tokenOcc
    Map<String, Integer> tokenOcc= new HashMap<String, Integer>();
    for (int i= 0; i< trainingExamples.size(); i++){
      Example e = trainingExamples.get(i);
      HashMapVector documentHMV= e.getHashMapVector();
      
      Map<String, Weight> tokenToWeightHashMap = documentHMV.hashMap;
      
      for(Map.Entry<String,Weight> entry: tokenToWeightHashMap.entrySet()){
        String token = entry.getKey();
        // we found a token thats in the document but hasn't been accounted for
        if(!tokenOcc.containsKey(token))
          tokenOcc.put(token,1);
        // found a instance of token in a document and it has already been accounted for so just increment count
        else{
          int counter= tokenOcc.get(token);
          counter++;
          tokenOcc.put(token,counter);
        }
      }
    }
      
    for (int i= 0; i< trainingExamples.size(); i++){
        Example e = trainingExamples.get(i);
        //get a copy of the hashmap vector so the original doesn't get changed
        documentHMV = e.getHashMapVector().copy();

      Map<String, Weight> tokenToWeightHashMap = documentHMV.hashMap;
      
      for(String token: tokenToWeightHashMap.keySet()){
        //the termfrequency
        double tf= documentHMV.getWeight(token);
        //the documentfrequency
        int df= tokenOcc.get(token);

        double idf= Math.log(trainingExamples.size()/ df);

        Weight weight= new Weight();
        weight.setValue(tf* idf);
        tokenToWeightHashMap.put(token, weight);
        
      }
      double d = documentHMV.maxWeight();
      // normalize document vector by the max weight before adding
      int category= e.getCategory();
      double scale = 1.0/d;
      // add the scaled tf-idf to prototype
      vectorPrototypes.get(category).addScaled(documentHMV, scale);
      if(neg){
        //subtract all of the documents in all other categories
        for (int x=0; x< numCategories; x++){
          if(x != category){
            scale= 1.0/ numCategories-1;
            vectorPrototypes.get(x).addScaled(documentHMV,  scale *-1.0);
          }
        }
      } 
    }
  }   
 /**
   * Returns true if the predicted category of the test example matches the correct category,
   * false otherwise
   */
 //followed the pseudocode on the TextCategorization slide from class
  public boolean test(Example testExample){
   HashMapVector d= testExample.getHashMapVector().copy();
   int r = 0;
   double m = -2.0;
   for (int i= 0; i< numCategories; i++){
      double s= d.cosineTo(vectorPrototypes.get(i));
      if(s>m){
        m=s;
        r=i; 
      }
   }  
   return r == testExample.getCategory();
  }
}