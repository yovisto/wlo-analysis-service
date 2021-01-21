package com.yovisto.kea.rest.guice;

import com.google.inject.Scopes;
import com.google.inject.servlet.ServletModule;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class RestServletModule extends ServletModule {

	@Override
	protected void configureServlets() {
		
		ResourceConfig rc = new PackagesResourceConfig("com.yovisto.kea.rest.services");

		for (Class<?> resource : rc.getClasses()) {
			bind(resource).in(Scopes.SINGLETON);
		}
		
		serve("/services/*").with(GuiceContainer.class);

	}
}
