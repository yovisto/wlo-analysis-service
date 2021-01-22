package com.yovisto.kea.rest.services;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.google.common.base.Optional;
import com.google.inject.Guice;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;
import com.yovisto.kea.EntityResolver;
import com.yovisto.kea.ParameterPresets;
import com.yovisto.kea.StandardEntityResolver;
import com.yovisto.kea.commons.DisambiguatedTerm;
import com.yovisto.kea.commons.Parameters;
import com.yovisto.kea.guice.KeaModule;
import com.yovisto.kea.util.MD5;
import com.yovisto.kea.util.SerializationUtil;

@Path("analyze")
public class WloService {

	private LanguageDetector languageDetector;
	private TextObjectFactory textObjectFactory;

	public void init() {

		// load all languages:
		List<LanguageProfile> languageProfiles = null;
		try {
			languageProfiles = new LanguageProfileReader().readAllBuiltIn();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// build language detector:
		languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard()).withProfiles(languageProfiles)
				.build();

		// create a text object factory
		textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();

	}

	private StandardEntityResolver resolverAnnotation = (StandardEntityResolver) Guice.createInjector(new KeaModule())
			.getInstance(EntityResolver.class);

	protected final Logger l = Logger.getLogger(getClass());

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject getAnnotationsPost(JSONObject obj) throws Exception {
		if (languageDetector == null) {
			init();
		}

		JSONObject jresult = new JSONObject();
		JSONObject props = obj.getJSONObject("_source").getJSONObject("properties");

		String text = "";

		String [] titleKeys = {"cclom:title", "cm:title", "cm:name"};
		String [] descriptionKeys = {"cclom:general_description", "cm:description"};
		
		String titleKey=null;
		for (String k : titleKeys){
			if (props.has(k)){
				titleKey = k;
				break;
			}
		}
		
		String descriptionKey=null;
		for (String k : descriptionKeys){
			if (props.has(k)){
				descriptionKey = k;
				break;
			}
		}
		
		if (titleKey!=null) {
			String title = props.getString(titleKey);
			text = text + " " + title;
			JSONObject anno = annotate(title);
			anno.put("key", titleKey);
			JSONArray annotations = new JSONArray();
			annotations.put(anno);
			jresult.put("title", annotations);

		}
		if (descriptionKey!=null) {
			String description = props.getJSONArray(descriptionKey).getString(0);
			text = text + " " + description;
			JSONObject anno = annotate(description);
			anno.put("key", descriptionKey);
			JSONArray annotations = new JSONArray();
			annotations.put(anno);
			jresult.put("description", annotations);
		}

		if (props.has("cclom:general_keyword")) {
			JSONArray kws = props.getJSONArray("cclom:general_keyword");
			// JSONArray annotations = new JSONArray();

			List<String> kwList = new ArrayList<String>();
			for (int k = 0; k < kws.length(); k++) {
				kwList.add(kws.getString(k));
			}

			JSONArray anno = annotateKw(kwList);			
			// annotations.put(anno);
			jresult.put("keywords", anno);
		}

		// detect language
		TextObject textObject = textObjectFactory.forText(text);
		Optional<LdLocale> lang = languageDetector.detect(textObject);

		if (lang.isPresent()) {
			String l = lang.get().getLanguage();
			jresult.put("language", l);
			// System.out.println("LANG: " + l);
		}

		
		// collect entities
		List<String> entities = new ArrayList<String>();
		
		String[] keys = { "title", "description", "keywords" };
		for (String k : keys) {
			if (jresult.has(k)) {
				JSONArray anno = jresult.getJSONArray(k);
				for (int j = 0; j < anno.length(); j++)
					for (int i = 0; i < anno.getJSONObject(j).getJSONArray("entities").length(); i++) {
						JSONObject e = anno.getJSONObject(j).getJSONArray("entities").getJSONObject(i);
						entities.add(e.getJSONObject("entity").getString("suffix"));

						
					}
			}
		}
		
		
		// detect discipline
		JSONArray subs = new JSONArray();
		if (entities.size() > 0)
			for (String sub : Categories.getDisciplines(entities)) {
				subs.put(sub);
			}
		jresult.put("disciplines", subs);

		// get and insert normdata
		Map<String, List<String>> normdata = Categories.getNormdata(entities);
		Set<String> whitelisted = Categories.getWhitelisted(entities);
		List<String> categories = new ArrayList<String>();
		
		for (String k : keys) {
			if (jresult.has(k)) {
				JSONArray anno = jresult.getJSONArray(k);
				for (int j = 0; j < anno.length(); j++)
					for (int i = 0; i < anno.getJSONObject(j).getJSONArray("entities").length(); i++) {
						JSONObject e = anno.getJSONObject(j).getJSONArray("entities").getJSONObject(i);
						if (whitelisted.contains(e.getJSONObject("entity").getString("iri"))){
							e.getJSONObject("entity").put("whitelisted", "true");
							
							if (e.has("categories"))
								for (int cat = 0; cat < e.getJSONArray("categories").length(); cat++) {
									categories.add(e.getJSONArray("categories").getString(cat));
								}
						}else{
							e.put("whitelisted", "false");
						}

						JSONArray jnorm = new JSONArray();
						if (normdata.containsKey(e.getJSONObject("entity").getString("iri"))) {
							for (String nd : normdata.get(e.getJSONObject("entity").getString("iri"))) {
								String[] n = nd.split("=");
								JSONObject jno = new JSONObject();

								jno.put("schema", n[0]);
								jno.put("id", n[1]);
								jnorm.put(jno);
							}
							e.put("normdata", jnorm);
						}

					}
			}
		}
				
		// get categories
		jresult.put("essentialCategories", Categories.getEssential(categories));

		System.out.println(jresult);
		return jresult;

	}

	// public static void main(String[] args) throws Exception {
	// WloService s = new WloService();
	// InputStream is = new FileInputStream("example.json");
	// String jsonTxt = IOUtils.toString( is , "UTF-8");
	// JSONObject json = new JSONObject(jsonTxt);
	// s.getAnnotationsPost(json);
	// }

	@SuppressWarnings("unchecked")
	private JSONArray annotateKw(List<String> keywords) throws JSONException {

		l.info("obj: " + this.hashCode());
		Parameters p = ParameterPresets.getDefaultParameters();	
		p.setProperty(Parameters.DATA_PATH, "/var/indices");

		l.info("###############################################################");

		l.info("Keywords: " + StringUtils.join(keywords, " "));

		if (!new File("keacache").exists()) {
			new File("keacache").mkdir();
		}
		String md5 = MD5.getInstance().toMD5(StringUtils.join(keywords, "-"));

		String fileName = "keacache/keaExtract" + md5;

		List<DisambiguatedTerm> output = new ArrayList<DisambiguatedTerm>();
		try {
			if (new File(fileName).exists()) {
				l.info("Loading from cache: " + fileName);
				output = (List<DisambiguatedTerm>) SerializationUtil.doLoad(fileName);
			} else {
				output = resolverAnnotation.resolveAsKeywords(keywords, p);
				if (output.size() > 0) {
					l.info("Storing to cache: " + fileName);
					SerializationUtil.doSave(output, fileName);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		JSONArray items = new JSONArray();

		Set<String> resultAllCats = new HashSet<String>();
		// Set<String> resultMainCats = new HashSet<String>();

		for (DisambiguatedTerm term : output) {
			JSONObject item = new JSONObject();
			Set<JSONObject> entityList = new HashSet<JSONObject>();

			l.info(term.getCandidate().getIri() + " " + term.getStartOffset() + " " + term.getEndOffset());

			if (term.getCandidate() != null) {
				JSONObject entityItem = new JSONObject();

				JSONObject e = new JSONObject();
				e.put("iri", "https://de.wikipedia.org/wiki/" + term.getCandidate().getIri());
				e.put("label", term.getCandidate().getIri().replace("_", " ").replaceAll("\\(.*\\)", ""));
				e.put("suffix", term.getCandidate().getIri());

				entityItem.put("entity", e);

				entityItem.put("start", term.getStartOffset());
				entityItem.put("end", term.getEndOffset());
				entityItem.put("score", term.getScore());

				if (term.getCandidate().getCategories() != null) {
					for (String cat : term.getCandidate().getCategories()) {
						resultAllCats.add(cat);
					}
					entityItem.put("categories", new JSONArray(term.getCandidate().getCategories()));
				} else {
					l.info("Categories null");
				}

				entityList.add(entityItem);
				item.put("text", getAnnotatedText(term.getSurfaceForm(), Arrays.asList(term)));
				item.put("entities", entityList);
				items.put(item);

			} else {
				l.info("URI null");
			}

		}

		/*
		 * if (output.size() > 0) { if (output.get(0).getEssentialCategories()
		 * != null) {
		 * resultMainCats.addAll(output.get(0).getEssentialCategories()); } }
		 */

		// items.put("essentialCategories", new JSONArray(resultMainCats));

		return items;
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
		String md5 = MD5.getInstance().toMD5(text);
		String fileName = "keacache/keaExtract" + md5;

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
		// Set<String> resultMainCats = new HashSet<String>();

		for (DisambiguatedTerm term : output) {
			l.info(term.getCandidate().getIri() + " " + term.getStartOffset() + " " + term.getEndOffset());

			if (term.getCandidate() != null) {
				JSONObject jo = new JSONObject();

				JSONObject e = new JSONObject();
				e.put("iri", "https://de.wikipedia.org/wiki/" + term.getCandidate().getIri());
				e.put("label", term.getCandidate().getIri().replace("_", " ").replaceAll("\\(.*\\)", ""));
				e.put("suffix", term.getCandidate().getIri());

				jo.put("entity", e);

				jo.put("start", term.getStartOffset());
				jo.put("end", term.getEndOffset());
				jo.put("score", term.getScore());

				if (term.getCandidate().getCategories() != null) {
					for (String cat : term.getCandidate().getCategories()) {
						resultAllCats.add(cat);
					}
					jo.put("categories", new JSONArray(term.getCandidate().getCategories()));
				} else {
					l.info("Categories null");
				}

				result.add(jo);
			} else {
				l.info("URI null");
			}

		}

		/*
		 * if (output.size() > 0) { if (output.get(0).getEssentialCategories()
		 * != null) {
		 * resultMainCats.addAll(output.get(0).getEssentialCategories()); } }
		 */

		JSONObject jresult = new JSONObject();
		jresult.put("text", getAnnotatedText(text, output));
		jresult.put("entities", new JSONArray(result));
		// jresult.put("essentialCategories", new JSONArray(resultMainCats));

		return jresult;
	}

	private String getAnnotatedText(String text, List<DisambiguatedTerm> terms) {

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
				// annotatedText = pre + ">" + surface + "<" + post;
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
