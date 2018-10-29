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

package org.springframework.cloud.gcp.security.iap;

import java.net.MalformedURLException;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.gcp.security.iap.claims.ClaimVerifier;
import org.springframework.cloud.gcp.security.iap.claims.IssueTimeInPastClaimVerifier;
import org.springframework.cloud.gcp.security.iap.claims.IssuerClaimVerifier;
import org.springframework.cloud.gcp.security.iap.claims.RequiredFieldsClaimVerifier;
import org.springframework.cloud.gcp.security.iap.jwk.JwkRegistry;

/**
 * Verify IAP authorization JWT token in incoming request.
 *
 * JwkTokenStore verifies the signature through JwkVerifyingJwtAccessTokenConverter.
 */
public class JwtTokenVerifier {

	private static final Log LOGGER = LogFactory.getLog(JwtTokenVerifier.class);

	// todo: externalize as properties?
	private static final String PUBLIC_KEY_VERIFICATION_LINK = "https://www.gstatic.com/iap/verify/public_key-jwk";

	private final JwkRegistry jwkRegistry = new JwkRegistry(PUBLIC_KEY_VERIFICATION_LINK);

	private final List<ClaimVerifier> claimVerifiers;

	public JwtTokenVerifier() throws MalformedURLException {
		claimVerifiers = ImmutableList.of(
				new RequiredFieldsClaimVerifier(),
				new IssueTimeInPastClaimVerifier(),
				// TODO: uncomment; commented out for local testing
				//new ExpirationTimeInFutureClaimVerifier()
				new IssuerClaimVerifier()
		);
	}

	/*
	 * private JwkTokenStore tokenStore = new JwkTokenStore(PUBLIC_KEY_VERIFICATION_URL, new
	 * IapJwtClaimsSetVerifier());
	 *
	 * public IapAuthentication verifyAndExtractPrincipal(String jwtToken, String
	 * expectedAudience) {
	 *
	 * // reads and validates OAuth2AccessToken token = tokenStore.readAccessToken(jwtToken);
	 *
	 * IapAuthentication iapAuth = new IapAuthentication((String)
	 * token.getAdditionalInformation().get("email"), null, jwtToken); return iapAuth; }
	 */

	public IapAuthentication verifyAndExtractPrincipal(String jwtToken, String expectedAudience) {
		if (jwtToken == null) {
			LOGGER.warn("Jwt token verification requested, yet no token passed in; not authenticating");
			return null;
		}

		IapAuthentication authentication = null;
		SignedJWT signedJwt = extractSignedToken(jwtToken);

		if (validateJwt(signedJwt)) {
			JWTClaimsSet claims = extractClaims(signedJwt);
			String email = extractClaimValue(claims, "email");

			if (validateClaims(claims) && email != null) {
				authentication = new IapAuthentication(email, claims.getSubject(), jwtToken);
			}
		}
		else {
			LOGGER.warn("Jwt public key verification failed; not authenticating");
		}

		return authentication;
	}

	private String extractClaimValue(JWTClaimsSet claims, String propertyName) {
		try {
			return claims.getStringClaim(propertyName);
		}
		catch (ParseException e) {
			LOGGER.warn("String value could not be parsed from claims.", e);
			return null;
		}
	}

	private JWTClaimsSet extractClaims(SignedJWT signedJwt) {

		JWTClaimsSet claims = null;
		try {
			claims = signedJwt.getJWTClaimsSet();
		}
		catch (ParseException e) {
			LOGGER.warn("JWT Claims could not be parsed", e);
		}
		return claims;
	}

	private boolean validateClaims(JWTClaimsSet claims) {
		if (claims == null) {
			LOGGER.warn("Null claims cannot be validated.");
			return false;
		}

		for (ClaimVerifier verifier : this.claimVerifiers) {
			if (!verifier.verify(claims)) {
				return false;
			}
		}

		// claims must have audience, issuer
		// TODO: Vary expectec audience based on whether installed in AppEngine or ComputeEngine
		// Preconditions.checkArgument(claims.getAudience().contains(expectedAudience));

		return true;
	}



	private SignedJWT extractSignedToken(String jwtToken) {
		SignedJWT signedJwt = null;

		try {
			signedJwt = SignedJWT.parse(jwtToken);
		}
		catch (ParseException e) {
			LOGGER.error("JWT Token could not be parsed.", e);
		}

		return signedJwt;
	}

	private boolean validateJwt(SignedJWT signedJwt) {
		if (signedJwt == null) {
			LOGGER.warn("Null signed JWT is invalid.");
			return false;
		}

		JWSHeader jwsHeader = signedJwt.getHeader();
		ECPublicKey publicKey = null;

		if (jwsHeader.getAlgorithm() == null) {
			LOGGER.warn("JWT header algorithm null.");
		}
		else if (jwsHeader.getKeyID() == null) {
			LOGGER.warn("JWT key ID null.");
		}
		else {

			publicKey = this.jwkRegistry.getPublicKey(jwsHeader.getKeyID(), jwsHeader.getAlgorithm().getName());
			if (publicKey != null) {
				return verifyAgainstPublicKey(signedJwt, publicKey);
			}
		}

		return false;
	}

	private boolean verifyAgainstPublicKey(SignedJWT signedJwt, ECPublicKey publicKey) {
		JWSVerifier jwsVerifier = null;
		try {
			jwsVerifier = new ECDSAVerifier(publicKey);
		}
		catch (JOSEException e) {
			LOGGER.warn("Public key verifier could not be created.", e);
			return false;
		}

		try {
			return signedJwt.verify(jwsVerifier);
		}
		catch (JOSEException e) {
			LOGGER.warn("Signed JWT Token could not be verified against public key.", e);
			return false;
		}
	}
}
