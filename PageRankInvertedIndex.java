package ir.vsr;

import java.io.*;
import java.util.*;
import java.lang.*;

import ir.utilities.*;
import ir.classifiers.*;

public class PageRankInvertedIndex extends InvertedIndex{

    double pageWeight = 0.0;

    public PageRankInvertedIndex(File dirFile, short docType, boolean stem, boolean feedback, double weight) {
        super(dirFile, docType, stem, feedback);
        pageWeight = weight;
        tokenHash = new HashMap<String, TokenInfo>();
        docRefs = new ArrayList<DocumentReference>();
        indexDocuments();
      }

      public PageRankInvertedIndex(List<Example> examples) {
        super(examples);
        tokenHash = new HashMap<String, TokenInfo>();
        docRefs = new ArrayList<DocumentReference>();
        indexDocuments(examples);
      }

      public Retrieval[] retrieve(HashMapVector vector) {
        // Create a hashtable to store the retrieved documents.  Keys
        // are docRefs and values are DoubleValues which indicate the
        // partial score accumulated for this document so far.
        // As each token in the query is processed, each document
        // it indexes is added to this hashtable and its retrieval
        // score (similarity to the query) is appropriately updated.
        Map<DocumentReference, DoubleValue> retrievalHash =
            new HashMap<DocumentReference, DoubleValue>();
        // Initialize a variable to store the length of the query vector
        double queryLength = 0.0;
        // Iterate through each token in the query input Document
        for (Map.Entry<String, Weight> entry : vector.entrySet()) {
          String token = entry.getKey();
          double count = entry.getValue().getValue();
          // Determine the score added to the similarity of each document
          // indexed under this token and update the length of the
          // query vector with the square of the weight for this token.
          queryLength = queryLength + incorporateToken(token, count, retrievalHash);
        }

        // Finalize the length of the query vector by taking the square-root of the
        // final sum of squares of its token weights.
        queryLength = Math.sqrt(queryLength);
        // Make an array to store the final ranked Retrievals.
        Retrieval[] retrievals = new Retrieval[retrievalHash.size()];
        // Iterate through each of the retrieved documents stored in
        // the final retrievalHash.
        int retrievalCount = 0;
        Map<String, Double> pageRanks = new HashMap<String, Double>();
        try {

            File rankFile = new File(dirFile.toString() + "/page_ranks.txt");

            FileReader fr = new FileReader(rankFile.getAbsoluteFile());
            BufferedReader br = new BufferedReader(fr);
            while(true){
                String line = br.readLine();
                if (line == null){
                    break;
                }
                String[] split = line.split(" ");
                pageRanks.put(split[0], Double.parseDouble(split[1]));

            }
            br.close(); 

        } catch (IOException e) {
            e.printStackTrace();
        }
        for (Map.Entry<DocumentReference, DoubleValue> entry : retrievalHash.entrySet()) {
          DocumentReference docRef = entry.getKey();
          double score = entry.getValue().value;
          double pageRank = pageRanks.get(docRef.toString());
          double modifier = pageWeight * pageRank;
          retrievals[retrievalCount++] = getRetrieval(queryLength, docRef, score, modifier);
        }
        // Sort the retrievals to produce a final ranked list using the
        // Comparator for retrievals that produces a best to worst ordering.
        Arrays.sort(retrievals);
        return retrievals;
          }

      protected Retrieval getRetrieval(double queryLength, DocumentReference docRef, double score, double modifier) {
        // Normalize score for the lengths of the two document vectors
        score = score / (queryLength * docRef.length) + modifier;
        // Add a Retrieval for this document to the result array
        return new Retrieval(docRef, score);
      }
    
    public static void main(String[] args) {
        // Parse the arguments into a directory name and optional flag

        String dirName = args[args.length - 1];
        short docType = DocumentIterator.TYPE_TEXT;
        boolean stem = false, feedback = false;
        double weight = 0.0;        
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
              else if (flag.equals("-weight")){
                  weight = Double.parseDouble(args[++i]);
                }
              else {
                throw new IllegalArgumentException("Unknown flag: "+ flag);
             }
        }
    

        // Create an inverted index for the files in the given directory.
        InvertedIndex index = new PageRankInvertedIndex(new File(dirName), docType, stem, feedback, weight);
        // index.print();
        // Interactively process queries to this index.
        
        index.processQueries();
    }
}
