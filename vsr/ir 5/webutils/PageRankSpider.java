package ir.webutils;

import java.util.*;
import java.io.*;

import ir.utilities.*;

/**
 * Spider defines a framework for writing a web crawler.  Users can
 * change the behavior of the spider by overriding methods.
 * Default spider does a breadth first crawl starting from a
 * given URL up to a specified maximum number of pages, saving (caching)
 * the pages in a given directory.  Also adds a "BASE" HTML command to
 * cached pages so links can be followed from the cached version.
 *
 * @author Ted Wild and Ray Mooney
 */

public class PageRankSpider extends Spider{

  

  /**
   * The maximum number of pages to be indexed.
   */
  protected int maxCount = 200;

  protected double alpha = 0.15;

  protected int iterations = 50;

  List<Node> dest= new ArrayList<Node>();
  List<Node> source= new ArrayList<Node>();
  
  List<String> visitedNode= new ArrayList<String>();
  
  Map<String, String> newName = new HashMap<String, String>(); 

  
  public void doCrawl() {
    
    Graph graph = new Graph();

    if (linksToVisit.size() == 0) {
      System.err.println("Exiting: No pages to visit.");
      System.exit(0);
    }
    visited = new HashSet<Link>();
    while (linksToVisit.size() > 0 && count < maxCount) {
      // Pause if in slow mode
      if (slow) {
        synchronized (this) {
          try {
            wait(1000);
          }
          catch (InterruptedException e) {
          }
        }
      }
      // Take the top link off the queue
      Link link = linksToVisit.remove(0);
      System.out.println("Trying: " + link);
      // Skip if already visited this page
      if (!visited.add(link)) {
        System.out.println("Already visited");
        continue;
      }
      if (!linkToHTMLPage(link)) {
        System.out.println("Not HTML Page");
        continue;
      }

      HTMLPage currentPage = null;
      Node currentNode = null;
      
      // Use the page retriever to get the page
      try {
        currentPage = retriever.getHTMLPage(link);
      }
      catch (PathDisallowedException e) {
        System.out.println(e);
        continue;
      }
      if (currentPage.empty()) {
        System.out.println("No Page Found");
        continue;
      }
      if (currentPage.indexAllowed()) {
        count++;
        System.out.println("Indexing" + "(" + count + "): " + link);
        indexPage(currentPage, link.toString());
        currentNode = graph.getNode(link.toString());
        visitedNode.add(currentNode.toString());
      }
    
      if (count <= maxCount) {
        List<Link> newLinks = getNewLinks(currentPage);
        // System.out.println("Adding the following links" + newLinks);
        // Add new links to end of queue
        linksToVisit.addAll(newLinks);

        for(Link subLink : newLinks){

          if(!subLink.toString().equals(link.toString())){
            subLink.cleanURL();
            //System.out.println("THIS IS THE SUBLINK NAME"+ subLink.toString());
            Node newNode= new Node(subLink.toString());
            dest.add(newNode);
            source.add(currentNode);
          }
        }
      }
    }
      
    int it= 0; 
     while(it < source.size() && visitedNode.contains(source.get(it).toString())){
        if(visitedNode.contains(dest.get(it).toString())){
          graph.addEdge(source.get(it).toString(), dest.get(it).toString());          
        }
        it++;
      }
  System.out.println("Graph Structure: ");    
  graph.print();
  computePageRank(graph);
  }

  protected void computePageRank(Graph graph){
    
    Node[] Nodes = graph.nodeArray();
    graph.resetIterator();
    
    Double rankSource =  alpha / Nodes.length;
    Map<String, Double> pageRank = new HashMap<String, Double>();

    for(int i= 0; i< Nodes.length; i++ ){
      pageRank.put(Nodes[i].toString(), 1.0/Nodes.length);
    }
    

    Map<String, Double> inversePageRank= new HashMap<String, Double>();
    for (int i= 0; i< iterations; i++){
      Double total= 0.0;
      for(int j= 0; j< Nodes.length; j++){
        
        List<Node> incomingEdges= Nodes[j].getEdgesIn();
        
          Double value= 0.0;
          for(int k= 0; k< incomingEdges.size(); k++){
            
            Node prev = incomingEdges.get(k);
            int outgoingEdges= prev.getEdgesOut().size();

            value +=(pageRank.get(prev.toString())/outgoingEdges);
          }
        
        inversePageRank.put(Nodes[j].toString(), (((1-alpha)*value)+rankSource));         
        total +=(((1-alpha)*value)+rankSource);  
      }
      
      Double c= 1/total;
      for(int x= 0; x< Nodes.length; x++){
        pageRank.put(Nodes[x].toString(), c*inversePageRank.get(Nodes[x].toString()));
      }
    }
    System.out.println("PageRank: ");    
    for (Map.Entry<String, Double> entry : pageRank.entrySet()) {
        System.out.println("PR(" + entry.getKey() + "): " + MoreMath.roundTo(entry.getValue(), 5));
      }
      writeToFile(pageRank);
  }
  
  protected void writeToFile(Map<String, Double> pageRank ){
    try {

      File file = new File(saveDir,"page_ranks.txt");

      // if file doesnt exists, then create it
      if (!file.exists()) {
        file.createNewFile();
      }

      FileWriter fw = new FileWriter(file.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fw);
      int print= 1;
      for (Map.Entry<String, Double> entry : pageRank.entrySet()) {
        //String content = "P" + MoreString.padWithZeros(print, (int) Math.floor(MoreMath.log(maxCount, 10)) + 1) +".html " + entry.getValue() + "\n";
        String content = newName.get(entry.getKey()) +".html " + entry.getValue() + "\n";
        bw.write(content);
        print++;
      }
      bw.close();


    } catch (IOException e) {
      e.printStackTrace();
    }
  }
 
 protected void indexPage(HTMLPage page, String linkName) {
    String convertedName = "P" + MoreString.padWithZeros(count, (int) Math.floor(MoreMath.log(maxCount, 10)) + 1);
    newName.put(linkName, convertedName);  
    page.write(saveDir,convertedName);
  }


  /**
   * Spider the web according to the following command options:
   * <ul>
   * <li>-safe : Check for and obey robots.txt and robots META tag
   * directives.</li>
   * <li>-d &lt;directory&gt; : Store indexed files in &lt;directory&gt;.</li>
   * <li>-c &lt;maxCount&gt; : Store at most &lt;maxCount&gt; files (default is 10,000).</li>
   * <li>-u &lt;url&gt; : Start at &lt;url&gt;.</li>
   * <li>-slow : Pause briefly before getting a page.  This can be
   * useful when debugging.
   * </ul>
   */
  public static void main(String args[]) {
    new PageRankSpider().go(args);
  }
}
