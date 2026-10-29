package com.example.incidents.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lit l'en-tête "Authorization: Bearer <JWT>", vérifie le token puis recharge l'utilisateur en base :
 * un compte désactivé ou supprimé perd l'accès immédiatement, même avec un token encore valide,
 * et les rôles sont ceux de la base (une promotion ou rétrogradation est prise en compte tout de suite).
 * En cas d'échec le filtre n'authentifie pas la requête et mémorise la cause ; c'est le point d'entrée
 * (RestAuthenticationEntryPoint) qui répond 401 pour les routes protégées.
 * Ce filtre n'est PAS un @Component : il est instancié dans SecurityConfig pour ne pas être enregistré deux fois.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTRIBUTE = "jwt.auth.error";

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, AppUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7).trim();
            try {
                Claims claims = jwtService.parse(token);
                AppUserDetails user = userDetailsService.loadUserByUsername(claims.getSubject());
                if (!user.isEnabled()) {
                    request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Compte désactivé");
                } else {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (ExpiredJwtException e) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Token expiré");
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Token invalide");
            }
        }
        chain.doFilter(request, response);
    }
}
