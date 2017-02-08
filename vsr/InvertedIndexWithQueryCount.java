package ir.vsr;

import java.io.*;
import java.util.*;
import java.lang.*;

import ir.utilities.*;
import ir.classifiers.*;

public class InvertedIndexWithQueryCount extends InvertedIndex {

  private double weight = 0;
  
  
  
  private int querySize;

  public InvertedIndexWithQueryCount(File dirFile, short docType, boolean stem,
      boolean feedback) {
    super(dirFile, docType, stem, feedback);
    
    querySize = 0;
    
  }
  public InvertedIndexWithQueryCount(List<Example> examples) {
    super(examples);

     querySize = 0;
  }


  /**
   * Perform ranked retrieval on this input query Document vector.
   */
  public Retrieval[] retrieve(HashMapVector vector) {
    // Create a hashtable to store the retrieved documents.  Keys
    // are docRefs and values are DoubleValues which indicate the
    // partial score accumulated for this document so far.
    // As each token in the query is processed, each document
    // it indexes is added to this hashtable and its retrieval
    // score (similarity to the query) is appropriately updated.
    Map<DocumentReference, DoubleValue> retrievalHash =
        new HashMap<DocumentReference, DoubleValue>();

    Map<DocumentReference, Double>  percentage = new HashMap<DocumentReference, Double>();   
    // Initialize a variable to store the length of the query vector
    double queryLength = 0.0;
    // Iterate through each token in the query input Document
    for (Map.Entry<String, Weight> entry : vector.entrySet()) {
      String token = entry.getKey();
      double count = entry.getValue().getValue();
      // Determine the score added to the similarity of each document
      // indexed under this token and update the length of the
      // query vector with the square of the weight for this token.
      queryLength = queryLength + incorporateToken(token, count, retrievalHash, percentage);
    }
    // Finalize the length of the query vector by taking the square-root of the
    // final sum of squares of its token weights.
    queryLength = Math.sqrt(queryLength);
    // Make an array to store the final ranked Retrievals.
    Retrieval[] retrievals = new Retrieval[retrievalHash.size()];
    // Iterate through each of the retrieved documents stored in
    // the final retrievalHash.
    int retrievalCount = 0;
    for (Map.Entry<DocumentReference, DoubleValue> entry : retrievalHash.entrySet()) {
      DocumentReference docRef = entry.getKey();
      double score = entry.getValue().value;
      retrievals[retrievalCount++] = getRetrieval(queryLength, docRef, score, percentage);
    }
    // Sort the retrievals to produce a final ranked list using the
    // Comparator for retrievals that produces a best to worst ordering.
    Arrays.sort(retrievals);
    return retrievals;
  }


  public double incorporateToken(String token, double count, Map<DocumentReference, DoubleValue> retrievalHash, Map<DocumentReference, Double> percentage){
    TokenInfo tokenInfo = tokenHash.get(token);
    // If token is not in the index, it adds nothing and its squared weight is 0
    if (tokenInfo == null) return 0.0;
    // The weight of a token in the query is is IDF factor times the number
    // of times it occurs in the query.
    double weight = tokenInfo.idf * count;
    // For each document occurrence indexed for this token...
    for (TokenOccurrence occ : tokenInfo.occList) {
      // Get the current score for this document in the retrievalHash.
      DoubleValue val = retrievalHash.get(occ.docRef);
      if (val == null) {
        // If this is a new retrieved document, create an initial score
        // for it and store in the retrievalHash
        val = new DoubleValue(0.0);
        retrievalHash.put(occ.docRef, val);
      }
      if(percentage.containsKey(occ.docRef))
        percentage.put(occ.docRef, percentage.get(occ.docRef) + 1.0 );
      else
        percentage.put(occ.docRef, 1.0 );



       // Update the score for this document by adding the product
      // of the weight of this token in the query and its weight
      // in the retrieved document (IDF * occurrence count)
      val.value = val.value + weight * tokenInfo.idf * occ.count;
    }
    // Return the square of the weight of this token in the query
    return weight * weight;
  }

  protected Retrieval getRetrieval(double queryLength, DocumentReference docRef, double score, Map<DocumentReference, Double> percentage) {
    double point= percentage.get(docRef)/ querySize;
    // Normalize score for the lengths of the two document vectors
    score = (score / (queryLength * docRef.length)) + point;
    // Add a Retrieval for this document to the result array
    return new Retrieval(docRef, score);
  }


   /**
   * Enter an interactive user-query loop, accepting queries and showing the retrieved
   * documents in ranked order.
   */
  public void processQueries() {

    System.out.println("Now able to process queries. When done, enter an empty query to exit.");
    // Loop indefinitely answering queries
    do {
      // Get a query from the console
      String query = UserInput.prompt("\nEnter query:  ");
      // If query is empty then exit the interactive loop
      if (query.equals(""))
        break;
      // Get the ranked retrievals for this query string and present them
      HashMapVector queryVector = (new TextStringDocument(query, stem)).hashMapVector();
      querySize= queryVector.size();
      Retrieval[] retrievals = retrieve(queryVector);
      presentRetrievals(queryVector, retrievals);
    }
    while (true);
  }

  /**
   * Index a directory of files and then interactively accept retrieval queries.
   * Command format: "InvertedIndex [OPTION]* [DIR]" where DIR is the name of
   * the directory whose files should be indexed, and OPTIONs can be
   * "-html" to specify HTML files whose HTML tags should be removed.
   * "-stem" to specify tokens should be stemmed with Porter stemmer.
   * "-feedback" to allow relevance feedback from the user.
   */
  public static void main(String[] args) {
    // Parse the arguments into a directory name and optional flag

    String dirName = args[args.length - 1];
    short docType = DocumentIterator.TYPE_TEXT;
    boolean stem = false, feedback = false;
    for (int i = 0; i < args.length - 1; i++) {
      String flag = args[i];
      if (flag.equals("-html"))
        // Create HTMLFileDocuments to filter HTML tags
        docType = DocumentIterator.TYPE_HTML;
      else if (flag.equals("-stem"))
        // Stem tokens with Porter stemmer
        stem = true;
      else if (flag.equals("-feedback"))
        // Use relevance feedback
        feedback = true;
      else {
        throw new IllegalArgumentException("Unknown flag: "+ flag);
      }
    }

    // Create an inverted index for the files in the given directory.
    InvertedIndex index = new InvertedIndexWithQueryCount(new File(dirName), docType, stem, feedback);
    // index.print();
    // Interactively process queries to this index.
    index.processQueries();
  }

}
