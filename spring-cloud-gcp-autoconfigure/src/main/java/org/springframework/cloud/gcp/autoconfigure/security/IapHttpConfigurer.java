/*
 *  Copyright 2018 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.gcp.autoconfigure.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.gcp.security.iap.IapAuthenticationFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@EnableWebSecurity
public class IapHttpConfigurer extends AbstractHttpConfigurer<IapHttpConfigurer, HttpSecurity> {
	private static final Log LOGGER = LogFactory.getLog(IapHttpConfigurer.class);


	public static IapHttpConfigurer iapHttpConfigurer() {
		LOGGER.info("************ IapHttpConfigurer instantiated through static factory");

		return new IapHttpConfigurer();

	}

	@Override
	public void configure(HttpSecurity http) {
		LOGGER.info("*********** IapHttpConfigurer configuring HTTP: " + http);

		ApplicationContext context = http.getSharedObject(ApplicationContext.class);

		if (context.getEnvironment().getProperty(IapSecurityConstants.IAP_GATING_PROPERTY, Boolean.class)) {
			IapAuthenticationFilter filter = context.getBean(IapAuthenticationFilter.class);
			http.addFilterBefore(filter, BasicAuthenticationFilter.class);
		}
		else {
			LOGGER.info("IapAuthenticationFilter bean not found; skipping IAP authentication.");
		}
	}
}
