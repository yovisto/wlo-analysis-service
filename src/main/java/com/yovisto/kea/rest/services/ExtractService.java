package com.yovisto.kea.rest.services;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.google.inject.Guice;
import com.yovisto.kea.EntityResolver;
import com.yovisto.kea.ParameterPresets;
import com.yovisto.kea.commons.DisambiguatedTerm;
import com.yovisto.kea.commons.Parameters;
import com.yovisto.kea.guice.KeaModule;
import com.yovisto.kea.util.MD5;
import com.yovisto.kea.util.SerializationUtil;

@Path("extract")
public class ExtractService {

	private EntityResolver resolverAnnotation = Guice.createInjector(new KeaModule())
			.getInstance(EntityResolver.class);

	protected final Logger l = Logger.getLogger(getClass());	
	
	@POST		
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject getAnnotationsPost(String text) throws Exception {
		JSONObject jresult = annotate(text);		
		return jresult;
	}

	@GET	
	@Path("{input}")
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject getAnnotationsGet(@PathParam("input") String text) throws Exception {
		JSONObject jresult = annotate(text);
		return jresult;
	}

	@SuppressWarnings("unchecked")
	private JSONObject annotate(String text) throws JSONException {
		
		l.info("obj: " + this.hashCode());
		Parameters p = ParameterPresets.getDefaultParameters();	
		
		l.info("###############################################################");

		l.info("Document: " + text);

		if (!new File("keacache").exists()) {
			new File("keacache").mkdir();
		}
		String fileName = "keacache/keaExtract" + MD5.getInstance().toMD5(text);

		List<DisambiguatedTerm> output = new ArrayList<DisambiguatedTerm>();
		try {
			if (new File(fileName).exists()) {
				l.info("Loading from cache: " + fileName);
				output = (List<DisambiguatedTerm>) SerializationUtil.doLoad(fileName);
			} else {
				output = resolverAnnotation.resolve(text, p);
				if (output.size() > 0) {
					l.info("Storing to cache: " + fileName);
					SerializationUtil.doSave(output, fileName);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}						
		
		Set<JSONObject> result = new HashSet<JSONObject>();
		Set<String> resultAllCats = new HashSet<String>();
		Set<String> resultMainCats = new HashSet<String>();

		for (DisambiguatedTerm term : output) {
			l.info(term.getCandidate().getIri() + " " + term.getStartOffset() + " " + term.getEndOffset());
			
			if (term.getCandidate() != null) {
				JSONObject jo = new JSONObject();
				jo.put("entity", term.getCandidate().getIri());
				jo.put("start", term.getStartOffset());
				jo.put("end", term.getEndOffset());
				jo.put("score", term.getScore());
				
				if (term.getCandidate().getCategories() != null) {
					for (String cat : term.getCandidate().getCategories()) {
						resultAllCats.add(cat);
					}
					jo.put("categories",new JSONArray(term.getCandidate().getCategories()));
				} else {
					l.info("Categories null");
				}
				
				result.add(jo);
			} else {
				l.info("URI null");
			}			
			
		}

		if (output.size() > 0) {
			if (output.get(0).getEssentialCategories()!=null){
				resultMainCats.addAll(output.get(0).getEssentialCategories());
			}
		}

		JSONObject jresult = new JSONObject();
		jresult.put("text", getAnnotatedText(text, output));
		jresult.put("entities", new JSONArray(result));		
		jresult.put("essentialCategories", new JSONArray(resultMainCats));
				
		return jresult;
	}
	
	
	
	private String getAnnotatedText(String text, List<DisambiguatedTerm> terms){
		
		String annotatedText = text;
		Collections.sort(terms);
		for (DisambiguatedTerm term : terms) {
			if (term != null) {
							
				String u = term.getCandidate().getIri();
				
				fixOffsets(term, annotatedText.substring(term.getStartOffset(), term.getEndOffset()));
				String pre = annotatedText.substring(0, term.getStartOffset());
				String post = annotatedText.substring(term.getEndOffset(), annotatedText.length());
				String surface = annotatedText.substring(term.getStartOffset(), term.getEndOffset());

				annotatedText = pre + "<a href='https://de.wikipedia.org/wiki/" + u + "'>" + surface + "</a>" + post;
				//annotatedText = pre + ">" + surface + "<" + post;
			}
		}
		
		return annotatedText;
		
	}

	private static void fixOffsets(DisambiguatedTerm term, String surface) {
		// trim special chars and adapt the offsets

		int addToStart = 0;
		int subFromEnd = 0;
		for (char c : surface.toCharArray()) {
			if (!Character.isLetter(c) && !Character.isDigit(c)) {
				addToStart++;
			} else {
				break;
			}
		}
		for (char c : new StringBuilder(surface).reverse().toString().toCharArray()) {
			if (!Character.isLetter(c) && !Character.isDigit(c)) {
				subFromEnd++;
			} else {
				break;
			}
		}
		term.setStartOffset(term.getStartOffset() + addToStart);
		term.setEndOffset(term.getEndOffset() - subFromEnd);
		System.out.println(surface + ": " + addToStart + "  " + subFromEnd);
	}
	
}
