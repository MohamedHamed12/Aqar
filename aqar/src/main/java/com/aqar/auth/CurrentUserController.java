package com.aqar.auth;

import com.aqar.auth.dto.CurrentUserResponse;
import com.aqar.auth.security.JwtPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CurrentUserController {
	@GetMapping("/me")
	public CurrentUserResponse me(Authentication authentication) {
		JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
		return new CurrentUserResponse(principal.userId(), principal.role());
	}
}