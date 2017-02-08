package ir.eval;

import java.io.*;
import java.util.*;
import java.lang.*;

import ir.utilities.*;
import ir.vsr.*;

/**
 * Version of Experiment for queries that have continuously rated 
 * gold-standard document relevance judgements and includes evaluation 
 * with NDCG. Computes and reports NDCG values at all ranks up to a
 * specified limit (NDCGlimit)
 * 
 * Assumes the format of the results for a query in the queries file is a 
 * list of pairs of document names followed by a relevance score between 0 and 1
 * 
 * @author Ray Mooney
 */


public class ExperimentRelFeedbackRated extends ExperimentRated{

/**
   * Whether tokens should be stemmed with Porter stemmer
   */
  public boolean stem = false;
  
  private boolean control = false;
  
  private boolean binary = false;
  
  private int numDoc = 0;
 

 
 public ExperimentRelFeedbackRated(File corpusDir, File queryFile, File outFile, short docType, boolean stem, int numDoc, boolean control, boolean binary)
      throws IOException {
      super(corpusDir, queryFile, outFile, docType, stem);
      this.control = control;
      this.binary = binary;
      this.numDoc = numDoc;
      this.index = new InvertedIndexRated(corpusDir, docType, stem, true);
  }

  boolean processQuery(BufferedReader in) throws IOException {
    String query = in.readLine();   // get the query
    if (query == null) return false;  // return false if end of file
    System.out.println("\nQuery " + (rpResults.size() + 1) + ": " + query);

    // Process the query and get the ranked retrievals
    HashMapVector queryVector = (new TextStringDocument(query, stem)).hashMapVector();
    Retrieval[] retrievals = index.retrieve(queryVector);

    System.out.println("Returned " + retrievals.length + " documents.");

    // Get the correct retrievals
    ArrayList<String> correctRetrievals = new ArrayList<String>();
    getCorrectRatedRetrievals(in, correctRetrievals);
    ArrayList<DocumentReference> toRemove = new ArrayList<DocumentReference>();
    FeedbackRated fdback = new FeedbackRated(queryVector, retrievals, index);

    if(!control){
    System.out.println("Feedback: ");
    
    for(int i= 0; i< numDoc; i++){
      String docName= retrievals[i].docRef.toString();
      toRemove.add(retrievals[i].docRef);
      if (correctRetrievals.contains(docName)){
        if(!binary)
        fdback.addGood(retrievals[i].docRef, ratingsMap.get(docName));
        else
          fdback.addGood(retrievals[i].docRef, 1.0);
      }
      else
        fdback.addBad(retrievals[i].docRef, -1.0);
    }
     System.out.println("Positive docs: " + fdback.goodDocRefs.keySet() +
              "\nNegative docs: " + fdback.badDocRefs.keySet());
      System.out.println("Executing New Expanded and Reweighted Query: ");
      queryVector = fdback.newQuery();
      retrievals = index.retrieve(queryVector);  
      }    

      
      Retrieval[] updatedRetrievals = new Retrieval[retrievals.length - numDoc];

      int increment = 0;
     if(!control){ 
     for (int i= 0; i< retrievals.length; i++){
        if (fdback.goodDocRefs.containsKey(retrievals[i].docRef) || fdback.badDocRefs.containsKey(retrievals[i].docRef)){
          increment++;
            if (correctRetrievals.contains(retrievals[i].docRef.toString()))
              correctRetrievals.remove(retrievals[i].docRef.toString());
          }
        else
              updatedRetrievals[i- increment]= retrievals[i];
      }
       } 
       else{
            for(int i = numDoc; i< retrievals.length; i++)
              updatedRetrievals[i-numDoc]= retrievals[i]; 
        }
        retrievals= updatedRetrievals;

    // Generate Recall/Precision points and save in rpResults
    rpResults.add(evalRetrievals(retrievals, correctRetrievals));

    // Update the NDCG values for this query
    UpdateNDCG(retrievals, correctRetrievals);    

    // Read the blank line delimiter between queries in the query file
    String line = in.readLine();
    if (!(line == null || line.trim().equals(""))) {
      System.out.println("\nCould not find blank line after query, bad queryFile format");
      System.exit(1);
    }
    return true;
  }

  /**
     *  Read the known relevant docs with gold-standard relevance scores from query file and parse them
     *  into an ArrayList of String's of relevant file names which ratingsMap maps to these relevance scores
     *  Assume the format is a list of pairs of document names followed by a relevance score between 0 and 1
     */
    void getCorrectRatedRetrievals(BufferedReader in, ArrayList<String> correctRetrievals) throws IOException {
  String line = in.readLine();
  ArrayList<String> ratedRetrievals = MoreString.segment(line, ' ');
  // Process input 2 items at a time (filename followed by score)
  for (int i = 0; i < ratedRetrievals.size(); i=i+2) {    
      correctRetrievals.add(ratedRetrievals.get(i));
      ratingsMap.put(ratedRetrievals.get(i), Double.valueOf(ratedRetrievals.get(i+1)));
  }
  System.out.println(correctRetrievals.size() + " truly relevant documents.");
    }





 public static void main(String[] args) throws IOException {
    // Parse the arguments into a directory name and optional flag
    String corpusDir = args[args.length - 4];
    String queryFile = args[args.length - 3];
    String outFile = args[args.length - 2];
    String numDocStr= args[args.length - 1];
    int numDoc = Integer.parseInt(numDocStr);

    short docType = DocumentIterator.TYPE_TEXT;
    boolean stem = false;
    boolean control = false;
    boolean binary= false;

    for (int i = 0; i < args.length - 4; i++) {
      String flag = args[i];
      if (flag.equals("-html"))
        // Create HTMLFileDocuments to filter HTML tags
        docType = DocumentIterator.TYPE_HTML;
      else if (flag.equals("-stem"))
        // Stem tokens with Porter stemmer
        stem = true;
      else if(flag.equals("-control"))
            control= true;
      else if(flag.equals("-binary"))
            binary= true;      
      else {
        throw new IllegalArgumentException("Unknown flag: " + flag);
      }
    }
    ExperimentRated exper = new ExperimentRelFeedbackRated(new File(corpusDir), new File(queryFile), new File(outFile), docType, stem, numDoc, control, binary);
    // Generate a recall precision curve and NDCG results for this dataset
    // makeRpCurve must be first since it calculates the statistics for both
    exper.makeRpCurve();
    exper.makeNDCGtable();
  }
}