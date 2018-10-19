package org.springframework.cloud.gcp.security.iap;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

public final class IapAuthentication extends AbstractAuthenticationToken {

	public static final String DEFAULT_ROLE = "ROLE_USER";
	private final String email;
	private final String subject;
	private final String jwtToken;

	public IapAuthentication(String email, String subject, String jwtToken) {
		super(Collections.singletonList(new SimpleGrantedAuthority(DEFAULT_ROLE)));
		this.email = email;
		this.subject = subject;
		this.jwtToken = jwtToken;
	}

	@Override
	public Object getCredentials() {
		return this.jwtToken;
	}

	@Override
	public Object getPrincipal() {
		return email;
	}

	@Override
	public boolean isAuthenticated() {
		return email != null && !email.equals("");
	}
}