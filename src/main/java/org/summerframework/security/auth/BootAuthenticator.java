/*
 * Copyright 2005 The Summer Boot Framework Project
 *
 * The Summer Boot Framework Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.summerframework.security.auth;

import org.summerframework.boot.BootErrorCode;
import org.summerframework.boot.BootPOI;
import org.summerframework.integration.cache.AuthTokenCache;
import org.summerframework.nio.server.domain.Error;
import org.summerframework.nio.server.domain.ServiceContext;
import org.summerframework.security.JwtUtil;
import org.summerframework.util.FormatterUtil;
import com.google.inject.Singleton;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.security.Key;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.naming.NamingException;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Changski Tie Zheng Zhang, Du Xiao
 */
@Singleton
public abstract class BootAuthenticator implements Authenticator {

    protected AuthenticatorListener listener;

    @Override
    public void setListener(AuthenticatorListener listener) {
        this.listener = listener;
    }

    @Override
    public String authenticate(String uid, String pwd, int validForMinutes, final ServiceContext context) throws IOException, NamingException {
        //1. protect request body from being logged
        context.privacyReqContent(true);

        //2. verifyToken caller      
        context.timestampPOI(BootPOI.LDAP_BEGIN);
        Caller caller = authenticateCaller(uid, pwd, listener);
        context.timestampPOI(BootPOI.LDAP_END);
        if (caller == null) {
            context.status(HttpResponseStatus.UNAUTHORIZED);
            return null;
        }

        //3. format JWT
        JwtBuilder builder = marshalCaller(caller);

        //4. create JWT
        //String token = JwtUtil.createJWT(AuthConfig.CFG.getJwtSignatureAlgorithm(),
        Key signingKey = AuthConfig.CFG.getJwtSigningKey();
        String token = JwtUtil.createJWT(signingKey, builder, TimeUnit.MINUTES, validForMinutes);
        if (listener != null) {
            listener.onLoginSuccess(caller.getUid(), token);
        }
        context.caller(caller).status(HttpResponseStatus.CREATED).privacyRespHeader(true);
        return token;
    }

    abstract protected Caller authenticateCaller(String uid, String password, AuthenticatorListener listener) throws IOException, NamingException;

    /**
     * Convert Caller to auth token, override this method to implement
     * customized token format
     *
     * @param caller
     * @return formatted auth token builder
     */
    protected JwtBuilder marshalCaller(Caller caller) {
        String jti = String.valueOf(caller.getId());
        String issuer = AuthConfig.CFG.getJwtIssuer();
        if (caller.getTenantId() != null || caller.getTenantName() != null) {
            issuer = caller.getTenantName() + "#" + caller.getTenantId();
        }
        String subject = caller.getUid();
        Set<String> groups = caller.getGroups();
        String groupsCsv = groups == null || groups.size() < 1
                ? null
                : groups.stream().collect(Collectors.joining(","));
        String audience = groupsCsv;

        JwtBuilder builder = Jwts.builder()
                .setId(jti)
                .setIssuer(issuer)
                .setSubject(subject)
                .setAudience(audience);
        return builder;
    }

    /**
     * Convert Caller back from auth token, override this method to implement
     * customized token format
     *
     * @param claims
     * @return Caller
     */
    protected Caller unmarshalCaller(Claims claims) {
        String jti = claims.getId();
        String issuer = claims.getIssuer();
        String subject = claims.getSubject();
        String audience = claims.getAudience();

        String[] tenantInfo = {null, "0"};// tenantName#tenantId
        if (issuer != null) {
            tenantInfo = issuer.split("#");
        }
        long tenantId;
        try {
            tenantId = Long.parseLong(tenantInfo[1]);
        } catch (Throwable ex) {
            tenantId = 0;
        }
        long id;
        try {
            id = Long.parseLong(jti);
        } catch (Throwable ex) {
            id = 0;
        }
        String userName = subject;
        User caller = new User(tenantId, tenantInfo[0], id, userName);

        String userGroups = audience;
        if (StringUtils.isNotBlank(userGroups)) {
            String[] groups = FormatterUtil.parseCsv(userGroups);
            for (String group : groups) {
                caller.addGroup(group);
            }
        }
        return caller;
    }

    @Override
    public String getAuthToken(HttpHeaders httpRequestHeaders) {
        String authToken = httpRequestHeaders.get(HttpHeaderNames.AUTHORIZATION);
        if (StringUtils.isBlank(authToken) || !authToken.startsWith("Bearer ")) {
            return null;
        }
        String[] a = authToken.split(" ");
        if (a.length < 2) {
            return null;
        }
        authToken = a[1];
        if (StringUtils.isBlank(authToken)) {
            return null;
        }
        return authToken;
    }

    @Override
    public Caller verifyToken(HttpHeaders httpRequestHeaders, AuthTokenCache cache, ServiceContext context) {
        String authToken = getAuthToken(httpRequestHeaders);
        return verifyToken(authToken, cache, context);
    }

    @Override
    public Caller verifyToken(String authToken, AuthTokenCache cache, ServiceContext context) {
        Caller caller = null;
        if (authToken == null) {
            Error e = new Error(BootErrorCode.AUTH_REQUIRE_TOKEN, null, "Missing AuthToken", null);
            context.error(e).status(HttpResponseStatus.UNAUTHORIZED);
        } else {
            try {
                Claims claims = JwtUtil.parseJWT(AuthConfig.CFG.getJwtParser(), authToken).getBody();
                String jti = claims.getId();
                context.callerId(jti);
                if (cache != null && cache.isOnBlacklist(jti)) {// because jti is used as blacklist key in logout
                    Error e = new Error(BootErrorCode.AUTH_EXPIRED_TOKEN, null, "Blacklisted AuthToken", null);
                    context.error(e).status(HttpResponseStatus.UNAUTHORIZED);
                } else {
                    caller = unmarshalCaller(claims);
                    if (listener != null && !listener.verify(caller, claims)) {
                        Error e = new Error(BootErrorCode.AUTH_INVALID_TOKEN, null, "Rejected AuthToken", null);
                        context.error(e).status(HttpResponseStatus.UNAUTHORIZED);
                        caller = null;
                    }
                }
            } catch (ExpiredJwtException ex) {
                Error e = new Error(BootErrorCode.AUTH_EXPIRED_TOKEN, null, "Expired AuthToken", null);
                context.error(e).status(HttpResponseStatus.UNAUTHORIZED);
            } catch (JwtException ex) {
                Error e = new Error(BootErrorCode.AUTH_INVALID_TOKEN, ex.getClass().getSimpleName(), "Invalid AuthToken - " + ex.getMessage(), null);
                context.error(e).status(HttpResponseStatus.UNAUTHORIZED);
            }
        }
        context.caller(caller);
        return caller;
    }

    @Override
    public void logout(HttpHeaders httpRequestHeaders, AuthTokenCache cache, ServiceContext context) {
        String authToken = getAuthToken(httpRequestHeaders);
        logout(authToken, cache, context);
    }

    @Override
    public void logout(String authToken, AuthTokenCache cache, ServiceContext context) {
        try {
            Claims claims = JwtUtil.parseJWT(AuthConfig.CFG.getJwtParser(), authToken).getBody();
            String jti = claims.getId();
            String uid = claims.getSubject();
            Date exp = claims.getExpiration();
            long expireInMilliseconds = exp.getTime() - System.currentTimeMillis();
            if (cache != null) {
                cache.putOnBlacklist(jti, authToken, expireInMilliseconds);
            }
            if (listener != null) {
                listener.onLogout(jti, authToken, expireInMilliseconds);
            }
        } catch (ExpiredJwtException ex) {
            //ignore
        } catch (JwtException ex) {
            context.status(HttpResponseStatus.FORBIDDEN);
            return;
        }
        context.status(HttpResponseStatus.NO_CONTENT);
    }

}
