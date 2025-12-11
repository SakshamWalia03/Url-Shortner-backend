package com.url.shortener.service;

import com.url.shortener.models.User;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Implementation of Spring Security's UserDetails interface.
 * Encapsulates user information required by Spring Security during authentication and authorization.
 */
@Data
@NoArgsConstructor
public class UserDetailsImpl implements UserDetails {

    private static final long serialVersionUID = 1L;

    // Unique identifier for the user
    private Long id;

    // Username used for authentication
    private String username;

    // User's email address
    private String email;

    // Encrypted password
    private String password;

    // Roles/authorities assigned to the user
    private Collection<? extends GrantedAuthority> authorities;

    /**
     * Full constructor for initializing all fields.
     *
     * @param id          User's ID
     * @param username    User's username
     * @param email       User's email
     * @param password    User's encrypted password
     * @param authorities Collection of granted authorities (roles)
     */
    public UserDetailsImpl(Long id, String username, String email, String password, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    /**
     * Factory method to build UserDetailsImpl from a User entity.
     *
     * @param user User entity from the database
     * @return UserDetailsImpl object with user info and authorities
     */
    public static UserDetailsImpl build(User user) {
        // Convert user's role string to a GrantedAuthority
        GrantedAuthority authority = new SimpleGrantedAuthority(user.getRole());

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(authority) // single role wrapped in a list
        );
    }

    // ---------------- UserDetails interface methods ----------------

    /**
     * Returns authorities granted to the user.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * Returns the password used to authenticate the user.
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Returns the username used to authenticate the user.
     */
    @Override
    public String getUsername() {
        return username;
    }
}