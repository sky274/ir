package ir.vsr;

import java.io.*;
import java.util.*;
import java.lang.*;

import ir.utilities.*;

/**
 * Gets and stores information about relevance feedback from the user and computes
 * an updated query based on original query and retrieved documents that are
 * rated relevant and irrelevant.
 *
 * @author Ray Mooney
 */


public class FeedbackRated extends Feedback {


  //list of good and bad document references with the feedback rating as its values.
  public Map<DocumentReference, Double> goodDocRefs=  new HashMap<DocumentReference, Double>();;
  public Map<DocumentReference, Double> badDocRefs=  new HashMap<DocumentReference, Double>();;  

	public FeedbackRated(HashMapVector queryVector, Retrieval[] retrievals, InvertedIndex invertedIndex){
		super(queryVector, retrievals, invertedIndex);

	}

	/**
   * Prompt the user for feedback on this numbered retrieval
   */
  public void getFeedback(int showNumber) {
    // Get the docRef for this document (remember showNumber starts at 1 and is 1 greater than array index)
    DocumentReference docRef = retrievals[showNumber - 1].docRef;
    if (!goodDocRefs.containsKey(docRef) && !badDocRefs.containsKey(docRef)){
      String stringresponse = UserInput.prompt("Is document #" + showNumber + ":" + docRef.file.getName() +
        " relevant (enter a number between -1 and 1 where -1: very irrelevant, 0: unsure, +1: very relevant)?: ");
      Double response = Double.parseDouble(stringresponse);
        if (response > 0.0 && response <= 1.0)
          addGood(docRef, response);
    
        else if (response < 0.0 && response >= -1.0)
          addBad(docRef, response);
    
        else if (response == 0.0);
    
        else
          getFeedback(showNumber);
    }
  }


  public void addGood(DocumentReference docRef, double value) {
    goodDocRefs.put(docRef, value);
  }

  /**
   * Add a document to the list of those deemed irrelevant
   */
  public void addBad(DocumentReference docRef, double value) {
    badDocRefs.put(docRef, value);
  }

   public boolean isEmpty() {
    if (goodDocRefs.isEmpty() && badDocRefs.isEmpty())
      return true;
    else
      return false;
  }


/**
   * Use the Ide_regular algorithm to compute a new revised query.
   *
   * @return The revised query vector.
   */
  public HashMapVector newQuery() {
    // Start the query as a copy of the original
    HashMapVector newQuery = queryVector.copy();
    // Normalize query by maximum token frequency and multiply by alpha
    newQuery.multiply(ALPHA / newQuery.maxWeight());
    
    // Add in the vector for each of the positively rated documents
    for (DocumentReference docRef : goodDocRefs.keySet()) {
      // Get the document vector for this positive document
      Document doc = docRef.getDocument(invertedIndex.docType, invertedIndex.stem);
      HashMapVector vector = doc.hashMapVector();
      // Multiply positive docs by beta and normalize by max token frequency
      vector.multiply(BETA / vector.maxWeight());
      // multiply the query vector by the feedback rating
      vector.multiply(goodDocRefs.get(docRef));
      // Add it to the new query vector
      newQuery.add(vector);
    }
    
    // Subtract the vector for each of the negatively rated documents
    for (DocumentReference docRef : badDocRefs.keySet()) {
      // Get the document vector for this negative document
      Document doc = docRef.getDocument(invertedIndex.docType, invertedIndex.stem);
      HashMapVector vector = doc.hashMapVector();
      // Multiply negative docs by beta and normalize by max token frequency
      vector.multiply(GAMMA / vector.maxWeight());
      // multiply the query vector by the feedback rating
      vector.multiply(-badDocRefs.get(docRef));
      // Subtract it from the new query vector
      newQuery.subtract(vector);
    }
    return newQuery;
  }


}