package com.yovisto.kea.rest.services;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.yovisto.kea.ParameterPresets;
import com.yovisto.kea.util.IndexAccess;
import com.yovisto.kea.util.IndexAccessImpl;

@Path("candidates")
public class CandidateService {

	private IndexAccess access = new IndexAccessImpl();

	protected final Logger l = Logger.getLogger(getClass());

	@GET
	@Path("{input}")	
	@Produces(MediaType.APPLICATION_JSON)
	public JSONArray getAnnotationsPlain(@PathParam("input") String text) throws Exception {
		access.setup(ParameterPresets.getDefaultParameters());
		
		JSONObject o = new JSONObject();
		JSONArray a = new JSONArray();
		List<String> items = access.getIrisForLabel(text);
		for (String item : items){
			a.put(item);
		}
		o.put("",a);
		return a;
	}

}
