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

  boolean control = false;
  
  boolean binary = false;
  
  double numDoc = 0;

 
 public ExperimentRelFeedbackRated(File corpusDir, File queryFile, File outFile, short docType, boolean stem, boolean control, boolean binary, double numDoc)
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
    Retrieval[] retrievals = index.retrieve(query);

    System.out.println("Returned " + retrievals.length + " documents.");

    // Get the correct retrievals
    ArrayList<String> correctRetrievals = new ArrayList<String>();
    getCorrectRatedRetrievals(in, correctRetrievals);
    
    FeedbackRated fdback = null     

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
    double numDoc = Double.parseDouble(numDocStr);

    short docType = DocumentIterator.TYPE_TEXT;
    boolean stem = false;
    boolean control = false;
    boolean binary= false;

    for (int i = 0; i < args.length - 3; i++) {
      String flag = args[i];
      if (flag.equals("-html"))
        // Create HTMLFileDocuments to filter HTML tags
        docType = DocumentIterator.TYPE_HTML;
      else if (flag.equals("-stem"))
        // Stem tokens with Porter stemmer
        stem = true;
      
      else if (flag.equals("-control"))
        // Stem tokens with Porter stemmer
        control = true;

      else if (flag.equals("-binary"))
        // Stem tokens with Porter stemmer
        binary = true;
      

      else {
        throw new IllegalArgumentException("Unknown flag: " + flag);
      }
    }
    ExperimentRated exper = new ExperimentRelFeedbackRated(new File(corpusDir), new File(queryFile), new File(outFile), docType, stem, control, binary, numDoc);
    // Generate a recall precision curve and NDCG results for this dataset
    // makeRpCurve must be first since it calculates the statistics for both
    exper.makeRpCurve();
    exper.makeNDCGtable();
  }
}
