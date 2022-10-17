package demo.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtUtil {

    private static final int expireInMs = 300 * 1000;

    private final static Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    public String generate(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuer("cyper.run")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expireInMs))
                .signWith(key)
                .compact();
    }

    public boolean validate(String token) {
        if (getUsername(token) != null && isExpired(token)) {
            return true;
        }
        return false;
    }

    public String getUsername(String token) {
        Jws<Claims> claims = getClaims(token);
        return claims.getBody().getSubject();
    }

    public boolean isExpired(String token) {
        Jws<Claims> claims = getClaims(token);
        return claims.getBody().getExpiration().after(new Date(System.currentTimeMillis()));
    }

    private Jws<Claims> getClaims(String token) {
        // fixed deprecated issue
        // see https://github.com/jwtk/jjwt
        return
                Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}