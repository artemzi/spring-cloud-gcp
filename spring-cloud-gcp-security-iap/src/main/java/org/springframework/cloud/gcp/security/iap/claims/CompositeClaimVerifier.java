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

package org.springframework.cloud.gcp.security.iap.claims;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.nimbusds.jwt.JWTClaimsSet;

public class CompositeClaimVerifier implements ClaimVerifier {

	private List<ClaimVerifier> delegates;

	public CompositeClaimVerifier(ClaimVerifier... delegates) {
		this.delegates = ImmutableList.copyOf(delegates);
	}

	@Override
	public boolean verify(JWTClaimsSet claims) {
		return this.delegates.stream().allMatch(verifier -> verifier.verify(claims));
	}
}