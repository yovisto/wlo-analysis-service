package com.yovisto.kea.rest.services;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

public class Categories {

	
	public static List<String> getDisciplines(List<String> entities) throws JsonProcessingException, MalformedURLException, IOException {
		 
		String needle = "";
		for (String e : entities){
			// w%3AErasmus_von_Rotterdam+w%3ABerlin+w%3ADeutschland
			needle=needle+ "<https://de.wikipedia.org/wiki/"+ e +">+";			
		}
		
//		String tmplt = "http://wlo-virtuoso:8890/sparql?default-graph-uri=&query=PREFIX+%3A<http%3A%2F%2Fexample-perma-id%2Feaf-schlagwortverzeichnis-all%2F>+%0D%0APREFIX+w%3A<https%3A%2F%2Fde.wikipedia.org%2Fwiki%2F>+%0D%0Aselect+count(*)+as+%3Fcnt+%3Fdis+where+{%0D%0Aselect+distinct+%3Fc+%3Fb+%3Ftop+%3Fdis+from+<http%3A%2F%2Fwlo.yovisto.com>+where+{%0D%0AVALUES+%3Fc+{+XXXXX+}%0D%0A%3Fk+<http%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23related>+%3Fc+.%0D%0A%3Fk+<http%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23broader>*+%3Fb+.%0D%0A%3Fb+<http%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23topConceptOf>+%3Ftop+.%0D%0A%3Fdis+<http%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23related>+%3Fb+.%0D%0A}}+group+by+%3Fdis+order+by+desc(%3Fcnt)&format=application%2Fsparql-results%2Bjson&timeout=0&debug=on&run=+Run+Query+";
		String tmplt = "http://yoyo2:8892/sparql?default-graph-uri=&query=PREFIX+%3A<http%3A%2F%2Fexample-perma-id%2Feaf-schlagwortverzeichnis-all%2F>+%0D%0APREFIX+w%3A<https%3A%2F%2Fde.wikipedia.org%2Fwiki%2F>+%0D%0Aselect+count(*)+as+%3Fcnt+%3Fdis+where+{%0D%0Aselect+distinct+%3Fc+%3Fb+%3Ftop+%3Fdis+from+<http%3A%2F%2Fwlo.yovisto.com>+where+{%0D%0AVALUES+%3Fc+{+XXXXX+}%0D%0A%3Fk+<http%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23related>+%3Fc+.%0D%0A%3Fk+<http%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23broader>*+%3Fb+.%0D%0A%3Fb+<http%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23topConceptOf>+%3Ftop+.%0D%0A%3Fdis+<http%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23related>+%3Fb+.%0D%0A}}+group+by+%3Fdis+order+by+desc(%3Fcnt)&format=application%2Fsparql-results%2Bjson&timeout=0&debug=on&run=+Run+Query+";

		String url = tmplt.replace("XXXXX", needle.replace("&","%26"));
		System.out.println(url);
		
		ObjectMapper mapper = new ObjectMapper();	    
	    JsonNode n = mapper.readTree(new URL(url));
	    
	    List<String> topSubjects = new ArrayList<String>();
	    
	    int currentNum = 0;
	    for (JsonNode no : n.path("results").path("bindings")){
	    		int num = no.path("cnt").path("value").asInt();
	    		String sub = no.path("dis").path("value").asText();
	    			
	    		if (currentNum==num){
	    			topSubjects.add(sub);
	    		}else{	    			
	    			if (topSubjects.size()<3){	    				
	    				topSubjects.add(sub);
	    			}	    			
	    		}	    			
	    }
	    return topSubjects; 
	}
	
	
	
	public static void main(String[] args) throws Exception {
		
		//System.out.println( getDisciplines(Arrays.asList("Berlin")) );
		
		System.out.println( getNormdata(Arrays.asList("Berlin")) );			
		
	}



	public static Map<String, List<String>> getNormdata(List<String> entities) throws JsonProcessingException, MalformedURLException, IOException {
		 
		String needle = "";
		for (String e : entities){
			// w%3AErasmus_von_Rotterdam+w%3ABerlin+w%3ADeutschland
			needle=needle+ "<https://de.wikipedia.org/wiki/"+ e +">+";			
		}
		
//		String tmplt = "http://wlo-virtuoso:8890/sparql?default-graph-uri=&query=select+distinct+%3Fs+%3Fn++from+%3Chttp%3A%2F%2Fwlo.yovisto.com%3E+where+%7B%0D%0AVALUES+%3Fs%7B+XXXXX+%7D%0D%0A%3Fs+%3Chttp%3A%2F%2Fwlo.yovisto.com%2Fontology%2F1.0%2Fnormdata%3E+%3Fn+.%0D%0A%7D+&should-sponge=&format=application%2Fsparql-results%2Bjson&timeout=0&debug=on&run=+Run+Query+";
		String tmplt = "http://yoyo2:8892/sparql?default-graph-uri=&query=select+distinct+%3Fs+%3Fn++from+%3Chttp%3A%2F%2Fwlo.yovisto.com%3E+where+%7B%0D%0AVALUES+%3Fs%7B+XXXXX+%7D%0D%0A%3Fs+%3Chttp%3A%2F%2Fwlo.yovisto.com%2Fontology%2F1.0%2Fnormdata%3E+%3Fn+.%0D%0A%7D+&should-sponge=&format=application%2Fsparql-results%2Bjson&timeout=0&debug=on&run=+Run+Query+";
		
		String url = tmplt.replace("XXXXX", needle.replace("&","%26"));
		System.out.println(url);
		
		ObjectMapper mapper = new ObjectMapper();	    
	    JsonNode n = mapper.readTree(new URL(url));
	    
	   	Map<String, List<String>> normdata = new HashMap<String, List<String>>();
	  
	    for (JsonNode no : n.path("results").path("bindings")){
	    		String ent = no.path("s").path("value").asText();
	    		String norm = no.path("n").path("value").asText();
	    		
	    		if (!normdata.containsKey(ent)){
	    			normdata.put(ent, new ArrayList<String>());
	    		}
	    		normdata.get(ent).add(norm);	    		
	    		
	    }
	    return normdata; 
	}
	

	public static Map<String, Integer> getEssential(List<String> categories) {
		Map<String, Integer> map = new HashMap<String , Integer>();
		
		for (String c : categories){
			if (map.containsKey(c)){
				map.put(c,  map.get(c) + 1);
			}else{
				map.put(c, 1);
			}
		}
				
		return sortReverseByValue(map);
	}
	
	private static Map<String, Integer> sortReverseByValue(Map<String, Integer> unsortMap) {

        // 1. Convert Map to List of Map
        List<Map.Entry<String, Integer>> list =
                new LinkedList<Map.Entry<String, Integer>>(unsortMap.entrySet());

        // 2. Sort list with Collections.sort(), provide a custom Comparator
        //    Try switch the o1 o2 position for a different order
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2) {
                return -(o1.getValue()).compareTo(o2.getValue());
            }
        });

        // 3. Loop the sorted list and put it into a new insertion order Map LinkedHashMap
        Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }



	public static Set<String> getWhitelisted(List<String> entities) throws JsonProcessingException, MalformedURLException, IOException {
		String needle = "";
		for (String e : entities){
			// w%3AErasmus_von_Rotterdam+w%3ABerlin+w%3ADeutschland
			needle=needle+ "<https://de.wikipedia.org/wiki/"+ e +">+";			
		}
		
//		String tmplt = "http://wlo-virtuoso:8890/sparql?default-graph-uri=&query=select+distinct+%3Fc+where+%7B%0D%0AVALUES+%3Fc+%7B+XXXXX++%7D%0D%0A%3Fk+%3Chttp%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23related%3E+%3Fc+.%0D%0A%7D%0D%0A%0D%0A&should-sponge=&format=application%2Fsparql-results%2Bjson&timeout=0&debug=on&run=+Run+Query+";
		String tmplt = "http://yoyo2:8892/sparql?default-graph-uri=&query=select+distinct+%3Fc+where+%7B%0D%0AVALUES+%3Fc+%7B+XXXXX++%7D%0D%0A%3Fk+%3Chttp%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23related%3E+%3Fc+.%0D%0A%7D%0D%0A%0D%0A&should-sponge=&format=application%2Fsparql-results%2Bjson&timeout=0&debug=on&run=+Run+Query+";

		String url = tmplt.replace("XXXXX", needle.replace("&","%26"));
		System.out.println(url);
		
		ObjectMapper mapper = new ObjectMapper();	    
	    JsonNode n = mapper.readTree(new URL(url));
	    
	    Set<String> whitelisted = new HashSet<String>();
	    	  
	    for (JsonNode no : n.path("results").path("bindings")){
	    		String item = no.path("c").path("value").asText();
	    		whitelisted.add(item);	    		 			
	    }
		return whitelisted;
	}
	
}
