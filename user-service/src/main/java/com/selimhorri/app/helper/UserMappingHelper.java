package com.selimhorri.app.helper;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;

public interface UserMappingHelper {
	
    public static UserDto map(final User user) {
	final Credential cred = user.getCredential();
	final CredentialDto credDto = cred == null ? null : CredentialDto.builder()
		.credentialId(cred.getCredentialId())
		.username(cred.getUsername())
		.password(cred.getPassword())
		.roleBasedAuthority(cred.getRoleBasedAuthority())
		.isEnabled(cred.getIsEnabled())
		.isAccountNonExpired(cred.getIsAccountNonExpired())
		.isAccountNonLocked(cred.getIsAccountNonLocked())
		.isCredentialsNonExpired(cred.getIsCredentialsNonExpired())
		.build();

	return UserDto.builder()
		.userId(user.getUserId())
		.firstName(user.getFirstName())
		.lastName(user.getLastName())
		.imageUrl(user.getImageUrl())
		.email(user.getEmail())
		.phone(user.getPhone())
		.credentialDto(credDto)
		.build();
    }
	
	public static User map(final UserDto userDto) {
		final User user = User.builder()
				.userId(userDto.getUserId())
				.firstName(userDto.getFirstName())
				.lastName(userDto.getLastName())
				.imageUrl(userDto.getImageUrl())
				.email(userDto.getEmail())
				.phone(userDto.getPhone())
				.build();

		if (userDto.getCredentialDto() != null) {
			final Credential cred = Credential.builder()
					.credentialId(userDto.getCredentialDto().getCredentialId())
					.username(userDto.getCredentialDto().getUsername())
					.password(userDto.getCredentialDto().getPassword())
					.roleBasedAuthority(userDto.getCredentialDto().getRoleBasedAuthority())
					.isEnabled(userDto.getCredentialDto().getIsEnabled())
					.isAccountNonExpired(userDto.getCredentialDto().getIsAccountNonExpired())
					.isAccountNonLocked(userDto.getCredentialDto().getIsAccountNonLocked())
					.isCredentialsNonExpired(userDto.getCredentialDto().getIsCredentialsNonExpired())
					.build();

			cred.setUser(user);
			user.setCredential(cred);
		}

		return user;
	}
	
	
	
}






